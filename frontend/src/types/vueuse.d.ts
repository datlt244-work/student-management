declare module '@vueuse/core' {
  // Minimal typing for watchDebounced to satisfy TypeScript; real types will override when package is installed.
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  export function watchDebounced<T = any>(
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    source: any,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    cb: (value: T) => any,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    options?: { debounce?: number; maxWait?: number; [key: string]: any },
  ): void
}


