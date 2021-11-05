/*
 * Copyright (C) 2021 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.api;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import jdk.internal.misc.Unsafe;

class UnsafeJdkImpl implements UnsafeInterface {
  private static final String GET_MODULE_METHOD_NAME = "getModule";

  private static final String MODULE_CLASS_NAME = "java.lang.Module";
  private static final String IMPL_ADD_OPENS_TO_ALL_UNNAMED_METHOD_NAME =
      "implAddOpensToAllUnnamed";

  private static final String UNSAFE_CLASS_NAME = "jdk.internal.misc.Unsafe";
  private static final String THE_UNSAFE_FIELD_NAME = "theUnsafe";

  private final Unsafe unsafe;

  UnsafeJdkImpl() throws Exception {
    // Throw exception if class does not exist.
    Class.forName(UNSAFE_CLASS_NAME);

    // Get unsafe module.
    Method getModuleMethod = Class.class.getDeclaredMethod(GET_MODULE_METHOD_NAME, null);
    getModuleMethod.setAccessible(true);
    Object unsafeModule = getModuleMethod.invoke(Unsafe.class, null);

    // Open unsafe package.
    Method implAddOpensToAllUnnamedMethod =
        Class.forName(MODULE_CLASS_NAME)
            .getDeclaredMethod(
                IMPL_ADD_OPENS_TO_ALL_UNNAMED_METHOD_NAME, new Class[] {String.class});
    implAddOpensToAllUnnamedMethod.setAccessible(true);
    implAddOpensToAllUnnamedMethod.invoke(
        unsafeModule, new Object[] {Unsafe.class.getPackage().getName()});

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
