import type {
    Component,
    ConfigurableSetting,
    ItemStack,
    PlayerData,
    Proxy,
    Screen,
    Server,
    TextComponent,
} from "./types";

export interface EventMap {
    socketReady: void;
    clickGuiValueChange: ClickGuiValueChangeEvent;
    moduleToggle: ModuleToggleEvent;
    keyboardKey: KeyboardKeyEvent;
    mouseButton: MouseButtonEvent;
    scaleFactorChange: ScaleFactorChangeEvent;
    componentsUpdate: ComponentsUpdateEvent;
    clientPlayerData: ClientPlayerDataEvent;
    overlayMessage: OverlayMessageEvent;
    notification: NotificationEvent;
    keyEvent: KeyEvent;
    targetChange: TargetChangeEvent;
    blockCountChange: BlockCountChangeEvent;
    accountManagerAddition: AccountManagerAdditionEvent;
    accountManagerRemoval: AccountManagerRemovalEvent;
    accountManagerMessage: AccountManagerMessageEvent;
    accountManagerLogin: AccountManagerLoginEvent;
    serverPinged: ServerPingedEvent;
    clientPlayerInventory: ClientPlayerInventoryEvent;
    proxyCheckResult: ProxyCheckResultEvent;
    spaceSeperatedNamesChange: SpaceSeperatedNamesChangeEvent;
    browserUrlChange: BrowserUrlChangeEvent;
}

export interface ClickGuiValueChangeEvent {
    configurable: ConfigurableSetting;
}

export interface ModuleToggleEvent {
    moduleName: string;
    hidden: boolean;
    enabled: boolean;
}

export interface KeyboardKeyEvent {
    keyCode: number;
    scanCode: number;
    action: number;
    mods: number;
    key: string;
    screen: Screen | undefined;
}

export interface MouseButtonEvent {
    key: string;
    button: number;
    action: number;
    mods: number;
    screen: Screen | undefined;
}

export interface ScaleFactorChangeEvent {
    scaleFactor: number;
}

export interface ComponentsUpdateEvent {
    id: string | null;
    components: Component[];
}

export interface ClientPlayerDataEvent {
    playerData: PlayerData;
}

export interface OverlayMessageEvent {
    text: TextComponent | string;
    tinted: boolean;
}

export interface NotificationEvent {
    title: string;
    message: string;
    severity: "INFO" | "SUCCESS" | "ERROR" | "ENABLED" | "DISABLED";
}

export interface KeyEvent {
    key: string;
    action: number;
    mods: number;
}

export interface TargetChangeEvent {
    target: PlayerData | null;
}

export interface BlockCountChangeEvent {
    count?: number;
}

export interface AccountManagerAdditionEvent {
    username: string | null;
    error: string | null;
}

export interface AccountManagerRemovalEvent {
    username: string | null;
}

export interface AccountManagerMessageEvent {
    message: string;
}

export interface AccountManagerLoginEvent {
    username: string | null;
    error: string | null;
}

export interface ServerPingedEvent {
    server: Server;
}

export interface ClientPlayerInventoryEvent {
    inventory: PlayerInventory;
}

export interface PlayerInventory {
    armor: ItemStack[];
    main: ItemStack[];
    crafting: ItemStack[];
    enderChest: ItemStack[];
}

export interface ProxyCheckResultEvent {
    proxy: Proxy | null;
    error: string | null;
}

export interface SpaceSeperatedNamesChangeEvent {
    value: boolean;
}

export interface BrowserUrlChangeEvent {
    url: string;
}
