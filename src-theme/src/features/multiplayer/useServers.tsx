import gomme from "~/assets/gomme.png";
import hypixel from "~/assets/hypixel.png";

import type { Server } from "~/utils/types";

// TODO: Pull server data from client
const servers: Server[] = [
  {
    name: "GommeHD",
    players: 1337,
    latency: Math.floor(Math.random() * 500),
    motd: "GommeHD.net - GommeHD Network - [1.8-1.20.1] - MONEYMAKER UPDATE (NEUE MINE & BALANCING)",
    ip: "gommehd.net",
    icon: gomme,
  },
  {
    name: "Hypixel",
    players: 1337,
    latency: Math.floor(Math.random() * 500),
    motd: "Hypixel Network [1.8-1.20] - SKYBLOCK 0.19 THE RIFT - SUMMER EVENT",
    ip: "hypixel.net",
    icon: hypixel,
  },
];

export function useServers() {
  return { servers };
}
