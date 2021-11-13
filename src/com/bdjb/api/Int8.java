/*
 * Copyright (C) 2021 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.api;

public final class Int8 extends AbstractInt {
  public static final int SIZE = 1;

  public Int8(int[] dimensions) {
    super(dimensions);
  }

  public Int8() {
    super();
  }

  public Int8(byte value) {
    this();
    this.set(value);
  }

  protected int elementSize() {
    return SIZE;
  }

  public byte get() {
    return api.read8(address);
  }

  public void set(byte value) {
    api.write8(address, value);
  }

  public byte get(int[] indices) {
    return api.read8(address + offset(indices));
  }

  public void set(int[] indices, byte value) {
    api.write8(address + offset(indices), value);
  }
}
