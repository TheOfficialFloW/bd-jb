/*
 * Copyright (C) 2021 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.api;

public final class Int64 extends IntBase {
  public static final int SIZE = 8;

  public Int64(int[] dimensions) {
    super(dimensions);
  }

  public Int64() {
    super();
  }

  public Int64(long value) {
    this();
    this.set(value);
  }

  int elementSize() {
    return SIZE;
  }

  public long get() {
    return api.read64(address);
  }

  public void set(long value) {
    api.write64(address, value);
  }

  public long get(int[] indices) {
    return api.read64(address + offset(indices));
  }

  public void set(int[] indices, long value) {
    api.write64(address + offset(indices), value);
  }
}
