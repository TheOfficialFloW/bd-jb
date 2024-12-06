/*
 * Copyright (C) 2021-2024 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.api;

public final class Int16 extends Buffer {
  public static final int SIZE = 2;

  public Int16() {
    super(SIZE);
  }

  public Int16(short value) {
    this();
    set(value);
  }

  public short get() {
    return getShort(0x00);
  }

  public void set(short value) {
    putShort(0x00, value);
  }
}
