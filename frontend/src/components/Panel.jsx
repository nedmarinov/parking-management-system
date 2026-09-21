export function Panel({ title, id, description, children, className = "" }) {
  return (
    <section aria-labelledby={id} className={`rounded-2xl border border-[#183c35]/10 bg-white p-5 sm:p-6 ${className}`}>
      <h2 id={id} className="text-base font-semibold">
        {title}
      </h2>
      {description && <p className="mt-1 text-sm text-[#183c35]/70">{description}</p>}
      <div className="mt-4">{children}</div>
    </section>
  );
}

export const fieldClass =
  "h-9 w-full rounded-lg border border-[#183c35]/20 bg-white px-3 text-sm outline-none focus-visible:border-[#183c35] focus-visible:ring-3 focus-visible:ring-[#183c35]/25 disabled:cursor-not-allowed disabled:opacity-60 aria-invalid:border-red-600";

export const labelClass = "mb-1.5 block text-sm font-medium";
