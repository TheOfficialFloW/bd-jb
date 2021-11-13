/*
 * Copyright (C) 2021 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.api;

public final class Int32 extends AbstractInt {
  public static final int SIZE = 4;

  public Int32(int[] dimensions) {
    super(dimensions, SIZE);
  }

  public Int32() {
    super(SIZE);
  }

  public Int32(int value) {
    this();
    this.set(value);
  }

  public int get() {
    return getInt(0x00);
  }

  public void set(int value) {
    putInt(0x00, value);
  }

  public int get(int[] indices) {
    return getInt(offset(indices));
  }

  public void set(int[] indices, int value) {
    putInt(offset(indices), value);
  }
}
