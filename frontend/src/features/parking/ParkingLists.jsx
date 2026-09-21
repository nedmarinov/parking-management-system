import { Feedback, SectionState } from "@/components/Feedback";
import { Button } from "@/components/ui/button";
import { formatDateTime, formatMoney } from "@/lib/format";

function Place({ session }) {
  return (
    <>
      {session.zone.name}, {session.zone.city.name}
    </>
  );
}

function StatusBadge({ session }) {
  const paid = session.paymentStatus === "PAID";
  return (
    <span
      className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${paid ? "bg-emerald-100 text-emerald-900" : "bg-amber-100 text-amber-950"}`}
    >
      {paid ? "Paid" : "Unpaid"}
    </span>
  );
}

export function ActiveParkingList({ active, busy, onStop, feedback }) {
  return (
    <>
      <SectionState state={active} empty="No active parking." isEmpty={(list) => list.length === 0} onRetry={active.retry}>
        <ul className="grid gap-3">
          {active.data?.map((session) => (
            <li key={session.id} className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-[#183c35]/10 p-3">
              <div className="text-sm">
                <p className="font-semibold">
                  {session.vehicle.plateNumber}{" "}
                  <span className="ml-1 rounded-full bg-sky-100 px-2 py-0.5 text-xs font-medium text-sky-900">Active</span>
                </p>
                <p className="text-[#183c35]/75">
                  <Place session={session} /> · {formatMoney(session.hourlyRate)}/hour
                </p>
                <p className="text-[#183c35]/75">Started {formatDateTime(session.startedAt)}</p>
              </div>
              <Button
                variant="outline"
                disabled={busy !== null}
                aria-label={`Stop parking for ${session.vehicle.plateNumber}`}
                onClick={() => onStop(session)}
              >
                {busy === `stop:${session.id}` ? "Stopping…" : "Stop parking"}
              </Button>
            </li>
          ))}
        </ul>
      </SectionState>
      <Feedback message={feedback} />
    </>
  );
}

export function UnpaidParkingList({ history, busy, onPay, feedback }) {
  const unpaid = history.data === null ? null : history.data.filter((s) => s.paymentStatus === "UNPAID");
  return (
    <>
      <SectionState state={{ ...history, data: unpaid }} empty="No unpaid parking." isEmpty={(list) => list.length === 0} onRetry={history.retry}>
        <ul className="grid gap-3">
          {unpaid?.map((session) => (
            <li key={session.id} className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-[#183c35]/10 p-3">
              <div className="text-sm">
                <p className="font-semibold">
                  {session.vehicle.plateNumber} · {formatMoney(session.amount)} <StatusBadge session={session} />
                </p>
                <p className="text-[#183c35]/75">
                  <Place session={session} /> · ended {formatDateTime(session.endedAt)}
                </p>
              </div>
              <Button
                disabled={busy !== null}
                aria-label={`Pay ${formatMoney(session.amount)} for ${session.vehicle.plateNumber}`}
                onClick={() => onPay(session)}
              >
                {busy === `pay:${session.id}` ? "Paying…" : "Pay"}
              </Button>
            </li>
          ))}
        </ul>
      </SectionState>
      <Feedback message={feedback} />
    </>
  );
}

export function ParkingHistory({ history }) {
  return (
    <SectionState state={history} empty="No completed parking yet." isEmpty={(list) => list.length === 0} onRetry={history.retry}>
      <div className="overflow-x-auto">
        <table className="w-full min-w-[720px] text-left text-sm">
          <thead className="text-xs text-[#183c35]/65">
            <tr>
              {["Vehicle", "Place", "Started", "Ended", "Rate", "Amount", "Status", "Paid at"].map((heading) => (
                <th key={heading} scope="col" className="py-2 pr-4 font-medium">
                  {heading}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {history.data?.map((session) => (
              <tr key={session.id} className="border-t border-[#183c35]/10">
                <td className="py-2 pr-4 font-medium">{session.vehicle.plateNumber}</td>
                <td className="py-2 pr-4">
                  <Place session={session} />
                </td>
                <td className="py-2 pr-4">{formatDateTime(session.startedAt)}</td>
                <td className="py-2 pr-4">{formatDateTime(session.endedAt)}</td>
                <td className="py-2 pr-4 tabular-nums">{formatMoney(session.hourlyRate)}/h</td>
                <td className="py-2 pr-4 tabular-nums">{formatMoney(session.amount)}</td>
                <td className="py-2 pr-4">
                  <StatusBadge session={session} />
                </td>
                <td className="py-2 pr-4">{formatDateTime(session.paidAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </SectionState>
  );
}
