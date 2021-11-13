/*
 * Copyright (C) 2021 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.api;

public final class Int16 extends AbstractInt {
  public static final int SIZE = 2;

  public Int16() {
    super(SIZE);
  }

  public Int16(long address) {
    super(address, SIZE);
  }

  public Int16(int[] dimensions) {
    super(dimensions, SIZE);
  }

  public Int16(long address, int[] dimensions) {
    super(address, dimensions, SIZE);
  }

  public short get() {
    return getShort(0x00);
  }

  public void set(short value) {
    putShort(0x00, value);
  }

  public short get(int[] indices) {
    return getShort(offset(indices));
  }

  public void set(int[] indices, short value) {
    putShort(offset(indices), value);
  }
}
