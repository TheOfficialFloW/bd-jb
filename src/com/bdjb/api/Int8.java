/*
 * Copyright (C) 2021-2024 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.api;

public final class Int8 extends Buffer {
  public static final int SIZE = 1;

  public Int8() {
    super(SIZE);
  }

  public Int8(byte value) {
    this();
    set(value);
  }

  public byte get() {
    return getByte(0x00);
  }

  public void set(byte value) {
    putByte(0x00, value);
  }
}
