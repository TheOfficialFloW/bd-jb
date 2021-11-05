/*
 * Copyright (C) 2021 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.api;

import java.lang.reflect.Field;
import sun.misc.Unsafe;

class UnsafeSunImpl implements UnsafeInterface {
  private static final String UNSAFE_CLASS_NAME = "sun.misc.Unsafe";
  private static final String THE_UNSAFE_FIELD_NAME = "theUnsafe";

  private final Unsafe unsafe;

  UnsafeSunImpl() throws Exception {
    // Throw exception if class does not exist.
    Class.forName(UNSAFE_CLASS_NAME);

    // Get unsafe instance.
    Field theUnsafeField = Unsafe.class.getDeclaredField(THE_UNSAFE_FIELD_NAME);
    theUnsafeField.setAccessible(true);
    unsafe = (Unsafe) theUnsafeField.get(null);
  }

  public byte getByte(long address) {
    return unsafe.getByte(address);
  }

  public short getShort(long address) {
    return unsafe.getShort(address);
  }

  public int getInt(long address) {
    return unsafe.getInt(address);
  }

  public long getLong(long address) {
    return unsafe.getLong(address);
  }

  public long getLong(Object o, long offset) {
    return unsafe.getLong(o, offset);
  }

  public void putByte(long address, byte x) {
    unsafe.putByte(address, x);
  }

  public void putShort(long address, short x) {
    unsafe.putShort(address, x);
  }

  public void putInt(long address, int x) {
    unsafe.putInt(address, x);
  }

  public void putLong(long address, long x) {
    unsafe.putLong(address, x);
  }

  public void putObject(Object o, long offset, Object x) {
    unsafe.putObject(o, offset, x);
  }

  public long objectFieldOffset(Field f) {
    return unsafe.objectFieldOffset(f);
  }

  public long allocateMemory(long bytes) {
    return unsafe.allocateMemory(bytes);
  }

  public long reallocateMemory(long address, long bytes) {
    return unsafe.reallocateMemory(address, bytes);
  }

  public void freeMemory(long address) {
    unsafe.freeMemory(address);
  }

  public void setMemory(long address, long bytes, byte value) {
    unsafe.setMemory(address, bytes, value);
  }

  public void copyMemory(long srcAddress, long destAddress, long bytes) {
    unsafe.copyMemory(srcAddress, destAddress, bytes);
  }
}
