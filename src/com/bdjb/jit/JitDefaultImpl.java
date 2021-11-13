/*
 * Copyright (C) 2021 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.jit;

import com.bdjb.api.API;
import com.bdjb.api.Buffer;
import com.bdjb.api.Int32;
import com.bdjb.api.Int64;
import com.bdjb.api.Text;

/** Default JIT implementation using SCE API. */
public final class JitDefaultImpl extends AbstractJit {
  private static final String SCE_KERNEL_JIT_CREATE_SHARED_MEMORY_SYMBOL =
      "sceKernelJitCreateSharedMemory";
  private static final String SCE_KERNEL_JIT_CREATE_ALIAS_OF_SHARED_MEMORY_SYMBOL =
      "sceKernelJitCreateAliasOfSharedMemory";
  private static final String SCE_KERNEL_JIT_MAP_SHARED_MEMORY_SYMBOL =
      "sceKernelJitMapSharedMemory";

  private static JitDefaultImpl instance;

  private long sceKernelJitCreateSharedMemory;
  private long sceKernelJitCreateAliasOfSharedMemory;
  private long sceKernelJitMapSharedMemory;

  private Int32 sharedHandle = new Int32();
  private Int32 aliasHandle = new Int32();

  private long rx;
  private long rw;

  private JitDefaultImpl() {
    sceKernelJitCreateSharedMemory =
        api.dlsym(API.LIBKERNEL_MODULE_HANDLE, SCE_KERNEL_JIT_CREATE_SHARED_MEMORY_SYMBOL);
    sceKernelJitCreateAliasOfSharedMemory =
        api.dlsym(API.LIBKERNEL_MODULE_HANDLE, SCE_KERNEL_JIT_CREATE_ALIAS_OF_SHARED_MEMORY_SYMBOL);
    sceKernelJitMapSharedMemory =
        api.dlsym(API.LIBKERNEL_MODULE_HANDLE, SCE_KERNEL_JIT_MAP_SHARED_MEMORY_SYMBOL);

    if (sceKernelJitCreateSharedMemory == 0
        || sceKernelJitCreateAliasOfSharedMemory == 0
        || sceKernelJitMapSharedMemory == 0) {
      throw new InternalError("symbols not found");
    }
  }

  public static synchronized JitDefaultImpl getInstance() {
    if (instance == null) {
      instance = new JitDefaultImpl();
    }
    return instance;
  }

  int sceKernelJitCreateSharedMemory(Text name, long len, int maxProt, Int32 fdOut) {
    return (int)
        api.call(
            sceKernelJitCreateSharedMemory,
            name.address(),
            len,
            maxProt,
            fdOut != null ? fdOut.address() : 0);
  }

  int sceKernelJitCreateAliasOfSharedMemory(int fd, int maxProt, Int32 fdOut) {
    return (int)
        api.call(
            sceKernelJitCreateAliasOfSharedMemory,
            fd,
            maxProt,
            fdOut != null ? fdOut.address() : 0);
  }

  int sceKernelJitMapSharedMemory(int fd, int prot, Int64 startOut) {
    return (int)
        api.call(sceKernelJitMapSharedMemory, fd, prot, startOut != null ? startOut.address() : 0);
  }

  protected long jitMap(long size, long alignment) {
    if (sceKernelJitCreateSharedMemory(
            new Text("jit"),
            align(size + alignment - 1, PAGE_SIZE),
            PROT_READ | PROT_WRITE | PROT_EXEC,
            sharedHandle)
        != 0) {
      throw new InternalError("sceKernelJitCreateSharedMemory failed");
    }

    if (sceKernelJitCreateAliasOfSharedMemory(
            sharedHandle.get(), PROT_READ | PROT_WRITE, aliasHandle)
        != 0) {
      throw new InternalError("sceKernelJitCreateAliasOfSharedMemory failed");
    }

    if ((rx =
            mmap(
                0,
                align(size + alignment - 1, PAGE_SIZE),
                PROT_READ | PROT_EXEC,
                MAP_SHARED,
                sharedHandle.get(),
                0))
        == MAP_FAILED) {
      throw new InternalError("mmap failed");
    }
    rx = align(rx, alignment);

    if ((rw =
            mmap(
                0,
                align(size + alignment - 1, PAGE_SIZE),
                PROT_READ | PROT_WRITE,
                MAP_SHARED,
                aliasHandle.get(),
                0))
        == MAP_FAILED) {
      throw new InternalError("mmap failed");
    }
    rw = align(rw, alignment);

    return rx;
  }

  protected void jitCopy(long dest, byte[] src, long n) {
    api.memcpy(dest - rx + rw, src, n);
  }
}
