/*
 * Copyright (C) 2021-2024 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

.intel_syntax noprefix
.section .text

.macro stub
jmp qword [rip-8]
.skip 8
.endm

// libkernel

.global __error
__error: stub

.global close
close: stub

.global connect
connect: stub

.global dup
dup: stub

.global dup2
dup2: stub

.global inet_pton
inet_pton: stub

.global read
read: stub

.global socket
socket: stub

.global write
write: stub

// libc

.global printf
printf: stub

.global putchar
putchar: stub

.global puts
puts: stub

.global vsnprintf
vsnprintf: stub
