import {REST_BASE} from "./host";
import type {
    ConfigurableSetting,
    Module,
    PersistentStorageItem,
    PlayerData,
    PrintableKey,
    Registries,
    Server,
    Session,
    VirtualScreen
} from "./types";

const API_BASE = `${REST_BASE}/api/v1`;

export async function getModules(): Promise<Module[]> {
    const response = await fetch(`${API_BASE}/client/modules`);
    const data: [Module] = await response.json();

    return data;
}

export async function getModuleSettings(name: string): Promise<ConfigurableSetting> {
    const searchParams = new URLSearchParams({name});

    const response = await fetch(`${API_BASE}/client/modules/settings?${searchParams.toString()}`);
    const data = await response.json();

    return data;
}

export async function setModuleSettings(name: string, settings: ConfigurableSetting) {
    const searchParams = new URLSearchParams({name});

    await fetch(`${API_BASE}/client/modules/settings?${searchParams.toString()}`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(settings)
    });
}

export async function setModuleEnabled(name: string, enabled: boolean) {
    await fetch(`${API_BASE}/client/modules/toggle`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            name,
            enabled
        })
    });
}

export async function getPersistentStorageItems(): Promise<PersistentStorageItem[]> {
    const response = await fetch(`${API_BASE}/client/localStorage/all`);
    const data: PersistentStorageItem[] = (await response.json()).items;

    return data;
}

export async function setPersistentStorageItems(items: PersistentStorageItem[]) {
    await fetch(`${API_BASE}/client/localStorage/all`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({items})
    })
}

export async function getVirtualScreen(): Promise<VirtualScreen> {
    const response = await fetch(`${API_BASE}/client/virtualScreen`);
    const data: VirtualScreen = await response.json();

    return data;
}

export async function confirmVirtualScreen(name: string) {
    await fetch(`${API_BASE}/client/virtualScreen`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({name})
    });
}

export async function getPlayerData(): Promise<PlayerData> {
    const response = await fetch(`${API_BASE}/client/player`);
    const data: PlayerData = await response.json();

    return data;
}

export async function getPrintableKeyName(code: number): Promise<PrintableKey> {
    const searchParams = new URLSearchParams({code: code.toString()});

    const response = await fetch(`${API_BASE}/client/input?${searchParams.toString()}`);
    const data: PrintableKey = await response.json();

    return data;
}

export async function getRegistries(): Promise<Registries> {
    const response = await fetch(`${API_BASE}/client/registries`);
    const data: Registries = await response.json();

    return data;
}

export async function getSession(): Promise<Session> {
    const response = await fetch(`${API_BASE}/client/session`);
    const data: Session = await response.json();

    return data;
}

export async function browse(url: string) {
    await fetch(`${API_BASE}/client/browse`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({url})
    });
}

export async function exitClient() {
    await fetch(`${API_BASE}/client/exit`, {
        method: "POST"
    });
}

export async function openScreen(name: string) {
    await fetch(`${API_BASE}/client/screen`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({name})
    });
}

export async function getServers(): Promise<Server[]> {
    const response = await fetch(`${API_BASE}/client/servers`);
    const data: Server[] = await response.json();

    return data;
}

export async function connectToServer(address: string) {
    await fetch(`${API_BASE}/client/servers/connect`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({address})
    });
}
