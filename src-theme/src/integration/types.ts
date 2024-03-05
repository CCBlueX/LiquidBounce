export interface Module {
    name: string;
    category: string;
    keyBind: number;
    enabled: boolean;
    description: string;
    hidden: boolean;
}

export interface GroupedModules {
    [category: string]: Module[]
}

export type ModuleSetting = BlocksSetting | KeySetting | BooleanSetting | FloatSetting | FloatRangeSetting | IntSetting | IntRangeSetting | ChoiceSetting | ChooseSetting | ConfigurableSetting | TogglableSetting | ColorSetting | TextSetting;

export interface BlocksSetting {
    valueType: string;
    name: string;
    value: string[];
}

export interface TextSetting {
    valueType: string;
    name: string;
    value: string;
}

export interface ColorSetting {
    valueType: string;
    name: string;
    value: number;
}

export interface KeySetting {
    valueType: string;
    name: string;
    value: number;
}

export interface BooleanSetting {
    valueType: string;
    name: string;
    value: boolean;
}

export interface FloatSetting {
    valueType: string;
    name: string;
    range: {
        from: number;
        to: number;
    };
    suffix: string;
    value: number;
}

export interface FloatRangeSetting {
    valueType: string;
    name: string;
    range: {
        from: number;
        to: number;
    };
    suffix: string;
    value: {
        from: number,
        to: number
    };
}

export interface IntSetting {
    valueType: string;
    name: string;
    range: {
        from: number;
        to: number;
    };
    suffix: string;
    value: number;
}

export interface IntRangeSetting {
    valueType: string;
    name: string;
    range: {
        from: number;
        to: number;
    };
    suffix: string;
    value: {
        from: number,
        to: number
    };
}

export interface ChoiceSetting {
    valueType: string;
    name: string;
    active: string;
    choices: { [name: string]: ModuleSetting }
    value: ModuleSetting[];
}

export interface ChooseSetting {
    valueType: string;
    name: string;
    choices: string[];
    value: string;
}

export interface ConfigurableSetting {
    valueType: string;
    name: string;
    value: ModuleSetting[];
}

export interface TogglableSetting {
    valueType: string;
    name: string;
    value: ModuleSetting[];
}

export interface PersistentStorageItem {
    key: string;
    value: string;
}

export interface VirtualScreen {
    name: string;
    showingSplash: boolean;
}

export interface Scoreboard {
    header: TextComponent;
    entries: {
        name: TextComponent;
        score: TextComponent;
    }[];
}

export interface PlayerData {
    username: string;
    textures: SkinTextures;
    selectedSlot: number;
    gameMode: string;
    health: number,
    maxHealth: number;
    absorption: number;
    armor: number;
    food: number;
    air: number;
    maxAir: number;
    experienceLevel: number;
    experienceProgress: number;
    effects: StatusEffect[];
    mainHandStack: ItemStack;
    offHandStack: ItemStack;
    armorItems: ItemStack[];
    scoreboard: Scoreboard;
}

export interface SkinTextures {
    texture: string,
    textureUrl: string,
    capeTexture: string,
    elytraTexture: string,
}

export interface StatusEffect {
    effect: string;
    localizedName: string;
    duration: number;
    amplifier: number;
    ambient: boolean;
    infinite: boolean;
    visible: boolean;
    showIcon: boolean;
}

export interface ItemStack {
    identifier: string;
    count: number;
    damage: number;
    maxDamage: number;
}

export interface PrintableKey {
    translationKey: string;
    localized: string;
}

export interface Registries {
    blocks: {
        identifier: string;
        name: string;
    }[];
    items: {
        identifier: string;
        name: string;
    }[];
}

export interface Session {
    username: string;
    accountType: string;
    avatar: string;
    premium: boolean;
    uuid: string;
}

export interface Server {
    id: number;
    address: string;
    icon: string;
    label: TextComponent | string;
    players: {
        max: number;
        online: number;
    };
    name: string;
    online: boolean;
    playerCountLabel: string;
    protocolVersion: number;
    version: string;
    ping: number;
    resourcePackPolicy: string;
}

export interface ServerPingedEvent {
    server: Server;
}

export interface TextComponent {
    type?: string;
    extra?: (TextComponent | string)[];
    color: string;
    bold?: boolean;
    italic?: boolean;
    underlined?: boolean;
    strikethrough?: boolean;
    obfuscated?: boolean;
    font?: string;
    text: string;
}

export interface Protocol {
    name: string;
    version: number;
}

export interface Account {
    avatar: string;
    favorite: boolean;
    id: number;
    type: string;
    username: string;
    uuid: string;
}

export interface World {
    id: number;
    name: string;
    displayName: string;
    lastPlayed: number;
    gameMode: string;
    difficulty: string;
    icon: string | undefined;
    hardcore: boolean;
    commandsAllowed: boolean;
    version: string;
}

export interface Proxy {
    id: number;
    host: string;
    port: number;
    username: string | undefined;
    password: string | undefined;
}