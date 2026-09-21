import { describe, expect, it } from "vitest";
import { formatDateTime, formatMoney, validateTopUpAmount } from "./format";

describe("formatMoney", () => {
  it.each([
    ["20.00", "€20.00"],
    ["0.00", "€0.00"],
    ["1234.50", "€1,234.50"],
    ["9999999999.99", "€9,999,999,999.99"],
    ["20", "€20.00"],
    ["20.5", "€20.50"],
  ])("formats %s as %s", (input, expected) => expect(formatMoney(input)).toBe(expected));

  it("shows a dash for missing values", () => expect(formatMoney(null)).toBe("—"));
});

describe("formatDateTime", () => {
  it("shows a dash for missing values", () => expect(formatDateTime(null)).toBe("—"));
  it("keeps the date", () => expect(formatDateTime("2026-09-21T10:15:00.000Z")).toMatch(/2026/));
});

describe("validateTopUpAmount", () => {
  it.each(["20", "20.0", "20.00", "5.50", "0.01", " 20 ", "9999999999.99"])("accepts %j", (input) =>
    expect(validateTopUpAmount(input)).toBeNull(),
  );

  it.each([
    ["", "Enter an amount."],
    ["   ", "Enter an amount."],
    ["-5", "The amount must be positive."],
    ["0", "The amount must be at least 0.01."],
    ["0.00", "The amount must be at least 0.01."],
    ["10.123", "Use at most two decimal places."],
    ["12345678901", "The amount is too large."],
  ])("rejects %j with a specific message", (input, message) => expect(validateTopUpAmount(input)).toBe(message));

  it.each(["abc", "1e2", "01.00", "1,00", "€5", "+5", "5.", ".5"])("rejects malformed %j", (input) =>
    expect(validateTopUpAmount(input)).toMatch(/^Enter an amount like/),
  );
});
