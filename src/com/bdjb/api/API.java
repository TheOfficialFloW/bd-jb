/*
 * Copyright (C) 2021-2024 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.api;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** API class to access native data and execute native code. */
public final class API {
  public static final int RTLD_DEFAULT = -2;

  public static final int LIBC_MODULE_HANDLE = 0x2;
  public static final int LIBKERNEL_MODULE_HANDLE = 0x2001;
  public static final int LIBJAVA_MODULE_HANDLE = 0x4A;

  private static final String UNSUPPORTED_DLOPEN_OPERATION_STRING =
      "Unsupported dlopen() operation";

  private static final String JAVA_JAVA_LANG_REFLECT_ARRAY_MULTI_NEW_ARRAY_SYMBOL =
      "Java_java_lang_reflect_Array_multiNewArray";
  private static final String JVM_NATIVE_PATH_SYMBOL = "JVM_NativePath";
  private static final String SIGSETJMP_SYMBOL = "sigsetjmp";
  private static final String UX86_64_SETCONTEXT_SYMBOL = "__Ux86_64_setcontext";
  private static final String ERROR_SYMBOL = "__error";

  private static final String MULTI_NEW_ARRAY_METHOD_NAME = "multiNewArray";
  private static final String MULTI_NEW_ARRAY_METHOD_SIGNATURE = "(J[I)J";

  private static final String NATIVE_LIBRARY_CLASS_NAME = "java.lang.ClassLoader$NativeLibrary";
  private static final String FIND_METHOD_NAME = "find";
  private static final String FIND_ENTRY_METHOD_NAME = "findEntry";
  private static final String HANDLE_FIELD_NAME = "handle";

  private static final String VALUE_FIELD_NAME = "value";

  private static final int[] MULTI_NEW_ARRAY_DIMENSIONS = new int[] {1};

  private static final int ARRAY_BASE_OFFSET = 0x18;

  private static final ThreadLocal callContexts = new ThreadLocal();

  private static API instance;

  private UnsafeInterface unsafe;

  private Object nativeLibrary;
  private Method findMethod;
  private Field handleField;

  private long executableHandle;

  private long Java_java_lang_reflect_Array_multiNewArray;
  private long JVM_NativePath;
  private long sigsetjmp;
  private long __Ux86_64_setcontext;
  private long __error;

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

  private native long multiNewArray(long componentType, int[] dimensions);

