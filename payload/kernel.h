/*
 * Copyright (C) 2021-2024 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

#ifndef __KERNEL_H__
#define __KERNEL_H__

int kread(void *dest, uintptr_t src, size_t n);
uint8_t kread8(uintptr_t addr);
uint16_t kread16(uintptr_t addr);
uint32_t kread32(uintptr_t addr);
uint64_t kread64(uintptr_t addr);

int kwrite(uintptr_t dest, const void *src, size_t n);
void kwrite8(uintptr_t addr, uint8_t val);
void kwrite16(uintptr_t addr, uint16_t val);
void kwrite32(uintptr_t addr, uint32_t val);
void kwrite64(uintptr_t addr, uint64_t val);

#endif
