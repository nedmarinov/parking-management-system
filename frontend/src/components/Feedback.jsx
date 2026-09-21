import { Button } from "@/components/ui/button";

const TONES = {
  success: "border-emerald-700/20 bg-emerald-50 text-emerald-900",
  error: "border-red-700/20 bg-red-50 text-red-900",
  warning: "border-amber-700/25 bg-amber-50 text-amber-950",
};

/** Live-region message for a section's action result; `retry` offers a read-only retry. */
export function Feedback({ message }) {
  return (
    <div role="status" aria-live="polite" className="empty:hidden">
      {message && (
        <div className={`mt-3 flex flex-wrap items-center gap-2 rounded-lg border px-3 py-2 text-sm ${TONES[message.tone]}`}>
          <span>{message.text}</span>
          {message.retry && (
            <Button variant="outline" size="xs" onClick={message.retry}>
              Retry refresh
            </Button>
          )}
        </div>
      )}
    </div>
  );
}

/** Loading, error, and empty states shared by the data sections. */
export function SectionState({ state, empty, isEmpty, children, onRetry }) {
  if (state.status === "loading" && state.data === null) {
    return <p className="text-sm text-muted-foreground">Loading…</p>;
  }
  return (
    <>
      {state.status === "error" && (
        <div role="alert" className="mb-3 flex flex-wrap items-center gap-2 text-sm text-red-800">
          <span>
            {state.data === null ? "Could not load this section." : "Could not refresh; showing earlier data."}{" "}
            {state.error?.message}
          </span>
          {onRetry && (
            <Button variant="outline" size="xs" onClick={onRetry}>
              Try again
            </Button>
          )}
        </div>
      )}
      {state.data !== null && (isEmpty(state.data) ? <p className="text-sm text-muted-foreground">{empty}</p> : children)}
    </>
  );
}
