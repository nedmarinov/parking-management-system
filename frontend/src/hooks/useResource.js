import { useCallback, useEffect, useState } from "react";
import { isAbort } from "@/api/client";

const IDLE = { status: "idle", data: null, error: null };
const LOADING = { status: "loading", data: null, error: null };

/**
 * Loads `load(key, signal)` whenever `key` changes; a null key means nothing to load.
 * Changing the key clears old data immediately and aborts the obsolete request.
 * `load` must be a stable (module-level) function.
 */
export function useResource(load, key) {
  const [state, setState] = useState(key === null ? IDLE : LOADING);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    if (key === null) {
      setState(IDLE);
      return undefined;
    }
    const controller = new AbortController();
    setState(LOADING);
    load(key, controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) setState({ status: "ready", data, error: null });
      })
      .catch((error) => {
        if (!controller.signal.aborted && !isAbort(error)) setState({ status: "error", data: null, error });
      });
    return () => controller.abort();
  }, [load, key, attempt]);

  const retry = useCallback(() => setAttempt((value) => value + 1), []);
  return { ...state, retry };
}
