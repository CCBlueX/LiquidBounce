import { useMemo, useState } from "react";

export type Module = {
  category: string;
  name: string;
  description?: string;
  instance: unknown;
  enabled: boolean;
};

const dummyModules: Module[] = [
  {
    name: "KillAura",
    category: "Combat",
    description: "Automatically attacks nearby players.",
    instance: null,
    enabled: false,
  },
  {
    name: "AutoClicker",
    category: "Combat",
    description: "Automatically clicks for you.",
    instance: null,
    enabled: false,
  },
  {
    name: "Fly",
    category: "Movement",
    description: "Allows you to fly.",
    instance: null,
    enabled: false,
  },
  {
    name: "Speed",
    category: "Movement",
    description: "Allows you to move faster.",
    instance: null,
    enabled: false,
  },
  {
    name: "Scaffold",
    category: "World",
    description: "Automatically places blocks under you.",
    instance: null,
    enabled: false,
  },
  {
    name: "Crasher",
    category: "Exploit",
    description: "Crashes the server.",
    instance: null,
    enabled: false,
  },
  {
    name: "Derp",
    category: "Fun",
    description: "Makes you look like you're derping.",
    instance: null,
    enabled: false,
  },
  {
    name: "Freecam",
    category: "Player",
    description: "Allows you to move your camera freely.",
    instance: null,
    enabled: false,
  },
];

export function useModules() {
  const [modules, setModules] = useState(dummyModules);

  function toggleModule(name: string) {
    const module = modules.find((module) => module.name === name);
    if (!module) return;

    module.enabled = !module.enabled;
    setModules([...modules]);
  }

  const modulesByCategory = useMemo(() => {
    const modulesByCategory: Record<string, Module[]> = {};

    for (const module of modules) {
      if (!modulesByCategory[module.category])
        modulesByCategory[module.category] = [];

      modulesByCategory[module.category].push(module);
    }

    return modulesByCategory;
  }, [modules]);

  return { modules, toggleModule, modulesByCategory };
}
