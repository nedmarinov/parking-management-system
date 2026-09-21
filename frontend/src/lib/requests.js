/**
 * Tracks the latest request per key. Starting a new request for a key aborts the previous one,
 * and `isCurrent()` tells a response handler whether it may still apply its result.
 */
export function createRequestTracker() {
  const controllers = new Map();
  return {
    begin(key) {
      controllers.get(key)?.abort();
      const controller = new AbortController();
      controllers.set(key, controller);
      return { signal: controller.signal, isCurrent: () => controllers.get(key) === controller };
    },
    abortAll() {
      for (const controller of controllers.values()) controller.abort();
      controllers.clear();
    },
  };
}
