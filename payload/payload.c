#include <stddef.h>
#include <stdint.h>
#include <sys/types.h>

typedef int32_t SceKernelModule;

int payload(int (* sceKernelDlsym)(SceKernelModule handle, const char *symbol, void **addrp)) {
  return 1337;
}
