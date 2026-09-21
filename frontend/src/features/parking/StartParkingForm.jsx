import { useState } from "react";
import { listZones } from "@/api/catalog";
import { Feedback, SectionState } from "@/components/Feedback";
import { fieldClass, labelClass } from "@/components/Panel";
import { Button } from "@/components/ui/button";
import { useResource } from "@/hooks/useResource";
import { formatMoney } from "@/lib/format";

export function StartParkingForm({ userId, vehicles, active, cities, busy, onStart, feedback }) {
  // The vehicle choice belongs to one user; city and zone are shared catalog context and survive user changes.
  const [vehicleChoice, setVehicleChoice] = useState({ userId, vehicleId: "" });
  const [cityId, setCityId] = useState(null);
  const [zoneId, setZoneId] = useState("");
  const zones = useResource(listZones, cityId);

  const vehicleId = vehicleChoice.userId === userId ? vehicleChoice.vehicleId : "";
  const parkedIds = new Set((active.data ?? []).map((session) => session.vehicle.id));
  const selectedZone = zones.data?.find((zone) => String(zone.id) === zoneId) ?? null;
  const locked = busy !== null;
  const canStart = !locked && vehicleId !== "" && !parkedIds.has(Number(vehicleId)) && selectedZone !== null;

  async function submit(event) {
    event.preventDefault();
    if (!canStart) return;
    if (await onStart(Number(vehicleId), selectedZone.id)) setVehicleChoice({ userId, vehicleId: "" });
  }

  return (
    <form onSubmit={submit} className="grid gap-4">
      <div>
        <label htmlFor="vehicle" className={labelClass}>
          Vehicle
        </label>
        <SectionState state={vehicles} empty="This user has no vehicles." isEmpty={(list) => list.length === 0} onRetry={vehicles.retry}>
          <select
            id="vehicle"
            className={fieldClass}
            value={vehicleId}
            disabled={locked}
            aria-describedby="vehicle-hint"
            onChange={(event) => setVehicleChoice({ userId, vehicleId: event.target.value })}
          >
            <option value="">Choose a vehicle</option>
            {vehicles.data?.map((vehicle) => (
              <option key={vehicle.id} value={vehicle.id} disabled={parkedIds.has(vehicle.id)}>
                {vehicle.plateNumber}
                {parkedIds.has(vehicle.id) ? " — already parked" : ""}
              </option>
            ))}
          </select>
          <p id="vehicle-hint" className="mt-1.5 text-xs text-[#183c35]/65">
            A vehicle that is already parked must be stopped before it can park again.
          </p>
        </SectionState>
      </div>

      <div>
        <label htmlFor="city" className={labelClass}>
          City
        </label>
        <SectionState state={cities} empty="No cities are available." isEmpty={(list) => list.length === 0} onRetry={cities.retry}>
          <select
            id="city"
            className={fieldClass}
            value={cityId ?? ""}
            disabled={locked}
            onChange={(event) => {
              setCityId(event.target.value === "" ? null : Number(event.target.value));
              setZoneId("");
            }}
          >
            <option value="">Choose a city</option>
            {cities.data?.map((city) => (
              <option key={city.id} value={city.id}>
                {city.name}
              </option>
            ))}
          </select>
        </SectionState>
      </div>

      <div>
        <label htmlFor="zone" className={labelClass}>
          Zone
        </label>
        {cityId === null ? (
          <select id="zone" className={fieldClass} disabled value="">
            <option value="">Choose a city first</option>
          </select>
        ) : (
          <SectionState state={zones} empty="This city has no active zones." isEmpty={(list) => list.length === 0} onRetry={zones.retry}>
            <select id="zone" className={fieldClass} value={zoneId} disabled={locked} onChange={(event) => setZoneId(event.target.value)}>
              <option value="">Choose a zone</option>
              {zones.data?.map((zone) => (
                <option key={zone.id} value={zone.id}>
                  {zone.name} — {formatMoney(zone.pricePerHour)}/hour
                </option>
              ))}
            </select>
          </SectionState>
        )}
        <p className="mt-1.5 text-xs text-[#183c35]/65">
          {selectedZone ? `${formatMoney(selectedZone.pricePerHour)} per hour. ` : ""}
          Every started hour is charged, with a one-hour minimum. The rate is fixed when parking starts.
        </p>
      </div>

      <div>
        <Button type="submit" size="lg" disabled={!canStart}>
          {busy === "start" ? "Starting…" : "Start parking"}
        </Button>
        <Feedback message={feedback} />
      </div>
    </form>
  );
}
