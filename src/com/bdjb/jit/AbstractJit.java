/*
 * Copyright (C) 2021 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.jit;

import com.bdjb.api.API;
import java.io.RandomAccessFile;

abstract class AbstractJit {
  public static final int PROT_NONE = 0x0;
  public static final int PROT_READ = 0x1;
  public static final int PROT_WRITE = 0x2;
  public static final int PROT_EXEC = 0x4;

  public static final int MAP_SHARED = 0x1;
  public static final int MAP_PRIVATE = 0x2;
  public static final int MAP_FIXED = 0x10;
  public static final int MAP_ANONYMOUS = 0x1000;

  public static final long MAP_FAILED = -1;

  public static final int PAGE_SIZE = 0x4000;
  public static final int ALIGNMENT = 0x100000;

  protected static final int CHUNK_SIZE = 0x30;

  protected static final API api;

  private static final String MMAP_SYMBOL = "mmap";

  static {
    try {
      api = API.getInstance();
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  protected long mmap;

  protected AbstractJit() {
    mmap = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, MMAP_SYMBOL);
    if (mmap == 0) {
      throw new InternalError("symbols not found");
    }
  }

  protected long mmap(long addr, long len, int prot, int flags, int fd, long offset) {
    return api.call(mmap, addr, len, prot, flags, fd, offset);
  }

  protected long align(long x, long align) {
    return (x + align - 1) & ~(align - 1);
  }

  protected abstract long jitMap(long size, long alignment);

  protected abstract void jitCopy(long dest, byte[] src, long n);

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
    for (long i = 0; i < dataSectionOffset; i += chunk.length) {
      api.memset(chunk, 0, chunk.length);

      file.seek(i);
      int read = file.read(chunk, 0, (int) Math.min(dataSectionOffset - i, chunk.length));

      jitCopy(address + i, chunk, read);
    }

    // Map the .data section as RW.
    if (mmap(
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
    for (long i = dataSectionOffset; i < file.length(); i += chunk.length) {
      api.memset(chunk, 0, chunk.length);

      file.seek(i);
      int read = file.read(chunk, 0, (int) Math.min(file.length() - i, chunk.length));

      api.memcpy(address + i, chunk, read);
    }

    return address;
  }
}
