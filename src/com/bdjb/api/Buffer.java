/*
 * Copyright (C) 2021 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.api;

public class Buffer {
  private static final API api;

  static {
    try {
      api = API.getInstance();
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private final long address;

  private final int size;

  public Buffer(int size) {
    this.address = api.malloc(size);
    this.size = size;
  }

  public void finalize() {
    api.free(address);
  }

  public long address() {
    return address;
  }

  public int size() {
    return size;
  }

  public byte getByte(int offset) {
    checkOffset(offset);
    return api.read8(address + offset);
  }

  public short getShort(int offset) {
    checkOffset(offset);
    return api.read16(address + offset);
  }

  public int getInt(int offset) {
    checkOffset(offset);
    return api.read32(address + offset);
  }

  public long getLong(int offset) {
    checkOffset(offset);
    return api.read64(address + offset);
  }

  public void putByte(int offset, byte value) {
    checkOffset(offset);
    api.write8(address + offset, value);
  }

  public void putShort(int offset, short value) {
    checkOffset(offset);
    api.write16(address + offset, value);
  }

  public void putInt(int offset, int value) {
    checkOffset(offset);
    api.write32(address + offset, value);
  }

  public void putLong(int offset, long value) {
    checkOffset(offset);
    api.write64(address + offset, value);
  }

  public void fill(byte value) {
    api.memset(address, value, size);
  }

  private void checkOffset(int offset) {
    if (offset < 0 || offset >= size) {
      throw new IndexOutOfBoundsException();
    }
  }
}
