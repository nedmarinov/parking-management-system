export async function getHealth(signal) {
  const response = await fetch("/api/health", { signal });

  if (!response.ok) {
    throw new Error("The parking service is unavailable.");
  }

  const health = await response.json();
  if (health.status !== "UP") {
    throw new Error("The parking service is not ready.");
  }

  return health;
}
