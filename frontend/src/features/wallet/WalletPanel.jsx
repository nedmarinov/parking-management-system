import { useState } from "react";
import { Feedback, SectionState } from "@/components/Feedback";
import { fieldClass, labelClass } from "@/components/Panel";
import { Button } from "@/components/ui/button";
import { formatMoney, validateTopUpAmount } from "@/lib/format";

export function WalletPanel({ user, busy, onTopUp, feedback }) {
  const [amount, setAmount] = useState("");
  const [fieldError, setFieldError] = useState(null);
  const pending = busy === "topup";

  async function submit(event) {
    event.preventDefault();
    const error = validateTopUpAmount(amount);
    setFieldError(error);
    if (error) return;
    if (await onTopUp(amount.trim())) setAmount("");
  }

  return (
    <>
      <SectionState state={user} empty="" isEmpty={() => false} onRetry={user.retry}>
        <p className="text-sm text-[#183c35]/70">Balance</p>
        <p className="text-3xl font-semibold tracking-tight tabular-nums">{formatMoney(user.data?.balance)}</p>
      </SectionState>
      <form onSubmit={submit} noValidate className="mt-4">
        <label htmlFor="top-up-amount" className={labelClass}>
          Add funds (EUR)
        </label>
        <div className="flex gap-2">
          <input
            id="top-up-amount"
            type="text"
            inputMode="decimal"
            autoComplete="off"
            placeholder="20.00"
            className={fieldClass}
            value={amount}
            disabled={busy !== null || user.data === null}
            aria-invalid={fieldError ? true : undefined}
            aria-describedby={fieldError ? "top-up-error" : undefined}
            onChange={(event) => {
              setAmount(event.target.value);
              setFieldError(null);
            }}
          />
          <Button type="submit" size="lg" disabled={busy !== null || user.data === null}>
            {pending ? "Adding…" : "Add funds"}
          </Button>
        </div>
        {fieldError && (
          <p id="top-up-error" className="mt-1.5 text-sm text-red-800">
            {fieldError}
          </p>
        )}
      </form>
      <Feedback message={feedback} />
    </>
  );
}
