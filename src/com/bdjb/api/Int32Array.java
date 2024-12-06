/*
 * Copyright (C) 2021-2024 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.api;

public final class Int32Array extends Buffer {
  public Int32Array(int length) {
    super(length * Int32.SIZE);
  }

  public int get(int index) {
    return getInt(index * Int32.SIZE);
  }

  public void set(int index, int value) {
    putInt(index * Int32.SIZE, value);
  }
}
