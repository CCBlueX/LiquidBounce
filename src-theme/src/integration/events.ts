import type {
    Component,
    ConfigurableSetting,
    ItemStack,
    PlayerData,
    Proxy,
    Screen,
    Server,
    TextComponent
} from "./types";

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

export interface PlayerInventoryEvent {
    inventory: PlayerInventory;
}

export interface PlayerInventory {
    armor: ItemStack[];
    main: ItemStack[];
    crafting: ItemStack[];
    enderChest: ItemStack[];
}

export interface ProxyAdditionResultEvent {
    proxy: Proxy | null;
    error: string | null;
}

export interface ProxyEditResultEvent {
    proxy: Proxy | null;
    error: string | null;
}

export interface ProxyCheckResultEvent {
    proxy: Proxy;
    error: string | null;
}

export interface SpaceSeperatedNamesChangeEvent {
    value: boolean;
}

export interface ClickGuiScaleChangeEvent {
    value: number;
}

export interface BrowserUrlChangeEvent {
    url: string;
}
