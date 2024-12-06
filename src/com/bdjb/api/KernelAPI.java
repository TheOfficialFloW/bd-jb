/*
 * Copyright (C) 2021-2024 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.bdjb.api;

/** API class to access kernel native data. */
public class KernelAPI {
  public static final int PAGE_SIZE = 0x4000;

  private static final int F_SETFL = 4;
  private static final int O_NONBLOCK = 4;

  private static final int PIPEBUF_SIZE = 0x18;

  private static final String PIPE_SYMBOL = "pipe";
  private static final String READ_SYMBOL = "read";
  private static final String WRITE_SYMBOL = "write";
  private static final String FCNTL_SYMBOL = "fcntl";
  private static final String CLOSE_SYMBOL = "close";

  private static final API api;

  private static KernelAPI instance;

  static {
    try {
      api = API.getInstance();
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private long pipe;
  private long read;
  private long write;
  private long fcntl;
  private long close;

  private long kaslrOffset;

  private Int32Array masterPipeFd = new Int32Array(2);
  private Int32Array victimPipeFd = new Int32Array(2);

  private int masterRpipeFd;
  private int masterWpipeFd;
  private int victimRpipeFd;
  private int victimWpipeFd;

  private Buffer victimPipebuf = new Buffer(PIPEBUF_SIZE);
  private Buffer tmp = new Buffer(PAGE_SIZE);

  private KernelAPI() {
    this.init();
  }

  public static synchronized KernelAPI getInstance() {
    if (instance == null) {
      instance = new KernelAPI();
    }
    return instance;
  }

  private void init() {
    initSymbols();
    initPipes();
  }

  private void initSymbols() {
    pipe = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, PIPE_SYMBOL);
    read = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, READ_SYMBOL);
    write = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, WRITE_SYMBOL);
    fcntl = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, FCNTL_SYMBOL);
    close = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, CLOSE_SYMBOL);

    if (pipe == 0 || read == 0 || write == 0 || fcntl == 0 || close == 0) {
      throw new InternalError("symbols not found");
    }
  }

  private void initPipes() {
    pipe(masterPipeFd);
    pipe(victimPipeFd);

    masterRpipeFd = masterPipeFd.get(0);
    masterWpipeFd = masterPipeFd.get(1);
    victimRpipeFd = victimPipeFd.get(0);
    victimWpipeFd = victimPipeFd.get(1);

    fcntl(masterRpipeFd, F_SETFL, O_NONBLOCK);
    fcntl(masterWpipeFd, F_SETFL, O_NONBLOCK);
    fcntl(victimRpipeFd, F_SETFL, O_NONBLOCK);
    fcntl(victimWpipeFd, F_SETFL, O_NONBLOCK);
  }

  private int pipe(Int32Array fildes) {
    return (int) api.call(pipe, fildes != null ? fildes.address() : 0);
  }

  private int fcntl(int fd, int cmd, long arg0) {
    return (int) api.call(fcntl, fd, cmd, arg0);
  }

  private long read(int fd, Buffer buf, long nbytes) {
    return api.call(read, fd, buf != null ? buf.address() : 0, nbytes);
  }

  private long write(int fd, Buffer buf, long nbytes) {
    return api.call(write, fd, buf != null ? buf.address() : 0, nbytes);
  }

  private int close(int fd) {
    return (int) api.call(close, fd);
  }

  private int corruptPipebuf(int cnt, int in, int out, int size, long buffer) {
    if (buffer == 0) {
      throw new IllegalArgumentException("buffer cannot be zero");
    }
    victimPipebuf.putInt(0x00, cnt); // cnt
    victimPipebuf.putInt(0x04, in); // in
    victimPipebuf.putInt(0x08, out); // out
    victimPipebuf.putInt(0x0C, size); // size
    victimPipebuf.putLong(0x10, buffer); // buffer
    write(masterWpipeFd, victimPipebuf, victimPipebuf.size());
    return (int) read(masterRpipeFd, victimPipebuf, victimPipebuf.size());
  }

  public int kread(Buffer dest, long src, long n) {
    corruptPipebuf((int) n, 0, 0, PAGE_SIZE, src);
    return (int) read(victimRpipeFd, dest, n);
  }

  public int kwrite(long dest, Buffer src, long n) {
    corruptPipebuf(0, 0, 0, PAGE_SIZE, dest);
    return (int) write(victimWpipeFd, src, n);
  }

  public byte kread8(long addr) {
    kread(tmp, addr, Int8.SIZE);
    return tmp.getByte(0x00);
  }

  public short kread16(long addr) {
    kread(tmp, addr, Int16.SIZE);
    return tmp.getShort(0x00);
  }

  public int kread32(long addr) {
    kread(tmp, addr, Int32.SIZE);
    return tmp.getInt(0x00);
  }

  public long kread64(long addr) {
    kread(tmp, addr, Int64.SIZE);
    return tmp.getLong(0x00);
  }

  public void kwrite8(long addr, byte val) {
    tmp.putByte(0x00, val);
    kwrite(addr, tmp, Int8.SIZE);
  }

  public void krite16(long addr, short val) {
    tmp.putShort(0x00, val);
    kwrite(addr, tmp, Int16.SIZE);
  }

  public void kwrite32(long addr, int val) {
    tmp.putInt(0x00, val);
    kwrite(addr, tmp, Int32.SIZE);
  }

  public void kwrite64(long addr, long val) {
    tmp.putLong(0x00, val);
    kwrite(addr, tmp, Int64.SIZE);
  }

  public Int32Array getMasterPipeFd() {
    return masterPipeFd;
  }

  public Int32Array getVictimPipeFd() {
    return victimPipeFd;
  }

  public long getKaslrOffset() {
    return kaslrOffset;
  }

  public void setKaslrOffset(long offset) {
    kaslrOffset = offset;
  }
}
