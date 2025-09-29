
export interface Metadata {
    id: string;
    name: string;
    version: string;
    authors: string[];
    screens: string[];
    overlays: string[];
    components: string[];
    fonts: string[];
    backgrounds: {
        name: string;
        types: string[];
    }[];
}

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
    | KeySetting
    | FileSetting
    | CurveSetting;

export type File = string;

export type FileDialogMode = "OPEN_FILE" | "OPEN_FOLDER" | "SAVE_FILE";

export interface FileSelectDialog {
    mode: FileDialogMode;
    supportedExtensions: string[] | undefined;
}

export interface FileSelectResult {
    file: File | undefined;
}

export interface Setting<V> {
    valueType: string;
    name: string;
    value: V;
    description: string;
    key: string;
}

export interface FileSetting extends Setting<File> {
    dialogMode: FileDialogMode;
    supportedExtensions: string[] | undefined;
}

export interface CurveSetting extends Setting<Vector2f[]> {
    xAxis: {
        label: string;
        range: Range;
    },
    yAxis: {
        label: string;
        range: Range;
    }
    tension: number;
}

export interface BlocksSetting extends Setting<string[]> {
}

export interface KeySetting extends Setting<string> {
}

export interface BindSetting extends Setting<InputBind> {
    defaultValue: InputBind;
}

export interface TextSetting extends Setting<string> {
}

export interface VectorSetting extends Setting<Vec3> {
}

export interface ColorSetting extends Setting<number> {
}

export interface BooleanSetting extends Setting<boolean> {
}

export interface FloatSetting extends Setting<number> {
    range: Range;
    suffix: string;
}

export interface FloatRangeSetting extends Setting<Range> {
    range: Range;
    suffix: string;
}

export interface IntSetting extends Setting<number> {
    range: Range;
    suffix: string;
}

export interface IntRangeSetting extends Setting<Range> {
    range: Range;
    suffix: string;
}

export interface ChoiceSetting extends Setting<ModuleSetting[]> {
    active: string;
    choices: { [name: string]: ModuleSetting }
}

export interface ChooseSetting extends Setting<string> {
    choices: string[];
}

export interface MultiChooseSetting extends Setting<string[]> {
    choices: string[];
    canBeNone: boolean;
}

export interface ListSetting extends Setting<string[]> {
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

export interface ConfigurableSetting extends Setting<ModuleSetting[]> {
}

export interface TogglableSetting extends Setting<ModuleSetting[]> {
}

export interface InputBind {
    boundKey: string;
    action: "Toggle" | "Hold";
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

export interface Theme {
    name: string;
    id: string;
    settings: { [name: string]: any };
}

export interface Component {
    name: string;
    id: string;
    settings: { [name: string]: any };
}

export interface Alignment {
    horizontalAlignment: HorizontalAlignment;
    verticalAlignment: VerticalAlignment;
    horizontalOffset: number;
    verticalOffset: number;
}

export enum HorizontalAlignment {
    LEFT = "Left",
    RIGHT = "Right",
    CENTER = "Center",
    CENTER_TRANSLATED = "CenterTranslated",
}

export enum VerticalAlignment {
    TOP = "Top",
    BOTTOM = "Bottom",
    CENTER = "Center",
    CENTER_TRANSLATED = "CenterTranslated",
}

export interface ClientInfo {
    gameVersion: string;
    clientVersion: string;
    clientName: string;
    development: boolean;
    fps: number;
    gameDir: File;
    clientDir: File;
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

export interface Range {
    from: number;
    to: number;
}

export interface Vector2f {
    x: number;
    y: number;
}

export interface BedState {
    block: string;
    pos: Vec3;
    surroundingBlocks: SurroundingBlock[];
    compactSurroundingBlocks: SurroundingBlock[];
}

export interface SurroundingBlock {
    block: string;
    count: number;
    layer: number;
}

type MouseKeyName =
    | "left"
    | "right"
    | "middle"
    | "4"
    | "5"
    | "6"
    | "7"
    | "8";

type KeyboardKeyName =
    | "unknown"
    | "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9"
    | "a" | "b" | "c" | "d" | "e" | "f" | "g" | "h" | "i" | "j"
    | "k" | "l" | "m" | "n" | "o" | "p" | "q" | "r" | "s" | "t"
    | "u" | "v" | "w" | "x" | "y" | "z"
    | `f${1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 | 14 | 15 | 16 | 17 | 18 | 19 | 20 | 21 | 22 | 23 | 24 | 25}`
    | "escape"
    | "enter"
    | "tab"
    | "space"
    | "backspace"
    | "caps.lock"
    | "left.shift" | "right.shift"
    | "left.control" | "right.control"
    | "left.alt" | "right.alt"
    | "left.win" | "right.win"
    | "menu"
    | "print.screen"
    | "scroll.lock"
    | "pause"
    | "insert"
    | "delete"
    | "home"
    | "end"
    | "page.up" | "page.down"
    | "up" | "down" | "left" | "right"
    | "num.lock"
    | "keypad.0" | "keypad.1" | "keypad.2" | "keypad.3"
    | "keypad.4" | "keypad.5" | "keypad.6" | "keypad.7"
    | "keypad.8" | "keypad.9"
    | "keypad.add" | "keypad.subtract"
    | "keypad.multiply" | "keypad.divide"
    | "keypad.enter" | "keypad.decimal" | "keypad.equal"
    | "semicolon" | "equal" | "comma"
    | "minus" | "period" | "slash"
    | "grave.accent" | "left.bracket" | "backslash"
    | "right.bracket" | "apostrophe"
    | "world.1" | "world.2";

export type MinecraftMouseKey = `key.mouse.${MouseKeyName}`;
export type MinecraftKeyboardKey = `key.keyboard.${KeyboardKeyName}`;
export type MinecraftKey = MinecraftMouseKey | MinecraftKeyboardKey;
