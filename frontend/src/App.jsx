import { useEffect, useState } from "react";
import { ArrowUpRight, CarFront, CircleParking, MapPin, Wallet } from "lucide-react";
import { getHealth } from "@/api/health";
import { Button } from "@/components/ui/button";

const steps = [
  { icon: MapPin, title: "Choose your spot", text: "Find a parking zone in your city." },
  { icon: CarFront, title: "Park on your terms", text: "Start a session and stop when you're ready." },
  { icon: Wallet, title: "Pay with ease", text: "Settle your parking from your account balance." },
];

export default function App() {
  const [connection, setConnection] = useState("loading");
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let ignore = false;
    setConnection("loading");

    const timeout = setTimeout(() => controller.abort(), 10_000);
    getHealth(controller.signal)
      .then(() => {
        if (!ignore) setConnection("ready");
      })
      .catch(() => {
        if (!ignore) setConnection("error");
      })
      .finally(() => clearTimeout(timeout));

    return () => {
      ignore = true;
      clearTimeout(timeout);
      controller.abort();
    };
  }, [attempt]);

  return (
    <div className="min-h-svh bg-[#f5f6f2] text-[#183c35]">
      <header className="border-b border-[#183c35]/10">
        <div className="mx-auto flex max-w-5xl items-center justify-between gap-4 px-6 py-6 sm:px-10">
          <div className="flex items-center gap-3 text-sm font-semibold">
            <CircleParking aria-hidden="true" className="size-8" strokeWidth={1.5} />
            <span>Parking Management</span>
          </div>
          <span className="rounded-full border border-[#183c35]/15 px-3 py-1 text-xs font-medium">
            Demo
          </span>
        </div>
      </header>

      <main className="mx-auto max-w-5xl px-6 py-16 sm:px-10 sm:py-24">
        <p className="mb-5 text-xs font-semibold tracking-[0.18em] uppercase">A little less city friction</p>
        <h1 className="max-w-2xl text-5xl leading-[1.05] font-medium tracking-tight sm:text-7xl">
          Park simply.<br />Get on with your day.
        </h1>
        <p className="mt-6 max-w-lg text-base leading-7 text-[#183c35]/75 sm:text-lg">
          Your vehicles, parking sessions, and payments.<br className="hidden sm:block" /> All in one place.
        </p>

        <section aria-labelledby="coming-soon" className="mt-12 rounded-2xl border border-[#183c35]/10 bg-white p-6 sm:p-8">
          <div className="flex items-start justify-between gap-4">
            <div>
              <h2 id="coming-soon" className="text-lg font-semibold">Your parking dashboard is coming soon</h2>
              <p className="mt-2 max-w-xl text-sm leading-6 text-[#183c35]/70">
                We're preparing city selection, parking sessions, and account balances. Here's what you'll be able to do.
              </p>
            </div>
            <ArrowUpRight aria-hidden="true" className="hidden size-6 shrink-0 sm:block" />
          </div>
          <div className="mt-8 grid gap-7 sm:grid-cols-3">
            {steps.map(({ icon: Icon, title, text }) => (
              <div key={title}>
                <Icon aria-hidden="true" className="mb-4 size-5" strokeWidth={1.5} />
                <h3 className="text-sm font-semibold">{title}</h3>
                <p className="mt-2 text-sm leading-6 text-[#183c35]/70">{text}</p>
              </div>
            ))}
          </div>
        </section>

        <div className="mt-6 flex flex-wrap items-center gap-3 text-xs text-[#183c35]/75">
          <p role="status" aria-live="polite" className="flex items-center gap-2">
            <span aria-hidden="true" className={`size-2 rounded-full ${connection === "ready" ? "bg-emerald-600" : connection === "error" ? "bg-amber-600" : "bg-slate-400"}`} />
            {connection === "ready" && "Parking service connected"}
            {connection === "loading" && "Connecting to parking service…"}
            {connection === "error" && "Unable to reach the parking service"}
          </p>
          {connection === "error" && (
            <Button variant="outline" size="sm" onClick={() => setAttempt((value) => value + 1)}>
              Try again
            </Button>
          )}
        </div>
      </main>

      <footer className="mx-auto max-w-5xl px-6 pb-8 text-xs text-[#183c35]/65 sm:px-10">
        A parking prototype for Sofia and Plovdiv. Account balances use fictional money.
      </footer>
    </div>
  );
}
