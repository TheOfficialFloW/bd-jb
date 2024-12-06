/*
 * Copyright (C) 2021-2024 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.api;

public final class Int64 extends Buffer {
  public static final int SIZE = 8;

  public Int64() {
    super(SIZE);
  }

  public Int64(long value) {
    this();
    set(value);
  }

  public long get() {
    return getLong(0x00);
  }

  public void set(long value) {
    putLong(0x00, value);
  }
}
