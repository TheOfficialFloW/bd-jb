/*
 * Copyright (C) 2021-2024 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.api;

public final class Int8Array extends Buffer {
  public Int8Array(int length) {
    super(Int8.SIZE);
  }

  public byte get(int index) {
    return getByte(index * Int8.SIZE);
  }

  public void set(int index, byte value) {
    putByte(index * Int8.SIZE, value);
  }
}
