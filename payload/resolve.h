/*
 * Copyright (C) 2021-2024 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

#ifndef __RESOLVE_H__
#define __RESOLVE_H__

#define LIBC_MODULE_HANDLE 0x2
#define LIBKERNEL_MODULE_HANDLE 0x2001

typedef int32_t SceKernelModule;

void resolve_imports(void *(*dlsym)(SceKernelModule handle,
                                    const char *symbol));

#endif
