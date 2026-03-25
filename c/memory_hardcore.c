#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#include "memory_hardcore.h"

static uint8_t* gHeap = NULL;
static size_t gHeapSize = 0;
static size_t gOffset = 0;

// the return for this is a little confusing
// basically, it just rounds 'n' to the nearest
// multiple of 8
// it ensures every allocated block in the heap
// starts on an 8-byte aligned address
static size_t align8(size_t n) {
  return (n + 7u) & ~7u;
}

bool hardcore_allocator_init(size_t heapBytes) {
  if (gHeap != NULL || heapBytes == 0) return false;

  gHeap = (uint8_t*)malloc(heapBytes); // called malloc here
  if (gHeap == NULL) return false;

  gHeapSize = heapBytes;
  gOffset = 0;
  return true;
}

void* hardcore_reallocate(void* pointer, size_t oldSize, size_t newSize) {
  if (gHeap == NULL || gHeapSize == 0 || newSize == 0) return NULL;

  size_t wanted = align8(newSize);
  if (gOffset + wanted > gHeapSize) return NULL;

  void* out = gHeap + gOffset;

  gOffset += wanted;

  if (pointer != NULL) {
    size_t copyBytes = oldSize < newSize ? oldSize : newSize;
    memcpy(out, pointer, copyBytes);
  }

  return out;
}

void hardcore_allocator_shutdown(void) {
  // In strict hardcore mode, no free() calls are allowed.
  // We keep the global reference alive so leak checkers
  // treat it as reachable.
  // this function is defined for free() calls if
  // constraints are ever to be changed
}