/*
 * Copyright (C) 2021 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.api;

public final class Int16 extends IntBase {
  public static final int SIZE = 2;

  public Int16(int[] dimensions) {
    super(dimensions);
  }

  public Int16() {
    super();
  }

  public Int16(short value) {
    this();
    this.set(value);
  }

  int elementSize() {
    return SIZE;
  }

  public short get() {
    return api.read16(address);
  }

  public void set(short value) {
    api.write16(address, value);
  }

  public short get(int[] indices) {
    return api.read16(address + offset(indices));
  }

  public void set(int[] indices, short value) {
    api.write16(address + offset(indices), value);
  }
}
