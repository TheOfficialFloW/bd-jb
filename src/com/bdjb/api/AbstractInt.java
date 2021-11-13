/*
 * Copyright (C) 2021 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.api;

abstract class AbstractInt extends Buffer {
  private final int[] dimensions;

  private final int elementSize;

  protected AbstractInt(int[] dimensions, int elementSize) {
    super(size(dimensions, elementSize));
    this.dimensions = dimensions;
    this.elementSize = elementSize;
  }

  protected AbstractInt(long address, int[] dimensions, int elementSize) {
    super(address, size(dimensions, elementSize));
    this.dimensions = dimensions;
    this.elementSize = elementSize;
  }

  protected AbstractInt(long address, int elementSize) {
    this(address, new int[] {1}, elementSize);
  }

  protected AbstractInt(int elementSize) {
    this(new int[] {1}, elementSize);
  }

  static int size(int[] dimensions, int elementSize) {
    assert (dimensions.length > 0);
    int size = 1;
    for (int i = 0; i < dimensions.length; i++) {
      size *= dimensions[i];
    }
    size *= elementSize;
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
    offset *= elementSize;
    checkOffset(offset, elementSize);
    return offset;
  }
}
