import { describe, expect, it } from "vitest";
import { createRequestTracker } from "./requests";

describe("createRequestTracker", () => {
  it("aborts and invalidates the previous request for the same key", () => {
    const tracker = createRequestTracker();
    const first = tracker.begin("history");
    const second = tracker.begin("history");

    expect(first.signal.aborted).toBe(true);
    expect(first.isCurrent()).toBe(false);
    expect(second.isCurrent()).toBe(true);
  });

  it("keeps different keys independent", () => {
    const tracker = createRequestTracker();
    const user = tracker.begin("user");
    tracker.begin("history");

    expect(user.isCurrent()).toBe(true);
    expect(user.signal.aborted).toBe(false);
  });

  it("abortAll invalidates everything, so a late response for the previous user is dropped", () => {
    const tracker = createRequestTracker();
    const previousUser = tracker.begin("user");
    tracker.abortAll();
    const nextUser = tracker.begin("user");

    expect(previousUser.signal.aborted).toBe(true);
    expect(previousUser.isCurrent()).toBe(false);
    expect(nextUser.isCurrent()).toBe(true);
  });
});
