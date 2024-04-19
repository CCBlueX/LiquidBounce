import type { Component, PlayerData, Proxy, Server, TextComponent } from "./types";

export interface ToggleModuleEvent {
    moduleName: string;
    hidden: boolean;
    enabled: boolean;
}

export interface KeyboardKeyEvent {
    keyCode: number;
    scanCode: number;
    action: number;
    mods: number;
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
    key: {
        code: number;
        name: string;
    };
    action: number;
    mods: number;
}

export interface TargetChangeEvent {
    target: PlayerData | null;
}

export interface AccountManagerAdditionEvent {
    username: string | null;
    error: string | null;
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

export interface ProxyAdditionResultEvent {
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