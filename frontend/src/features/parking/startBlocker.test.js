import { describe, expect, it } from "vitest";
import { startBlocker } from "./startBlocker";

const vehicles = [
  { id: 1, plateNumber: "CA1234AB" },
  { id: 2, plateNumber: "CB5678CD" },
];
const zones = [{ id: 1, name: "Blue Zone" }];
const ready = { vehicles, parkedIds: new Set(), vehicleId: "1", cityId: 1, zones, zoneId: "1" };

describe("startBlocker", () => {
  it("allows starting when everything is chosen and the vehicle is free", () => {
    expect(startBlocker(ready)).toBeNull();
  });

  it("blocks a vehicle that is parked and unblocks it once the active list no longer contains it", () => {
    expect(startBlocker({ ...ready, parkedIds: new Set([1]) })).toBe(
      "CA1234AB is already parked. Stop it first, or choose another vehicle.",
    );
    expect(startBlocker({ ...ready, parkedIds: new Set() })).toBeNull();
  });

  it("blocks a vehicle with unpaid parking until it is paid, without affecting other vehicles", () => {
    expect(startBlocker({ ...ready, unpaidIds: new Set([1]) })).toBe(
      "CA1234AB has unpaid parking. Pay it first, or choose another vehicle.",
    );
    expect(startBlocker({ ...ready, unpaidIds: new Set([1]), vehicleId: "2" })).toBeNull();
    expect(startBlocker({ ...ready, unpaidIds: new Set() })).toBeNull();
  });

  it.each([
    [{ vehicles: null }, "Loading vehicles…"],
    [{ vehicles: [] }, "This user has no vehicles."],
    [{ vehicleId: "" }, "Choose a vehicle."],
    [{ vehicleId: "99" }, "Choose a vehicle."],
    [{ cityId: null }, "Choose a city."],
    [{ zones: null }, "Loading zones…"],
    [{ zones: [] }, "This city has no active zones."],
    [{ zoneId: "" }, "Choose a zone."],
    [{ zoneId: "7" }, "Choose a zone."],
  ])("explains %o", (change, reason) => {
    expect(startBlocker({ ...ready, ...change })).toBe(reason);
  });
});
