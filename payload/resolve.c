/*
 * Copyright (C) 2021-2024 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

#include <stdint.h>

#include "resolve.h"

void resolve_imports(void *(*dlsym)(SceKernelModule handle,
                                    const char *symbol)) {
#define LIBKERNEL_RESOLVE(name)                                                \
  extern void *name;                                                           \
  *(uintptr_t *)((uintptr_t)&name + 0x6) =                                     \
      (uintptr_t)dlsym(LIBKERNEL_MODULE_HANDLE, #name)

  LIBKERNEL_RESOLVE(__error);
  LIBKERNEL_RESOLVE(close);
  LIBKERNEL_RESOLVE(connect);
  LIBKERNEL_RESOLVE(dup);
  LIBKERNEL_RESOLVE(dup2);
  LIBKERNEL_RESOLVE(inet_pton);
  LIBKERNEL_RESOLVE(read);
  LIBKERNEL_RESOLVE(socket);
  LIBKERNEL_RESOLVE(write);
#undef LIBKERNEL_RESOLVE

#define LIBC_RESOLVE(name)                                                     \
  extern void *name;                                                           \
  *(uintptr_t *)((uintptr_t)&name + 0x6) =                                     \
      (uintptr_t)dlsym(LIBC_MODULE_HANDLE, #name)

  LIBC_RESOLVE(printf);
  LIBC_RESOLVE(putchar);
  LIBC_RESOLVE(puts);
  LIBC_RESOLVE(vsnprintf);
#undef LIBC_RESOLVE
}
