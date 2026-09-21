import { request } from "./client";

export const listCities = (signal) => request("/cities", { signal });
export const listZones = (cityId, signal) => request(`/cities/${cityId}/zones`, { signal });
