import { useMemo, useState } from "react";

type BaseModuleSetting = {
  name: string;
  description?: string;
};

export type StringModuleSetting = BaseModuleSetting & {
  type: "string";
  value: string;
};

export type BooleanModuleSetting = BaseModuleSetting & {
  type: "boolean";
  value: boolean;
};

export type NumberModuleSetting = BaseModuleSetting & {
  type: "number";
  value: number;
  min?: number;
  max?: number;
  step?: number;
};

export type RangeModuleSetting = BaseModuleSetting & {
  type: "range";
  value: [number, number];
  min: number;
  max: number;
  step?: number;
};

export type EnumModuleSetting = BaseModuleSetting & {
  type: "enum";
  value: string;
  values: string[];
};

export type ColorModuleSetting = BaseModuleSetting & {
  type: "color";
  value: string;
};

export type ModuleSetting =
  | StringModuleSetting
  | BooleanModuleSetting
  | RangeModuleSetting
  | EnumModuleSetting
  | ColorModuleSetting
  | NumberModuleSetting;

export type Module = {
  category: string;
  name: string;
  description?: string;
  settings: ModuleSetting[];
  enabled: boolean;
};

const dummyModules: Module[] = [
  {
    name: "KillAura",
    category: "Combat",
    description: "Automatically attacks nearby players.",
    settings: [
      {
        name: "Range",
        type: "range",
        value: [3.5, 5],
        min: 0,
        max: 10,
      },
    ],
    enabled: false,
  },
  {
    name: "AutoClicker",
    category: "Combat",
    description: "Automatically clicks for you.",
    settings: [
      {
        name: "CPS",
        type: "range",
        value: [10, 15],
        min: 1,
        max: 20,
        step: 0.5,
      },
    ],
    enabled: false,
  },
  {
    name: "Fly",
    category: "Movement",
    description: "Allows you to fly.",
    settings: [
      {
        name: "Speed",
        type: "number",
        value: 1,
        min: 0.1,
        max: 10,
      },
    ],
    enabled: false,
  },
  {
    name: "Speed",
    category: "Movement",
    description: "Allows you to move faster.",
    settings: [
      {
        name: "Speed",
        type: "number",
        value: 1,
        min: 0.1,
        max: 10,
      },
      {
        name: "Strafe",
        type: "boolean",
        value: false,
      },
      {
        name: "Mode",
        type: "enum",
        value: "Vanilla",
        values: ["Vanilla", "Bhop", "OnGround"],
      },
    ],
    enabled: false,
  },
  {
    name: "Scaffold",
    category: "World",
    description: "Automatically places blocks under you.",
    settings: [
      {
        name: "SafeWalk",
        type: "boolean",
        value: false,
      },
    ],
    enabled: false,
  },
  {
    name: "Crasher",
    category: "Exploit",
    description: "Crashes the server.",
    settings: [
      {
        name: "Mode",
        type: "enum",
        value: "Packet",
        values: ["Packet", "Book"],
      },
    ],
    enabled: false,
  },
  {
    name: "Derp",
    category: "Fun",
    description: "Makes you look like you're derping.",
    settings: [
      {
        name: "Headless",
        type: "boolean",
        value: false,
      },
    ],
    enabled: false,
  },
  {
    name: "Spammer",
    category: "Fun",
    description: "Spams the chat.",
    settings: [
      {
        name: "Message",
        type: "string",
        value: "Hello, world!",
      },
    ],
    enabled: false,
  },
  {
    name: "Freecam",
    category: "Player",
    description: "Allows you to move your camera freely.",
    settings: [
      {
        name: "Speed",
        type: "number",
        value: 1,
        min: 0.1,
        max: 10,
      },
    ],
    enabled: false,
  },
  {
    name: "Example",
    category: "Render",
    description: "Example module.",
    settings: [
      {
        name: "Enum",
        type: "enum",
        value: "Option 1",
        values: ["Option 1", "Option 2", "Option 3", "Option 4"],
      },
      {
        name: "Color",
        type: "color",
        value: "#ffffff",
      },
      {
        name: "Boolean",
        type: "boolean",
        value: false,
      },
      {
        name: "Range",
        type: "range",
        value: [5, 10],
        min: 0,
        max: 20,
        step: 0.5,
      },
      {
        name: "Number",
        type: "number",
        value: 2,
        min: 0,
        max: 10,
        step: 2,
      },
      {
        name: "String",
        type: "string",
        value: "Hello, world!",
      },
    ],
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
