export interface Metadata {
    id: string;
    name: string;
    version: string;
    authors: string[];
    colors: {
        Accent: string;
        Tint: string;
    }
    screens: string[];
    overlays: string[];
    components: string[];
    fonts: string[];
    backgrounds: {
        name: string;
        types: string[];
    }[];
}

export interface ModuleCategory {
    name: string;
    icon: string | null;
}

export interface Module {
    name: string;
    category: string;
    keyBind: InputBind;
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
    | RegistryMutableListSetting
    | ConfigurableSetting
    | TogglableSetting
    | ColorSetting
    | TextSetting
    | BindSetting
    | Vec2Setting
    | Vec3Setting
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
    description: string | undefined;
    key: string | undefined;
}

export interface FileSetting extends Setting<File> {
    dialogMode: FileDialogMode;
    supportedExtensions: string[] | undefined;
}

export interface CurveSetting extends Setting<Vec2[]> {
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

export interface Vec2Setting extends Setting<Vec2> {
}

export interface Vec3Setting extends Setting<Vec3> {
    useLocateButton: boolean;
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
    isOrderSensitive: boolean;
}

export interface ListSetting extends Setting<string[]> {
    innerValueType: string;
}

export interface RegistryListSetting extends ListSetting {
    registry: string;
}

export interface RegistryMutableListSetting extends Setting<string[]> {
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
    action: BindAction;
    modifiers: BindModifier[];
}

export type BindAction = "Toggle" | "Hold" | "Smart";

export type BindModifier = "Shift" | "Control" | "Alt" | "Super";

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
    yaw: number;
    pitch: number;
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

export interface Vec2 extends Vec<"x" | "y"> {
}

export interface Vec3 extends Vec<"x" | "y" | "z"> {
}

export type VecAxis = "x" | "y" | "z" | "w";

export type Vec<D extends VecAxis> = Record<D, number>;

export interface ItemStack {
    identifier: string;
    count: number;
    damage: number;
    maxDamage: number;
    displayName: TextComponent | string;
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
    type: string;
    service: string;
    avatar: string;
    online: boolean;
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
    lan?: boolean;
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
    colors: {
        accent: number;
        tint: number;
    };
    settings: { [name: string]: any };
}

export interface HudComponent {
    name: string;
    description: string;
    id: string;
    settings: { [name: string]: any };
    width?: number;
    height?: number;
}

export interface HudComponentCatalogEntry {
    name: string;
    description: string;
    id: string;
    singleton: boolean;
    canAdd: boolean;
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

export type OS = "linux" | "solaris" | "windows" | "mac" | "unknown";

export interface ClientInfo {
    os: OS;
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

export interface ClientUser {
    userId: string;
    email: string;
    name: string | null;
    nickname: string | null;
    groups: string[];
    premium: boolean;
    admin: boolean;
}

export interface RegistryItem {
    name: string;
    icon: string | undefined;
}

export interface Range {
    from: number;
    to: number;
}

export interface BedState {
    block: string;
    trackedBlockPos: Vec3;
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

export type MarketplaceItemType = "Config" | "Theme" | "Addon" | "Script" | "Other";

export type ConfigTrackerState = "None" | "Tracked" | "Editing";

export type MarketplaceVisibility = "public" | "unlisted";

export interface MarketplacePagination {
    current: number;
    pages: number;
    items: number;
}

export interface MarketplaceContext {
    server?: string;
    autoConfig: boolean;
    onlyFeatured: boolean;
    user?: string;
}

export interface MarketplaceLinkedConfig {
    id: number;
    address: string;
}

export interface MarketplaceConfig {
    id: number;
    name: string;
    address: string;
    image?: string;
    featured: boolean;
    binds: boolean;
    tags: string[];
    servers: string[];
    protocol?: string;
    protocolMatches: boolean;
    works: number;
    fails: number;
    downloads: number;
    updatedAt?: number;
    overlayOn?: MarketplaceLinkedConfig;
    tracking: ConfigTrackerState;
    own: boolean;
    visibility?: MarketplaceVisibility;
}

export interface MarketplaceConfigPage {
    items: MarketplaceConfig[];
    pagination: MarketplacePagination;
    code: boolean;
    unfeatured: number;
}

export interface MarketplaceRevision {
    id: number;
    version: string;
    liquidbounce?: string;
    createdAt?: number;
    changelog?: string;
}

export interface MarketplaceItem {
    id: number;
    type: MarketplaceItemType;
    name: string;
    author?: string;
    image?: string;
    summary: string;
    featured: boolean;
    downloads: number;
    rating?: number;
    reviews: number;
    subscribed: boolean;
    installable: boolean;
    notFor?: string;
    installed?: MarketplaceRevision;
    update: boolean;
    restartRequired: boolean;
    inUse: boolean;
}

export interface MarketplaceItemPage {
    items: MarketplaceItem[];
    pagination: MarketplacePagination;
}

export interface MarketplaceVersion {
    revision: MarketplaceRevision;
    installed: boolean;
    fits: boolean;
}

export interface MarketplaceItemDetail {
    item: MarketplaceItem;
    description: string;
    liquidbounce: string;
    versions: MarketplaceVersion[];
}

export interface MarketplaceDependency {
    id: number;
    type: MarketplaceItemType;
    address: string;
    image?: string;
    order?: number;
    featured: boolean;
    protocol?: string;
    protocolMatches: boolean;
    status?: MarketplaceItem;
}

export interface MarketplaceConfigRevision {
    id: number;
    createdAt?: number;
    changelog?: string;
    works: number;
    fails: number;
    latest: boolean;
    loaded: boolean;
    first: boolean;
}

export interface MarketplaceConfigDetail {
    config: MarketplaceConfig;
    summary: string;
    description: string;
    createdAt?: number;
    shareCode?: string;
    forkOf?: MarketplaceLinkedConfig;
    dependencies: MarketplaceDependency[];
    revisions: MarketplaceConfigRevision[];
    changes?: string[];
    report?: boolean;
}

export interface MarketplaceLoadPlan {
    installs: {
        id: number;
        type: MarketplaceItemType;
        name: string;
        author?: string;
        image?: string;
        revision: MarketplaceRevision;
        restart: boolean;
    }[];
    leftOut: {
        id: number;
        type: MarketplaceItemType;
        name: string;
        image?: string;
    }[];
    modules: string[];
}

export interface MarketplaceLoadResult {
    installed: string[];
}

export interface MarketplaceInstallResult {
    installed: string[];
}

export interface MarketplaceReport {
    works: number;
    fails: number;
    report?: boolean;
}

export interface ConfigTracker {
    state: ConfigTrackerState;
    id: number;
    address: string;
    image?: string;
    own: boolean;
    backup: boolean;
    edited: string[];
}

export interface MarketplacePublished {
    id: number;
    address: string;
    visibility?: MarketplaceVisibility;
    shareCode?: string;
}

export interface MarketplaceInstalledItem {
    id: number;
    type: MarketplaceItemType;
    name: string;
}

export interface MarketplaceConfigDetails {
    name: string;
    description: string;
    tags: string[];
    servers: string[];
    visibility: MarketplaceVisibility;
}

export interface MarketplaceConfigQuery {
    page: number;
    query: string;
    tags: string[];
    server: boolean;
    featured: boolean;
    sort: "top" | "new";
}
