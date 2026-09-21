import { request } from "./client";

export const listActiveParking = (userId, signal) => request(`/users/${userId}/parkings/active`, { signal });
export const listParkingHistory = (userId, signal) => request(`/users/${userId}/parkings/history`, { signal });
export const startParking = (userId, vehicleId, zoneId) =>
  request("/parkings", { method: "POST", body: { userId, vehicleId, zoneId } });
export const stopParking = (parkingId, userId) =>
  request(`/parkings/${parkingId}/stop`, { method: "POST", body: { userId } });
export const payParking = (parkingId, userId) =>
  request(`/parkings/${parkingId}/payment`, { method: "POST", body: { userId } });
