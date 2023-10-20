import type { Account } from "~/utils/types";

type AccountEntryProps = {
  account: Account;
};

export default function AccountEntry({ account }: AccountEntryProps) {
  return (
    <div className="flex space-x-4 items-center bg-black/40 py-4 px-5 rounded-md">
      {/* Account Head Wrapper */}
      <div className="relative h-[68px] w-[68px]">
        {/* Account Head */}
        <img
          src={`https://crafatar.com/avatars/${account.uuid}?size=100`}
          alt="Account Head"
          className="rounded-full"
        />
      </div>

      {/* Metadata */}
      <div className="flex flex-col space-y-1">
        {/* Username Wrapper */}
        <div className="text-white text-xl font-semibold uppercase">
          {account.username}
        </div>

        {/* E-mail */}
        <div className="text-white/50 text-xl font-semibold">
          {account.email}
        </div>
      </div>
    </div>
  );
}
