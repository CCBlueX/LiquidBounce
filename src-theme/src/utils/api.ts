const BASE_API_URL = "http://localhost:15743/api/v1/client";

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE_API_URL}${path}`, {
    ...options,
  });
  const data = await response.json();

  if (!response.ok) {
    if (data.reason) throw new Error(data.reason);

    throw new Error("An unknown error occurred");
  }

  return data;
}

export type World = {
  name: string;
  displayName: string;
  lastPlayed: number;
  gameMode: string;
  difficulty: string;
  icon: string;
  hardcore: boolean;
  commandsAllowed: boolean;
};

export function getWorlds(): Promise<World[]> {
  return request("/worlds");
}

export type Server = {
  name: string;
  address: string;
  online: boolean;
  playerList: string[];
  version: {
    content: {
      string: string;
    };
    siblings: unknown[];
    style: unknown;
    ordered: unknown;
  };
  protocolVersion: number;
  players: unknown;
  icon: string;
};

export async function getServers(): Promise<Server[]> {
  return request("/servers");
}

/**
 * {
	"health": 20.0,
	"maxHealth": 20.0,
	"food": 20,
	"experienceProgress": 0.73333347,
	"dead": false
}
 */
export type LocalPlayer = {
  health: number;
  maxHealth: number;
  food: number;
  experienceProgress: number;
  dead: boolean;
};

export function getLocalPlayer(): Promise<LocalPlayer> {
  return request("/player");
}

export type Module = {
  name: string;
  category: string;
  keyBind: number;
  enabled: boolean;
  description: string;
  hidden: boolean;
};

export function getModules(): Promise<Module[]> {
  return request("/modules");
}

type ValueType =
  | "BOOLEAN"
  | "FLOAT"
  | "FLOAT_RANGE"
  | "INT"
  | "INT_RANGE"
  | "TEXT"
  | "TEXT_ARRAY"
  | "CURVE"
  | "COLOR"
  | "BLOCK"
  | "BLOCKS"
  | "ITEM"
  | "ITEMS"
  | "CHOICE"
  | "CHOOSE"
  | "INVALID"
  | "CONFIGURABLE"
  | "TOGGLEABLE";

type Range = {
  from: number;
  to: number;
};

export interface Value<TValue = unknown> {
  name: string;
  value: TValue;
  valueType: ValueType;
}

export interface BooleanValue extends Value {
  name: string;
  value: boolean;
  valueType: "BOOLEAN";
}

export interface FloatValue extends Value {
  name: string;
  value: number;
  range: Range;
  valueType: "FLOAT";
}

export interface FloatRangeValue extends Value {
  name: string;
  range: Range;
  value: Range;
  valueType: "FLOAT_RANGE";
}

export interface IntValue extends Value {
  name: string;
  value: number;
  range: Range;
  valueType: "INT";
}

export interface IntRangeValue extends Value {
  name: string;
  range: Range;
  value: Range;
  valueType: "INT_RANGE";
}

export interface TextValue extends Value {
  name: string;
  value: string;
  valueType: "TEXT";
}

export interface TextArrayValue extends Value {
  name: string;
  value: string[];
  valueType: "TEXT_ARRAY";
}

export interface CurveValue extends Value {
  name: string;
  value: unknown;
  valueType: "CURVE";
}

export interface ColorValue extends Value {
  name: string;
  value: string;
  valueType: "COLOR";
}

export interface BlockValue extends Value {
  name: string;
  value: string;
  valueType: "BLOCK";
}

export interface BlocksValue extends Value {
  name: string;
  value: string[];
  valueType: "BLOCKS";
}

export interface ItemValue extends Value {
  name: string;
  value: string;
  valueType: "ITEM";
}

export interface ItemsValue extends Value {
  name: string;
  value: string[];
  valueType: "ITEMS";
}

export interface ChoiceValue extends Value {
  name: string;
  active: string;
  value: string[];
  choices: {
    [key: string]: Value;
  };
  valueType: "CHOICE";
}

export interface ChooseValue extends Value {
  name: string;
  value: string;
  choices: {
    [key: string]: Value;
  };
  valueType: "CHOOSE";
}

export interface InvalidValue extends Value {
  name: string;
  value: unknown;
  valueType: "INVALID";
}

export interface ConfigurableValue extends Value {
  name: string;
  value: Values[];
  valueType: "CONFIGURABLE";
}

export interface ToggleableValue extends Value {
  name: string;
  value: boolean;
  valueType: "TOGGLEABLE";
}

export type Values =
  | BooleanValue
  | FloatValue
  | FloatRangeValue
  | IntValue
  | IntRangeValue
  | TextValue
  | TextArrayValue
  | CurveValue
  | ColorValue
  | BlockValue
  | BlocksValue
  | ItemValue
  | ItemsValue
  | ChoiceValue
  | ChooseValue
  | InvalidValue
  | ConfigurableValue
  | ToggleableValue;

export function getModuleSettings(name: string): Promise<ConfigurableValue> {
  const searchParams = new URLSearchParams({ name });

  return request("/modules/settings?" + searchParams.toString(), {
    headers: {
      "Content-Type": "application/json",
    },
  });
}