  public boolean isJdk11() {
    return jdk11;
  }

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
      throw new InternalError("JVM_NativePath not found");
    }

    __Ux86_64_setcontext = dlsym(LIBKERNEL_MODULE_HANDLE, UX86_64_SETCONTEXT_SYMBOL);
    if (__Ux86_64_setcontext == 0) {
      // In earlier versions, there's a bug where only the main executable's handle is used.
      executableHandle = JVM_NativePath & -4;
      while (strcmp(executableHandle, UNSUPPORTED_DLOPEN_OPERATION_STRING) != 0) {
        executableHandle += 4;
      }
      executableHandle -= 4;

      // Try again.
      __Ux86_64_setcontext = dlsym(LIBKERNEL_MODULE_HANDLE, UX86_64_SETCONTEXT_SYMBOL);
    }
    if (__Ux86_64_setcontext == 0) {
      throw new InternalError("__Ux86_64_setcontext not found");
    }

    if (jdk11) {
      Java_java_lang_reflect_Array_multiNewArray =
          dlsym(LIBJAVA_MODULE_HANDLE, JAVA_JAVA_LANG_REFLECT_ARRAY_MULTI_NEW_ARRAY_SYMBOL);
    } else {
      Java_java_lang_reflect_Array_multiNewArray =
          dlsym(RTLD_DEFAULT, JAVA_JAVA_LANG_REFLECT_ARRAY_MULTI_NEW_ARRAY_SYMBOL);
    }
    if (Java_java_lang_reflect_Array_multiNewArray == 0) {
      throw new InternalError("Java_java_lang_reflect_Array_multiNewArray not found");
    }

    sigsetjmp = dlsym(LIBKERNEL_MODULE_HANDLE, SIGSETJMP_SYMBOL);
    if (sigsetjmp == 0) {
      throw new InternalError("sigsetjmp not found");
    }

    __error = dlsym(LIBKERNEL_MODULE_HANDLE, ERROR_SYMBOL);
    if (__error == 0) {
      throw new InternalError("__error not found");
    }
  }

  private void initApiCall() {
    long apiInstance = addrof(this);
    long apiKlass = read64(apiInstance + 0x08);

    boolean installed = false;
    if (jdk11) {
      long methods = read64(apiKlass + 0x170);
      int numMethods = read32(methods + 0x00);

      for (int i = 0; i < numMethods; i++) {
        long method = read64(methods + 0x08 + i * 8);
        long constMethod = read64(method + 0x08);
        long constants = read64(constMethod + 0x08);
        short nameIndex = read16(constMethod + 0x2A);
        short signatureIndex = read16(constMethod + 0x2C);
        long nameSymbol = read64(constants + 0x40 + nameIndex * 8) & -2;
        long signatureSymbol = read64(constants + 0x40 + signatureIndex * 8) & -2;
        short nameLength = read16(nameSymbol + 0x00);
        short signatureLength = read16(signatureSymbol + 0x00);

        String name = readString(nameSymbol + 0x06, nameLength);
        String signature = readString(signatureSymbol + 0x06, signatureLength);
        if (name.equals(MULTI_NEW_ARRAY_METHOD_NAME)
            && signature.equals(MULTI_NEW_ARRAY_METHOD_SIGNATURE)) {
          write64(method + 0x50, Java_java_lang_reflect_Array_multiNewArray);
          installed = true;
          break;
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
        long nameSymbol = read64(constants + 0x40 + nameIndex * 8) & -2;
        long signatureSymbol = read64(constants + 0x40 + signatureIndex * 8) & -2;
        short nameLength = read16(nameSymbol + 0x08);
        short signatureLength = read16(signatureSymbol + 0x08);

        String name = readString(nameSymbol + 0x0A, nameLength);
        String signature = readString(signatureSymbol + 0x0A, signatureLength);
        if (name.equals(MULTI_NEW_ARRAY_METHOD_NAME)
            && signature.equals(MULTI_NEW_ARRAY_METHOD_SIGNATURE)) {
          write64(method + 0x78, Java_java_lang_reflect_Array_multiNewArray);
          installed = true;
          break;
        }
      }
    }

    if (!installed) {
      throw new InternalError("installing native method failed");
    }

    // Invoke call method many times to kick in optimization.
    train();
  }

  private void train() {
    for (int i = 0; i < 10000; i++) {
      call(0);
    }
  }

  private void buildContext(
      long[] contextBuf,
      long[] jmpBuf,
      int offset,
      long rip,
      long rdi,
      long rsi,
      long rdx,
      long rcx,
      long r8,
      long r9) {
    long rbx = jmpBuf[(offset + 0x08) / 8];
    long rsp = jmpBuf[(offset + 0x10) / 8];
    long rbp = jmpBuf[(offset + 0x18) / 8];
    long r12 = jmpBuf[(offset + 0x20) / 8];
    long r13 = jmpBuf[(offset + 0x28) / 8];
    long r14 = jmpBuf[(offset + 0x30) / 8];
    long r15 = jmpBuf[(offset + 0x38) / 8];

    contextBuf[(offset + 0x48) / 8] = rdi;
    contextBuf[(offset + 0x50) / 8] = rsi;
    contextBuf[(offset + 0x58) / 8] = rdx;
    contextBuf[(offset + 0x60) / 8] = rcx;
    contextBuf[(offset + 0x68) / 8] = r8;
    contextBuf[(offset + 0x70) / 8] = r9;
    contextBuf[(offset + 0x80) / 8] = rbx;
    contextBuf[(offset + 0x88) / 8] = rbp;
    contextBuf[(offset + 0xA0) / 8] = r12;
    contextBuf[(offset + 0xA8) / 8] = r13;
    contextBuf[(offset + 0xB0) / 8] = r14;
    contextBuf[(offset + 0xB8) / 8] = r15;
    contextBuf[(offset + 0xE0) / 8] = rip;
    contextBuf[(offset + 0xF8) / 8] = rsp;
  }

  public long call(long func, long arg0, long arg1, long arg2, long arg3, long arg4, long arg5) {
    long ret = 0;

    // When func is 0, only do one iteration to avoid calling __Ux86_64_setcontext.
    // This is used to "train" this function to kick in optimization early. Otherwise, it is
    // possible that optimization kicks in between the calls to sigsetjmp and __Ux86_64_setcontext
    // leading to different stack layouts of the two calls.
    int iter = func == 0 ? 1 : 2;

    CallContext callContext = getCallContext();

    if (jdk11) {
      callContext.fakeKlass[0xC0 / 8] = 0; // dimension

      for (int i = 0; i < iter; i++) {
        callContext.fakeKlass[0x00 / 8] = callContext.fakeKlassVtableAddr;
        callContext.fakeKlass[0x00 / 8] = callContext.fakeKlassVtableAddr;
        if (i == 0) {
          callContext.fakeKlassVtable[0x158 / 8] = sigsetjmp + 0x23; // multi_allocate
        } else {
          callContext.fakeKlassVtable[0x158 / 8] = __Ux86_64_setcontext + 0x39; // multi_allocate
        }

        ret = multiNewArray(callContext.fakeClassOopAddr, MULTI_NEW_ARRAY_DIMENSIONS);

        if (i == 0) {
          buildContext(
              callContext.fakeKlass,
              callContext.fakeKlass,
              0x00,
              func,
              arg0,
              arg1,
              arg2,
              arg3,
              arg4,
              arg5);
        }
      }
    } else {
      callContext.fakeKlass[0xB8 / 8] = 0; // dimension

      for (int i = 0; i < iter; i++) {
        callContext.fakeKlass[0x10 / 8] = callContext.fakeKlassVtableAddr;
        callContext.fakeKlass[0x20 / 8] = callContext.fakeKlassVtableAddr;
        if (i == 0) {
          callContext.fakeKlassVtable[0x230 / 8] = sigsetjmp + 0x23; // multi_allocate
        } else {
          callContext.fakeKlassVtable[0x230 / 8] = __Ux86_64_setcontext + 0x39; // multi_allocate
        }

        ret = multiNewArray(callContext.fakeClassOopAddr, MULTI_NEW_ARRAY_DIMENSIONS);

        if (i == 0) {
          buildContext(
              callContext.fakeKlass,
              callContext.fakeKlass,
              0x20,
              func,
              arg0,
              arg1,
              arg2,
              arg3,
              arg4,
              arg5);
        }
      }
    }

    if (ret == 0) {
      return 0;
    }

    return read64(ret);
  }

  public long call(long func, long arg0, long arg1, long arg2, long arg3, long arg4) {
    return call(func, arg0, arg1, arg2, arg3, arg4, (long) 0);
  }

  public long call(long func, long arg0, long arg1, long arg2, long arg3) {
    return call(func, arg0, arg1, arg2, arg3, (long) 0, (long) 0);
  }

  public long call(long func, long arg0, long arg1, long arg2) {
    return call(func, arg0, arg1, arg2, (long) 0, (long) 0, (long) 0);
  }

  public long call(long func, long arg0, long arg1) {
    return call(func, arg0, arg1, (long) 0, (long) 0, (long) 0, (long) 0);
  }

  public long call(long func, long arg0) {
    return call(func, arg0, (long) 0, (long) 0, (long) 0, (long) 0, (long) 0);
  }

  public long call(long func) {
    return call(func, (long) 0, (long) 0, (long) 0, (long) 0, (long) 0, (long) 0);
  }

  public int errno() {
    return read32(call(__error));
  }

  public long dlsym(long handle, String symbol) {
    int oldHandle = RTLD_DEFAULT;
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
    } catch (IllegalAccessException e) {
      return 0;
    } catch (InvocationTargetException e) {
      return 0;
    } finally {
      if (executableHandle != 0) {
        write32(executableHandle, oldHandle);
      }
    }
  }

  public long addrof(Object obj) {
    Object[] array = new Object[] {obj};
    return unsafe.getLong(array, ARRAY_BASE_OFFSET);
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

  public long calloc(long number, long size) {
    long p = malloc(number * size);
    if (p != 0) {
      memset(p, 0, number * size);
    }
    return p;
  }

  public long realloc(long ptr, long size) {
    return unsafe.reallocateMemory(ptr, size);
  }

  public void free(long ptr) {
    unsafe.freeMemory(ptr);
  }

  public long memcpy(long dest, long src, long n) {
    unsafe.copyMemory(src, dest, n);
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
    unsafe.setMemory(s, n, (byte) c);
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
    for (int i = 0; ; i++) {
      byte b1 = read8(s1 + i);
      byte b2 = read8(s2 + i);
      if (b1 != b2) {
        return (int) b1 - (int) b2;
      }
      if (b1 == (byte) 0 && b2 == (byte) 0) {
        return 0;
      }
    }
  }

  public int strcmp(long s1, String s2) {
    byte[] bytes = toCBytes(s2);
    for (int i = 0; ; i++) {
      byte b1 = read8(s1 + i);
      byte b2 = bytes[i];
      if (b1 != b2) {
        return (int) b1 - (int) b2;
      }
      if (b1 == (byte) 0 && b2 == (byte) 0) {
        return 0;
      }
    }
  }

  public int strcmp(String s1, long s2) {
    return strcmp(s2, s1);
  }

  public long strcpy(long dest, long src) {
    for (int i = 0; ; i++) {
      byte ch = read8(src + i);
      write8(dest + i, ch);
      if (ch == (byte) 0) {
        break;
      }
    }
    return dest;
  }

  public long strcpy(long dest, String src) {
    byte[] bytes = toCBytes(src);
    for (int i = 0; ; i++) {
      byte ch = bytes[i];
      write8(dest + i, ch);
      if (ch == (byte) 0) {
        break;
      }
    }
    return dest;
  }

  public String readString(long src, long n) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    for (int i = 0; ; i++) {
      byte ch = read8(src + i);
      if (ch == (byte) 0 || i == n) {
        break;
      }
      outputStream.write(new byte[] {ch}, 0, 1);
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

  private CallContext getCallContext() {
    CallContext callContext = (CallContext) callContexts.get();
    if (callContext != null) {
      return callContext;
    }

    callContext = new CallContext();
    callContexts.set(callContext);
    return callContext;
  }

  class CallContext {
    final long[] fakeClassOop;
    final long[] fakeClass;
    final long[] fakeKlass;
    final long[] fakeKlassVtable;

    final long fakeClassOopAddr;
    final long fakeClassAddr;
    final long fakeKlassAddr;
    final long fakeKlassVtableAddr;

    private final long callContextBuffer;

    CallContext() {
      callContextBuffer =
          malloc(
              ARRAY_BASE_OFFSET
                  + Int64.SIZE
                  + ARRAY_BASE_OFFSET
                  + 0x100
                  + ARRAY_BASE_OFFSET
                  + 0x200
                  + ARRAY_BASE_OFFSET
                  + 0x400);
      if (callContextBuffer == 0) {
        throw new OutOfMemoryError("malloc failed");
      }

      // Get array addresses.
      fakeClassOopAddr = callContextBuffer + ARRAY_BASE_OFFSET;
      fakeClassAddr = fakeClassOopAddr + Int64.SIZE + ARRAY_BASE_OFFSET;
      fakeKlassAddr = fakeClassAddr + 0x100 + ARRAY_BASE_OFFSET;
      fakeKlassVtableAddr = fakeKlassAddr + 0x200 + ARRAY_BASE_OFFSET;

      long[] array = new long[1];
      long arrayAddr = addrof(array);
      long arrayKlass = read64(arrayAddr + 0x08);

      // Write array headers.
      write64(fakeClassOopAddr - 0x18, 1);
      write64(fakeClassAddr - 0x18, 1);
      write64(fakeKlassAddr - 0x18, 1);
      write64(fakeKlassVtableAddr - 0x18, 1);

      write64(fakeClassOopAddr - 0x10, arrayKlass);
      write64(fakeClassAddr - 0x10, arrayKlass);
      write64(fakeKlassAddr - 0x10, arrayKlass);
      write64(fakeKlassVtableAddr - 0x10, arrayKlass);

      write64(fakeClassOopAddr - 8, 0xFFFFFFFF);
      write64(fakeClassAddr - 8, 0xFFFFFFFF);
      write64(fakeKlassAddr - 8, 0xFFFFFFFF);
      write64(fakeKlassVtableAddr - 8, 0xFFFFFFFF);

      long[][] callContextArray = new long[4][0];
      long callContextArrayAddr = addrof(callContextArray) + ARRAY_BASE_OFFSET;

      // Put array addresses into callContextArray.
      write64(callContextArrayAddr + 0x00, fakeClassOopAddr - ARRAY_BASE_OFFSET);
      write64(callContextArrayAddr + 0x08, fakeClassAddr - ARRAY_BASE_OFFSET);
      write64(callContextArrayAddr + 0x10, fakeKlassAddr - ARRAY_BASE_OFFSET);
      write64(callContextArrayAddr + 0x18, fakeKlassVtableAddr - ARRAY_BASE_OFFSET);

      // Get fake arrays.
      fakeClassOop = callContextArray[0];
      fakeClass = callContextArray[1];
      fakeKlass = callContextArray[2];
      fakeKlassVtable = callContextArray[3];

      // Restore.
      write64(callContextArrayAddr + 0x00, 0);
      write64(callContextArrayAddr + 0x08, 0);
      write64(callContextArrayAddr + 0x10, 0);
      write64(callContextArrayAddr + 0x18, 0);

      if (jdk11) {
        fakeClassOop[0x00 / 8] = fakeClassAddr;
        fakeClass[0x98 / 8] = fakeKlassAddr;
        fakeKlassVtable[0xD8 / 8] = JVM_NativePath; // array_klass
      } else {
        fakeClassOop[0x00 / 8] = fakeClassAddr;
        fakeClass[0x68 / 8] = fakeKlassAddr;
        fakeKlassVtable[0x80 / 8] = JVM_NativePath; // array_klass
        fakeKlassVtable[0xF0 / 8] = JVM_NativePath; // oop_is_array
      }
    }

    protected void finalize() {
      free(callContextBuffer);
    }
  }
}
