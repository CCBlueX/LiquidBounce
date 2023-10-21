import { useMemo } from "react";

type Module = {
  category: string;
  name: string;
  description?: string;
  instance: unknown;
  enabled: boolean;
};

const modules: Module[] = [
  {
    name: "KillAura",
    category: "Combat",
    description: "Automatically attacks nearby players.",
    instance: null,
    enabled: false,
  },
];

export function useModules() {
  function toggleModule(name: string) {
    const module = modules.find((module) => module.name === name);
    if (!module) return;

    module.enabled = !module.enabled;
  }

  const modulesByCategory = useMemo(() => {
    const modulesByCategory: Record<string, Module[]> = {};

    for (const module of modules) {
      if (!modulesByCategory[module.category])
        modulesByCategory[module.category] = [];

      modulesByCategory[module.category].push(module);
    }

    return modulesByCategory;
  }, []);

  return { modules, toggleModule, modulesByCategory };
}
