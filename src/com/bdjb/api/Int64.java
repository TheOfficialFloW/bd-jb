/*
 * Copyright (C) 2021 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.api;

public final class Int64 extends AbstractInt {
  public static final int SIZE = 8;

  public Int64() {
    super(SIZE);
  }

  public Int64(long address) {
    super(address, SIZE);
  }

  public Int64(int[] dimensions) {
    super(dimensions, SIZE);
  }

  public Int64(long address, int[] dimensions) {
    super(address, dimensions, SIZE);
  }

  public long get() {
    return getLong(0x00);
  }

  public void set(long value) {
    putLong(0x00, value);
  }

  public long get(int[] indices) {
    return getLong(offset(indices));
  }

  public void set(int[] indices, long value) {
    putLong(offset(indices), value);
  }
}
