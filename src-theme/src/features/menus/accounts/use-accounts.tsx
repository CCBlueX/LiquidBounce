import type { Account } from "~/utils/types";

const accounts: Account[] = [
  {
    email: "placeholder@example.com",
    username: "NurMarvin",
    uuid: "3e395dd4-7158-4641-a469-35001933cf70",
    password: "password",
  },
];

export function useAccounts() {
  return { accounts };
}
