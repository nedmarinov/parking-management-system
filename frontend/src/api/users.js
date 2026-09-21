import { request } from "./client";

export const listUsers = (signal) => request("/users", { signal });
export const getUser = (userId, signal) => request(`/users/${userId}`, { signal });
export const listVehicles = (userId, signal) => request(`/users/${userId}/vehicles`, { signal });
export const topUp = (userId, amount) => request(`/users/${userId}/top-up`, { method: "POST", body: { amount } });
