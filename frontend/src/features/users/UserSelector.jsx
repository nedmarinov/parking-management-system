import { SectionState } from "@/components/Feedback";
import { fieldClass, labelClass } from "@/components/Panel";

export function UserSelector({ users, selectedUserId, onChange, disabled }) {
  return (
    <SectionState state={users} empty="No demo users are available." isEmpty={(list) => list.length === 0} onRetry={users.retry}>
      <label htmlFor="current-user" className={labelClass}>
        Current user
      </label>
      <select
        id="current-user"
        className={fieldClass}
        value={selectedUserId ?? ""}
        disabled={disabled}
        onChange={(event) => onChange(Number(event.target.value))}
      >
        {users.data?.map((user) => (
          <option key={user.id} value={user.id}>
            {user.name}
          </option>
        ))}
      </select>
      <p className="mt-2 text-xs text-[#183c35]/65">Demo accounts — no sign-in. Balances are fictional EUR.</p>
    </SectionState>
  );
}
