/*
 * Copyright (C) 2021 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.api;

abstract class IntBase {
  static final API api;

  static {
    try {
      api = API.getInstance();
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  final long address;

  final int size;

  final int[] dimensions;

  IntBase(int[] dimensions) {
    this.dimensions = dimensions;
    this.size = size(dimensions);
    this.address = api.malloc(size);
  }

  IntBase() {
    this(new int[] {1});
  }

  abstract int elementSize();

  public void finalize() {
    api.free(address);
  }

  public long address() {
    return address;
  }

  public int size() {
    return size;
  }

  public int size(int[] dimensions) {
    assert (dimensions.length > 0);
    int size = 1;
    for (int i = 0; i < dimensions.length; i++) {
      size *= dimensions[i];
    }
    size *= elementSize();
    return size;
  }

  public int offset(int[] indices) {
    assert (indices.length == dimensions.length);
    int offset = 0;
    int stride = 1;
    for (int i = indices.length - 1; i >= 0; i--) {
      offset += stride * indices[i];
      stride *= dimensions[i];
    }
    offset *= elementSize();
    checkOffset(offset);
    return offset;
  }

  private void checkOffset(int offset) {
    if (offset < 0 || offset >= size) {
      throw new IndexOutOfBoundsException();
    }
  }
}
