import { useCallback, useEffect, useRef, useState } from "react";
import { isAbort } from "@/api/client";
import { listActiveParking, listParkingHistory } from "@/api/parking";
import { getUser, listVehicles } from "@/api/users";
import { createRequestTracker } from "@/lib/requests";

const LOADERS = {
  user: getUser,
  vehicles: listVehicles,
  active: listActiveParking,
  history: listParkingHistory,
};
export const ALL_SECTIONS = Object.keys(LOADERS);

const sectionsWith = (value) => Object.fromEntries(ALL_SECTIONS.map((section) => [section, value]));
const LOADING = { status: "loading", data: null, error: null };
const IDLE = { status: "idle", data: null, error: null };

/**
 * Owns the selected user's details, vehicles, active parking, and history.
 * Switching users clears everything and aborts the old requests; a response is applied only if it is
 * still the latest request for its section, so another user's data can never appear.
 * `reload(sections)` refreshes sections in place and resolves to the list of sections that failed.
 */
export function useUserData(userId) {
  const [data, setData] = useState(() => sectionsWith(userId === null ? IDLE : LOADING));
  const trackerRef = useRef(null);
  if (trackerRef.current === null) trackerRef.current = createRequestTracker();

  const reload = useCallback(
    async (sections) => {
      const tracker = trackerRef.current;
      const results = await Promise.all(
        sections.map(async (section) => {
          const request = tracker.begin(section);
          setData((current) => ({ ...current, [section]: { ...current[section], status: "loading", error: null } }));
          try {
            const value = await LOADERS[section](userId, request.signal);
            if (request.isCurrent()) {
              setData((current) => ({ ...current, [section]: { status: "ready", data: value, error: null } }));
            }
            return null;
          } catch (error) {
            if (!request.isCurrent() || isAbort(error)) return null;
            // Keep previously loaded data visible; the error explains it may be out of date.
            setData((current) => ({ ...current, [section]: { ...current[section], status: "error", error } }));
            return section;
          }
        }),
      );
      return results.filter(Boolean);
    },
    [userId],
  );

  useEffect(() => {
    const tracker = trackerRef.current;
    if (userId === null) {
      setData(sectionsWith(IDLE));
      return undefined;
    }
    setData(sectionsWith(LOADING));
    reload(ALL_SECTIONS);
    return () => tracker.abortAll();
  }, [userId, reload]);

  return { ...data, reload };
}
