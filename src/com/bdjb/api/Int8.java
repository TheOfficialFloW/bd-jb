/*
 * Copyright (C) 2021 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.api;

public final class Int8 extends AbstractInt {
  public static final int SIZE = 1;

  public Int8() {
    super(SIZE);
  }

  public Int8(long address) {
    super(address, SIZE);
  }

  public Int8(int[] dimensions) {
    super(dimensions, SIZE);
  }

  public Int8(long address, int[] dimensions) {
    super(address, dimensions, SIZE);
  }

  public byte get() {
    return getByte(0x00);
  }

  public void set(byte value) {
    putByte(0x00, value);
  }

  public byte get(int[] indices) {
    return getByte(offset(indices));
  }

  public void set(int[] indices, byte value) {
    putByte(offset(indices), value);
  }
}
