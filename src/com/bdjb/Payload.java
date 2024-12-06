/*
 * Copyright (C) 2021-2024 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb;

import com.bdjb.api.API;
import com.bdjb.api.Buffer;
import com.bdjb.api.KernelAPI;
import com.bdjb.api.Int32;
import com.bdjb.api.Text;

public class Payload {
  private static final String SCE_KERNEL_JIT_CREATE_SHARED_MEMORY_SYMBOL =
      "sceKernelJitCreateSharedMemory";
  private static final String MMAP_SYMBOL = "mmap";
  private static final String MUNMAP_SYMBOL = "munmap";
  private static final String CLOSE_SYMBOL = "close";
  private static final String DLSYM_SYMBOL = "dlsym";

  private static final int LIBPROSPERO_WRAPPER_MODULE_HANDLE = 0x3D;

  private static final int PROT_READ = 0x1;
  private static final int PROT_WRITE = 0x2;
  private static final int PROT_EXEC = 0x4;

  private static final int MAP_SHARED = 0x1;

  private static final long MAP_FAILED = -1;

  private static final int ALIGNMENT = 0x100000;

  private static final API api;
  private static final KernelAPI kapi = KernelAPI.getInstance();

  static {
    try {
      api = API.getInstance();
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private final long sceKernelJitCreateSharedMemory;
  private final long mmap;
  private final long munmap;
  private final long close;

  private final byte[] payload;

  public Payload(byte[] payload) {
    sceKernelJitCreateSharedMemory =
        api.dlsym(API.LIBKERNEL_MODULE_HANDLE, SCE_KERNEL_JIT_CREATE_SHARED_MEMORY_SYMBOL);
    mmap = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, MMAP_SYMBOL);
    munmap = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, MUNMAP_SYMBOL);
    close = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, CLOSE_SYMBOL);
    if (sceKernelJitCreateSharedMemory == 0 || mmap == 0 || munmap == 0 || close == 0) {
      throw new InternalError("symbols not found");
    }

    this.payload = payload;
  }

  private int sceKernelJitCreateSharedMemory(Text name, long len, int maxProt, Int32 fdOut) {
    return (int)
        api.call(
            sceKernelJitCreateSharedMemory,
            name.address(),
            len,
            maxProt,
            fdOut != null ? fdOut.address() : 0);
  }

  private long mmap(long addr, long len, int prot, int flags, int fd, long offset) {
    return api.call(mmap, addr, len, prot, flags, fd, offset);
  }

  private int munmap(long addr, long len) {
    return (int) api.call(munmap, addr, len);
  }

  private int close(int fd) {
    return (int) api.call(close, fd);
  }

  private long align(long x, long align) {
    return (x + align - 1) & -align;
  }

  public int execute() {
    long alignedSize = align(payload.length, ALIGNMENT);

    Int32 handle = new Int32();

    // Create JIT handle.
    if (sceKernelJitCreateSharedMemory(
            new Text("payload"), alignedSize, PROT_READ | PROT_WRITE | PROT_EXEC, handle)
        != 0) {
      throw new InternalError("sceKernelJitCreateSharedMemory failed");
    }

    // Map payload.
    long rwx =
        mmap(0, alignedSize, PROT_READ | PROT_WRITE | PROT_EXEC, MAP_SHARED, handle.get(), 0);
    if (rwx == MAP_FAILED) {
      throw new InternalError("mmap failed");
    }

    // Copy in payload.
    api.memcpy(rwx, payload, payload.length);

    // Close JIT handle.
    close(handle.get());

    // Prepare arguments.
    Buffer args = new Buffer(0x20);
    args.putLong(0x00, api.dlsym(LIBPROSPERO_WRAPPER_MODULE_HANDLE, DLSYM_SYMBOL));
    args.putLong(0x08, kapi.getKaslrOffset());
    args.putLong(0x10, kapi.getMasterPipeFd().address());
    args.putLong(0x18, kapi.getVictimPipeFd().address());

    // Execute payload.
    int ret = (int) api.call(rwx, args.address());

    // Unmap payload.
    munmap(rwx, alignedSize);

    return ret;
  }
}
