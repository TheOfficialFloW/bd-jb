/*
 * Copyright (C) 2021-2024 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.api;

public final class Int16Array extends Buffer {
  public Int16Array(int length) {
    super(Int16.SIZE);
  }

  public short get(int index) {
    return getShort(index * Int16.SIZE);
  }

  public void set(int index, short value) {
    putShort(index * Int16.SIZE, value);
  }
}
