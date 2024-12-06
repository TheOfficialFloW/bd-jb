/*
 * Copyright (C) 2021-2024 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.api;

public final class Int64Array extends Buffer {
  public Int64Array(int length) {
    super(Int64.SIZE);
  }

  public long get(int index) {
    return getLong(index * Int64.SIZE);
  }

  public void set(int index, long value) {
    putLong(index * Int64.SIZE, value);
  }
}
