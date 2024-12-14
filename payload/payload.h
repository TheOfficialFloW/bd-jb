/*
 * Copyright (C) 2021-2024 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

#ifndef __PAYLOAD_H__
#define __PAYLOAD_H__

typedef struct {
  void *dlsym;
  uint64_t kaslr_offset;
  int *master_pipe_fd;
  int *victim_pipe_fd;
} PayloadArgs;

extern PayloadArgs *payload_args;

#endif
