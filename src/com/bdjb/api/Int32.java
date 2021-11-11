/*
 * Copyright (C) 2021 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.api;

public final class Int32 extends IntBase {
  public static final int SIZE = 4;

  public Int32(int[] dimensions) {
    super(dimensions);
  }

  public Int32() {
    super();
  }

  public Int32(int value) {
    this();
    this.set(value);
  }

  protected int elementSize() {
    return SIZE;
  }

  public int get() {
    return api.read32(address);
  }

  public void set(int value) {
    api.write32(address, value);
  }

  public int get(int[] indices) {
    return api.read32(address + offset(indices));
  }

  public void set(int[] indices, int value) {
    api.write32(address + offset(indices), value);
  }
}
