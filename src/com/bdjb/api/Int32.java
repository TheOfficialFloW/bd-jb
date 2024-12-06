/*
 * Copyright (C) 2021-2024 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.api;

public final class Int32 extends Buffer {
  public static final int SIZE = 4;

  public Int32() {
    super(SIZE);
  }

  public Int32(int value) {
    this();
    set(value);
  }

  public int get() {
    return getInt(0x00);
  }

  public void set(int value) {
    putInt(0x00, value);
  }
}
