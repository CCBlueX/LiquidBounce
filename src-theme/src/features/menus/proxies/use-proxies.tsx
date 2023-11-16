import type { Proxy } from "~/utils/types";

const proxies: Proxy[] = [
  {
    type: "socks5",
    location: "de",
    host: "127.0.0.1",
    port: 9050,
    direct: false,
    password: "password",
    username: "username",
  },
];

export function useProxies() {
  return { proxies };
}
