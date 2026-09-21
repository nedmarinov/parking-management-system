const AMOUNT_FORMAT = /^(0|[1-9][0-9]{0,9})(\.[0-9]{1,2})?$/;

/** Formats a server decimal string such as "1234.50" as "€1,234.50" without float arithmetic. */
export function formatMoney(amount) {
  if (amount === null || amount === undefined) return "—";
  const [whole, fraction = "00"] = String(amount).split(".");
  return `€${whole.replace(/\B(?=(\d{3})+(?!\d))/g, ",")}.${fraction.padEnd(2, "0")}`;
}

/** Formats a UTC API timestamp in the browser's timezone, keeping the date. */
export function formatDateTime(timestamp) {
  if (!timestamp) return "—";
  return new Date(timestamp).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" });
}

/** Returns a message describing why a top-up amount is invalid, or null if the API will accept it. */
export function validateTopUpAmount(input) {
  const value = input.trim();
  if (value === "") return "Enter an amount.";
  if (value.startsWith("-")) return "The amount must be positive.";
  if (!AMOUNT_FORMAT.test(value)) {
    if (/^\d+\.\d{3,}$/.test(value)) return "Use at most two decimal places.";
    if (/^\d{11,}(\.\d*)?$/.test(value)) return "The amount is too large.";
    return "Enter an amount like 20 or 20.50, without currency signs or separators.";
  }
  if (/^0(\.0{1,2})?$/.test(value)) return "The amount must be at least 0.01.";
  return null;
}
