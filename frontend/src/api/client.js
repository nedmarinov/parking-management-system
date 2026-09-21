/** A failed API call. `kind` is "http", "network" (including timeouts), "aborted", or "invalid". */
export class ApiError extends Error {
  constructor(kind, message, { status = null, code = null } = {}) {
    super(message);
    this.name = "ApiError";
    this.kind = kind;
    this.status = status;
    this.code = code;
  }
}

const DEFAULT_TIMEOUT_MS = 15_000;

/** Calls `/api{path}` and returns parsed JSON. Never retries; callers decide what to do on failure. */
export async function request(path, { method = "GET", body, signal, timeoutMs = DEFAULT_TIMEOUT_MS } = {}) {
  const timeout = AbortSignal.timeout(timeoutMs);
  const combined = signal ? AbortSignal.any([signal, timeout]) : timeout;
  const headers = { Accept: "application/json" };
  if (body !== undefined) headers["Content-Type"] = "application/json";

  let response;
  let text;
  try {
    response = await fetch(`/api${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: combined,
    });
    text = await response.text();
  } catch {
    if (signal?.aborted) throw new ApiError("aborted", "The request was cancelled.");
    if (timeout.aborted) throw new ApiError("network", "The parking service did not respond in time.");
    throw new ApiError("network", "Could not reach the parking service.");
  }

  let json = null;
  try {
    json = text ? JSON.parse(text) : null;
  } catch {
    // A proxy or server error page; handled below.
  }

  if (!response.ok) {
    if (json && typeof json.code === "string" && typeof json.message === "string") {
      throw new ApiError("http", json.message, { status: response.status, code: json.code });
    }
    throw new ApiError("http", `The parking service returned an error (${response.status}).`, {
      status: response.status,
    });
  }
  if (json === null) throw new ApiError("invalid", "The parking service sent an unexpected response.");
  return json;
}

export function isAbort(error) {
  return error instanceof ApiError && error.kind === "aborted";
}
