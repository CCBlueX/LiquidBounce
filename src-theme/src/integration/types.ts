export interface Module {
    name: string;
    category: string;
    keyBind: number;
    enabled: boolean;
    description: string;
    hidden: boolean;
    aliases: string[];
    tag: string | null;
}

export interface GroupedModules {
    [category: string]: Module[]
}

export type ModuleSetting =
    BlocksSetting
    | BooleanSetting
    | FloatSetting
    | FloatRangeSetting
    | IntSetting
    | IntRangeSetting
    | ChoiceSetting
    | ChooseSetting
    | MultiChooseSetting
    | ListSetting
    | RegistryListSetting
    | ItemListSetting
    | ConfigurableSetting
    | TogglableSetting
    | ColorSetting
    | TextSetting
    | BindSetting
    | VectorSetting
    | KeySetting;

export interface BlocksSetting {
    valueType: string;
    name: string;
    value: string[];
}

export interface KeySetting {
    valueType: string;
    name: string;
    value: string;
}

export interface BindSetting {
    valueType: string;
    name: string;
    value: {
        boundKey: string;
        action: string;
    };
    defaultValue: {
        boundKey: string;
        action: string;
    };
}

export interface TextSetting {
    valueType: string;
    name: string;
    value: string;
}

export interface VectorSetting {
    valueType: string;
    name: string;
    value: Vec3;
}

export interface ColorSetting {
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

export interface MultiChooseSetting {
    valueType: string;
    name: string;
    choices: string[];
    value: string[];
    canBeNone: boolean;
}

export interface ListSetting {
    valueType: string;
    name: string;
    value: string[];
    innerValueType: string;
}

export interface RegistryListSetting extends ListSetting {
    registry: string;
}

export interface ItemListSetting extends ListSetting {
    items: NamedItem[];
}

export interface NamedItem {
    name: string;
    value: string;
    icon: string | undefined;
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
    uuid: string;
    position: Vec3;
    blockPosition: Vec3;
    velocity: Vec3;
    selectedSlot: number;
    gameMode: string;
    health: number,
    actualHealth: number;
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

export interface StatusEffect {
    effect: string;
    localizedName: string;
    duration: number;
    amplifier: number;
    ambient: boolean;
    infinite: boolean;
    visible: boolean;
    showIcon: boolean;
    color: number;
}

export interface Vec3 {
    x: number;
    y: number;
    z: number;
}

export interface ItemStack {
    identifier: string;
    count: number;
    damage: number;
    maxDamage: number;
    displayName: TextComponent | string;
    /**
     * @deprecated use {@link enchantments} instead.
     */
    hasEnchantment: boolean;
    enchantments?: Record<string, number>;
}

export interface PrintableKey {
    translationKey: string;
    localized: string;
}

export interface MinecraftKeybind {
    bindName: string;
    key: PrintableKey;
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
    type: 'HTTP' | 'SOCKS5';
    forwardAuthentication: boolean;
    favorite: boolean;
    credentials: {
        username: string;
        password: string;
    } | undefined;
    ipInfo: {
        city?: string;
        country?: string;
        ip: string;
        loc?: string;
        org?: string;
        postal?: string;
        region?: string;
        timezone?: string;
    } | undefined;
}

export interface GameWindow {
    width: number;
    height: number;
    scaledWidth: number;
    scaledHeight: number;
    scaleFactor: number;
    guiScale: number;
}

export interface Component {
    name: string;
    settings: { [name: string]: any };
}

export interface ClientInfo {
    gameVersion: string;
    clientVersion: string;
    clientName: string;
    development: boolean;
    fps: number;
    gameDir: string;
    inGame: boolean;
    viaFabricPlus: boolean;
    hasProtocolHack: boolean;
}

export interface ClientUpdate {
    development: boolean;
    commit: string;
    update: {
        buildId: number | undefined;
        commitId: string | undefined;
        branch: string | undefined;
        clientVersion: string | undefined;
        minecraftVersion: string | undefined;
        release: boolean;
        date: string;
        message: string;
        url: string;
    } | undefined;
}

export interface Browser {
    url: string
}

export interface HitResult {
    type: "block" | "entity" | "miss";
    pos: Vec3;
}

export interface BlockHitResult extends HitResult {
    blockPos: Vec3;
    side: string;
    isInsideBlock: boolean;
}

export interface EntityHitResult extends HitResult {
    entityName: string;
    entityType: string;
    entityPos: Vec3;
}

export interface GeneratorResult {
    name: string;
}

export interface Screen {
    class: string,
    title: string,
}

export interface RegistryItem {
    name: string;
    icon: string | undefined;
}
