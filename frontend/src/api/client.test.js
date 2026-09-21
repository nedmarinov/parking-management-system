import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError, request } from "./client";

function respondWith(status, body, contentType = "application/json") {
  vi.stubGlobal("fetch", vi.fn(async () => new Response(body, { status, headers: { "Content-Type": contentType } })));
}

afterEach(() => vi.unstubAllGlobals());

describe("request", () => {
  it("sends JSON to the relative /api URL and parses the response", async () => {
    respondWith(201, '{"id":1}');
    await expect(request("/parkings", { method: "POST", body: { userId: 1 } })).resolves.toEqual({ id: 1 });

    const [url, init] = fetch.mock.calls[0];
    expect(url).toBe("/api/parkings");
    expect(init.method).toBe("POST");
    expect(init.body).toBe('{"userId":1}');
    expect(init.headers["Content-Type"]).toBe("application/json");
  });

  it("uses the API error message and code", async () => {
    respondWith(409, '{"code":"INSUFFICIENT_BALANCE","message":"Insufficient account balance"}');
    await expect(request("/x")).rejects.toMatchObject({
      kind: "http",
      status: 409,
      code: "INSUFFICIENT_BALANCE",
      message: "Insufficient account balance",
    });
  });

  it("handles a non-JSON proxy error", async () => {
    respondWith(502, "<html>Bad Gateway</html>", "text/html");
    await expect(request("/x")).rejects.toMatchObject({ kind: "http", status: 502, code: null });
  });

  it("reports network failures separately", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => Promise.reject(new TypeError("Failed to fetch"))));
    await expect(request("/x")).rejects.toMatchObject({ kind: "network" });
  });

  it("reports caller aborts as aborted, not as failures", async () => {
    vi.stubGlobal("fetch", vi.fn((_url, init) => new Promise((_resolve, reject) => {
      init.signal.addEventListener("abort", () => reject(new DOMException("Aborted", "AbortError")));
    })));
    const controller = new AbortController();
    const pending = request("/x", { signal: controller.signal });
    controller.abort();
    await expect(pending).rejects.toMatchObject({ kind: "aborted" });
  });

  it("treats a timeout as a network failure whose outcome is unknown", async () => {
    vi.stubGlobal("fetch", vi.fn((_url, init) => new Promise((_resolve, reject) => {
      init.signal.addEventListener("abort", () => reject(new DOMException("Timed out", "TimeoutError")));
    })));
    await expect(request("/x", { timeoutMs: 10 })).rejects.toMatchObject({ kind: "network" });
  });

  it("rejects an empty success body", async () => {
    respondWith(200, "");
    await expect(request("/x")).rejects.toBeInstanceOf(ApiError);
  });
});
