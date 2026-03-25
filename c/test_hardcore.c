#include <assert.h>
#include <stdio.h>
#include "memory_hardcore.h"

int main(void) {
  assert(hardcore_allocator_init(256));
  printf("init ok (256 bytes)\n");

  int* a = (int*)hardcore_reallocate(NULL, 0, sizeof(int) * 4);
  assert(a != NULL);
  for (int i = 0; i < 4; i++) a[i] = i + 1;
  printf("alloc 4 ints at %p\n", (void*)a);

  int* b = (int*)hardcore_reallocate(a, sizeof(int) * 4, sizeof(int) * 8);
  assert(b != NULL);
  assert(b[0] == 1 && b[1] == 2 && b[2] == 3 && b[3] == 4);
  printf("grow to 8 ints at %p (copied old values)\n", (void*)b);

  void* freed = hardcore_reallocate(b, sizeof(int) * 8, 0);
  assert(freed == NULL);
  printf("free request returned NULL (no reclaim in bump allocator)\n");

  void* tooBig = hardcore_reallocate(NULL, 0, 10000);
  assert(tooBig == NULL);
  printf("oversized alloc returned NULL\n");

  hardcore_allocator_shutdown();
  printf("hardcore bump allocator demo passed\n");
  return 0;
}