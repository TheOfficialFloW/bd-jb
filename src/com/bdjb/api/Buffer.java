/*
 * Copyright (C) 2021 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.api;

public class Buffer {
  protected static final API api;

  static {
    try {
      api = API.getInstance();
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private final long address;

  private final int size;

  private final boolean allocated;

  public Buffer(int size) {
    this.address = api.malloc(size);
    this.size = size;
    this.allocated = true;
  }

  public Buffer(long address, int size) {
    this.address = address;
    this.size = size;
    this.allocated = false;
  }

  public void finalize() {
    if (allocated) {
      api.free(address);
    }
  }

  public long address() {
    return address;
  }

  public int size() {
    return size;
  }

  public byte getByte(int offset) {
    checkOffset(offset, Int8.SIZE);
    return api.read8(address + offset);
  }

  public short getShort(int offset) {
    checkOffset(offset, Int16.SIZE);
    return api.read16(address + offset);
  }

  public int getInt(int offset) {
    checkOffset(offset, Int32.SIZE);
    return api.read32(address + offset);
  }

  public long getLong(int offset) {
    checkOffset(offset, Int64.SIZE);
    return api.read64(address + offset);
  }

  public void putByte(int offset, byte value) {
    checkOffset(offset, Int8.SIZE);
    api.write8(address + offset, value);
  }

  public void putShort(int offset, short value) {
    checkOffset(offset, Int16.SIZE);
    api.write16(address + offset, value);
  }

  public void putInt(int offset, int value) {
    checkOffset(offset, Int32.SIZE);
    api.write32(address + offset, value);
  }

  public void putLong(int offset, long value) {
    checkOffset(offset, Int64.SIZE);
    api.write64(address + offset, value);
  }

  public void put(int offset, Buffer buffer) {
    checkOffset(offset, buffer.size());
    api.memcpy(address + offset, buffer.address(), buffer.size());
  }

  public void put(int offset, byte[] buffer) {
    checkOffset(offset, buffer.length);
    api.memcpy(address + offset, buffer, buffer.length);
  }

  public void fill(byte value) {
    api.memset(address, value, size);
  }

  protected void checkOffset(int offset, int length) {
    if (offset < 0 || length < 0 || (offset + length) > size) {
      throw new IndexOutOfBoundsException();
    }
  }
}
