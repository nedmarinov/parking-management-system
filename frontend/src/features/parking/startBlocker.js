/**
 * Explains why Start parking cannot be submitted, or returns null when it can.
 * Takes plain loaded data so the button state always follows the current server state.
 */
export function startBlocker({ vehicles, parkedIds, unpaidIds = new Set(), vehicleId, cityId, zones, zoneId }) {
  if (vehicles === null) return "Loading vehicles…";
  if (vehicles.length === 0) return "This user has no vehicles.";
  if (vehicleId === "") return "Choose a vehicle.";
  const vehicle = vehicles.find((v) => String(v.id) === String(vehicleId));
  if (!vehicle) return "Choose a vehicle.";
  if (parkedIds.has(vehicle.id)) return `${vehicle.plateNumber} is already parked. Stop it first, or choose another vehicle.`;
  if (unpaidIds.has(vehicle.id)) return `${vehicle.plateNumber} has unpaid parking. Pay it first, or choose another vehicle.`;
  if (cityId === null) return "Choose a city.";
  if (zones === null) return "Loading zones…";
  if (zones.length === 0) return "This city has no active zones.";
  if (!zones.some((zone) => String(zone.id) === String(zoneId))) return "Choose a zone.";
  return null;
}
