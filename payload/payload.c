#include <stddef.h>
#include <stdint.h>
#include <sys/types.h>

int payload(int (* sceKernelDlsym)(int handle, const char *symbol, uintptr_t *address)) {
  return 1337;
}
