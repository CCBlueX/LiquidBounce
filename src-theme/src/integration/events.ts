import type {
    Component,
    ConfigurableSetting,
    ItemStack,
    PlayerData,
    Proxy,
    Screen,
    Server, Session,
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
    chatReceive: ChatReceiveEvent;
    connectionDetails: ConnectionDetailsEvent;
    overlayTitle: OverlayTitleEvent;
    overlayPlayList: OverlayPlayListEvent;
    overlayChat: OverlayChatEvent;
    overlayDisconnection: OverlayDisconnectionEvent;
    progress: ProgressEvent;

    session: SessionEvent;
    key: KeyEvent;


    nameProtectValueChange:ClickGuiValueChangeEvent;
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
    components: Component[];
}

export interface ClientPlayerDataEvent {
    playerData: PlayerData;
}

export interface OverlayMessageEvent {
    text: TextComponent | string;
    tinted: boolean;
}

export interface ConnectionDetailsEvent {

    result: TextComponent | string;

}

export interface OverlayTitleEvent {
    title: TextComponent | string;
    subtitle: TextComponent | string;
}

export interface PlayerEntry {
    name: TextComponent | string;
    uuid: string;
    latency: TextComponent | string;
    isFriend: boolean;
    isStaff: boolean;
}

export interface OverlayPlayListEvent {
    header: TextComponent | string;
    footer: TextComponent | string;
    players: PlayerEntry[];

}

export interface OverlayChatEvent {
    content: TextComponent | string,
    timestamp: number;
    isSystem: Boolean;
    id: number
    visible: boolean
    fadeTimeout?: number;
}

export interface OverlayDisconnectionEvent  {
    parent: "title" | "menu" | "custom";
    info: TextComponent | string;
}
export interface ChatReceiveEvent {
    message: string;
    textData: TextComponent | string;
    type: ChatType;
    applyChatDecoration: (text: TextComponent | string) => TextComponent | string;
    cancelled?: boolean;
}

export enum ChatType {
    CHAT_MESSAGE = "ChatMessage",
    DISGUISED_CHAT_MESSAGE = "DisguisedChatMessage",
    GAME_MESSAGE = "GameMessage"
}



export interface NotificationEvent {
    title: string;
    message: string;
    severity: "INFO" | "SUCCESS" | "ERROR" | "ENABLED" | "DISABLED" | "BLINK" | "BLINKED" | "BLINKING";
}

export interface ProgressEvent {
    title: string;
    progress: number;
    maxProgress: number;
    timeRemaining: number;
}

export interface KeyEvent {
    key: string;
    action: number;
    mods: number;
}

export interface TargetChangeEvent {
    target: PlayerData | null;
    distant:number;
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
    openChest: ItemStack[];
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
export interface SessionEvent {
    session: Session;
}
