import { useEffect, useRef, useState } from "react";
import { CircleParking } from "lucide-react";
import { listCities } from "@/api/catalog";
import { isAbort } from "@/api/client";
import { payParking, startParking, stopParking } from "@/api/parking";
import { listUsers, topUp } from "@/api/users";
import { Panel } from "@/components/Panel";
import { ActiveParkingList, ParkingHistory, UnpaidParkingList } from "@/features/parking/ParkingLists";
import { StartParkingForm } from "@/features/parking/StartParkingForm";
import { UserSelector } from "@/features/users/UserSelector";
import { WalletPanel } from "@/features/wallet/WalletPanel";
import { useResource } from "@/hooks/useResource";
import { useUserData } from "@/hooks/useUserData";
import { formatMoney } from "@/lib/format";

const loadUsers = (_key, signal) => listUsers(signal);
const loadCities = (_key, signal) => listCities(signal);

export default function App() {
  const users = useResource(loadUsers, "all");
  const cities = useResource(loadCities, "all");
  const [chosenUserId, setChosenUserId] = useState(null);
  const selectedUserId = users.data?.some((user) => user.id === chosenUserId)
    ? chosenUserId
    : (users.data?.[0]?.id ?? null);

  const userData = useUserData(selectedUserId);
  const section = (name) => ({ ...userData[name], retry: () => userData.reload([name]) });

  // One mutation at a time; its key ("topup", "start", "stop:12", "pay:12") drives the busy labels.
  const [busy, setBusy] = useState(null);
  // Feedback belongs to the user it was produced for, so switching users clears it.
  const [feedback, setFeedback] = useState({ userId: null, messages: {} });
  const messages = feedback.userId === selectedUserId ? feedback.messages : {};
  const userRef = useRef(selectedUserId);
  useEffect(() => {
    userRef.current = selectedUserId;
  }, [selectedUserId]);

  function setMessage(userId, name, message) {
    setFeedback((current) => ({
      userId,
      messages: { ...(current.userId === userId ? current.messages : {}), [name]: message },
    }));
  }

  /**
   * Runs one mutation, then refreshes the affected sections. A rejected request, a request whose
   * outcome is unknown, and a success followed by a failed refresh are reported differently.
   * Never resubmits automatically. Resolves to true when the mutation succeeded.
   */
  async function runMutation({ key, feedbackFor, action, refresh, success }) {
    const userId = selectedUserId;
    const { reload } = userData;
    setBusy(key);
    setMessage(userId, feedbackFor, null);
    try {
      let result;
      try {
        result = await action(userId);
      } catch (error) {
        if (userRef.current !== userId || isAbort(error)) return false;
        await reload(refresh);
        if (error.kind === "http") {
          const hint = error.code === "INSUFFICIENT_BALANCE" ? " Add funds, then pay again." : "";
          setMessage(userId, feedbackFor, { tone: "error", text: `${error.message}${hint}` });
        } else {
          setMessage(userId, feedbackFor, {
            tone: "warning",
            text: `${error.message} We could not confirm whether this worked. The page has been refreshed — review it before trying again.`,
          });
        }
        return false;
      }
      if (userRef.current !== userId) return true;
      const text = success(result);
      const failed = await reload(refresh);
      if (failed.length === 0) {
        setMessage(userId, feedbackFor, { tone: "success", text });
      } else {
        const retry = async () => {
          if ((await reload(failed)).length === 0) setMessage(userId, feedbackFor, { tone: "success", text });
        };
        setMessage(userId, feedbackFor, { tone: "warning", text: `${text} Some information could not be refreshed.`, retry });
      }
      return true;
    } finally {
      setBusy(null);
    }
  }

  const handleTopUp = (amount) =>
    runMutation({
      key: "topup",
      feedbackFor: "wallet",
      action: (userId) => topUp(userId, amount),
      refresh: ["user"],
      success: (result) => `Added ${formatMoney(amount)}. Balance is now ${formatMoney(result.balance)}.`,
    });

  const handleStart = (vehicleId, zoneId) =>
    runMutation({
      key: "start",
      feedbackFor: "start",
      action: (userId) => startParking(userId, vehicleId, zoneId),
      refresh: ["active"],
      success: (s) => `Parking started for ${s.vehicle.plateNumber} in ${s.zone.name}, ${s.zone.city.name}.`,
    });

  const handleStop = (session) =>
    runMutation({
      key: `stop:${session.id}`,
      feedbackFor: "active",
      action: (userId) => stopParking(session.id, userId),
      refresh: ["active", "history"],
      success: (s) => `Stopped ${s.vehicle.plateNumber}. Amount due: ${formatMoney(s.amount)} — see Unpaid parking.`,
    });

  const handlePay = (session) =>
    runMutation({
      key: `pay:${session.id}`,
      feedbackFor: "unpaid",
      action: (userId) => payParking(session.id, userId),
      refresh: ["user", "history"],
      success: (p) => `Paid ${formatMoney(p.amount)} for ${session.vehicle.plateNumber}. Balance is now ${formatMoney(p.remainingBalance)}.`,
    });

  const hasUser = selectedUserId !== null;

  return (
    <div className="min-h-svh bg-[#f5f6f2] text-[#183c35]">
      <header className="border-b border-[#183c35]/10">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-4 sm:px-8">
          <div className="flex items-center gap-3 text-sm font-semibold">
            <CircleParking aria-hidden="true" className="size-7" strokeWidth={1.5} />
            <h1>Parking Management</h1>
          </div>
          <span className="rounded-full border border-[#183c35]/15 px-3 py-1 text-xs font-medium">Demo · fictional EUR</span>
        </div>
      </header>

      <main className="mx-auto grid max-w-6xl gap-5 px-4 py-6 sm:px-8 lg:grid-cols-2">
        <div className="grid content-start gap-5">
          <Panel id="account-heading" title="Account">
            <UserSelector users={users} selectedUserId={selectedUserId} onChange={setChosenUserId} disabled={busy !== null} />
          </Panel>
          {hasUser && (
            <>
              <Panel id="wallet-heading" title="Wallet">
                <WalletPanel user={section("user")} busy={busy} onTopUp={handleTopUp} feedback={messages.wallet} />
              </Panel>
              <Panel id="start-heading" title="Start parking">
                <StartParkingForm
                  userId={selectedUserId}
                  vehicles={section("vehicles")}
                  active={section("active")}
                  cities={cities}
                  busy={busy}
                  onStart={handleStart}
                  feedback={messages.start}
                />
              </Panel>
            </>
          )}
        </div>

        {hasUser && (
          <div className="grid content-start gap-5">
            <Panel id="active-heading" title="Active parking">
              <ActiveParkingList active={section("active")} busy={busy} onStop={handleStop} feedback={messages.active} />
            </Panel>
            <Panel id="unpaid-heading" title="Unpaid parking" description="Stopped parking waiting for payment from your balance.">
              <UnpaidParkingList history={section("history")} busy={busy} onPay={handlePay} feedback={messages.unpaid} />
            </Panel>
          </div>
        )}

        {hasUser && (
          <Panel id="history-heading" title="Parking history" description="Completed parking, paid and unpaid." className="lg:col-span-2">
            <ParkingHistory history={section("history")} />
          </Panel>
        )}
      </main>

      <footer className="mx-auto max-w-6xl px-4 pb-8 text-xs text-[#183c35]/65 sm:px-8">
        Times are shown in your local timezone. Parking is charged per started hour with a one-hour minimum. Balances
        use fictional money.
      </footer>
    </div>
  );
}
