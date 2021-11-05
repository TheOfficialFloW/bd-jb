/*
 * Copyright (C) 2021 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.api;

import java.lang.reflect.Field;

interface UnsafeInterface {
  public byte getByte(long address);

  public short getShort(long address);

  public int getInt(long address);

  public long getLong(long address);

  public long getLong(Object o, long offset);

  public void putByte(long address, byte x);

  public void putShort(long address, short x);

  public void putInt(long address, int x);

  public void putLong(long address, long x);

  public void putObject(Object o, long offset, Object x);

  public long objectFieldOffset(Field f);

  public long allocateMemory(long bytes);

  public long reallocateMemory(long address, long bytes);

  public void freeMemory(long address);

  public void setMemory(long address, long bytes, byte value);

  public void copyMemory(long srcAddress, long destAddress, long bytes);
}
