/*
 * Copyright (C) 2021-2024 Andy Nguyen
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

#include <errno.h>
#include <netinet/in.h>
#include <stdarg.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h>

#include "kernel.h"
#include "payload.h"
#include "resolve.h"

#define LOG_IP "192.168.1.53"
#define LOG_PORT 1337

int inet_pton(int, const char *__restrict, void *__restrict);

PayloadArgs *payload_args;

static int old_stdout = -1;
static int log_sock = -1;

static int init_log(void) {
  int ret;

  log_sock = socket(AF_INET, SOCK_STREAM, 0);
  if (log_sock < 0)
    return log_sock;

  struct sockaddr_in sin;
  memset(&sin, 0, sizeof(sin));
  sin.sin_family = AF_INET;
  sin.sin_port = htons(LOG_PORT);
  ret = inet_pton(AF_INET, LOG_IP, &sin.sin_addr);
  if (ret < 0) {
    close(log_sock);
    return ret;
  }

  ret = connect(log_sock, (struct sockaddr *)&sin, sizeof(sin));
  if (ret < 0) {
    close(log_sock);
    return ret;
  }

  // Redirect stdout.
  old_stdout = dup(1);
  dup2(log_sock, 1);

  return 0;
}

static void shutdown_log(void) {
  dup2(old_stdout, 1);
  close(old_stdout);
  close(log_sock);
}

int payload(PayloadArgs *args) {
  int ret;

  payload_args = args;

  resolve_imports(args->dlsym);

  ret = init_log();
  if (ret < 0)
    return errno;

  printf("[+] Payload entered\n");

  shutdown_log();

  return 0;
}
