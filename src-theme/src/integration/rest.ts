import {REST_BASE} from "./host";
import type {
    Account,
    Browser,
    ClientInfo,
    ClientUpdate,
    Component,
    ConfigurableSetting,
    GameWindow,
    GeneratorResult,
    HitResult,
    MinecraftKeybind,
    RegistryItem,
    Module,
    PersistentStorageItem,
    PlayerData,
    PrintableKey,
    Protocol,
    Proxy,
    Server,
    Session,
    VirtualScreen,
    World
} from "./types";
import type {PlayerInventory} from "./events";
import {isLoggingIn} from "../routes/menu/altmanager/altmanager_store";

const API_BASE = `${REST_BASE}/api/v1`;

export async function getModules(): Promise<Module[]> {
    const response = await fetch(`${API_BASE}/client/modules`);
    const data: [Module] = await response.json();

    return data;
}

export async function getModule(name: string): Promise<Module> {
    const response = await fetch(`${API_BASE}/client/module/${name}`);
    const data = await response.json();

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

export async function getSpooferSettings(): Promise<ConfigurableSetting> {
    const response = await fetch(`${API_BASE}/client/spoofer`);
    const data = await response.json();

    return data;
}

export async function setSpooferSettings(settings: ConfigurableSetting) {
    await fetch(`${API_BASE}/client/spoofer`, {
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

export async function getPlayerInventory(): Promise<PlayerInventory> {
    const response = await fetch(`${API_BASE}/client/player/inventory`);
    const data: PlayerInventory = await response.json();

    return data;
}

export async function getCrosshairData(): Promise<HitResult> {
    const response = await fetch(`${API_BASE}/client/crosshair`);
    const data: HitResult = await response.json();

    return data;
}

export async function getPrintableKeyName(key: string): Promise<PrintableKey> {
    const searchParams = new URLSearchParams({key});

    const response = await fetch(`${API_BASE}/client/input?${searchParams.toString()}`);
    const data: PrintableKey = await response.json();

    return data;
}

export async function getMinecraftKeybinds(): Promise<MinecraftKeybind[]> {
    const response = await fetch(`${API_BASE}/client/keybinds`);
    const data: MinecraftKeybind[] = await response.json();

    return data;
}

export async function getRegistryItems(name: string): Promise<Record<string, RegistryItem>> {
    const response = await fetch(`${API_BASE}/client/registry/${name}`);
    const data: Record<string, RegistryItem> = await response.json();

    return data;
}

export async function getSession(): Promise<Session> {
    const response = await fetch(`${API_BASE}/client/session`);
    const data: Session = await response.json();

    return data;
}

export async function browse(target: string) {
    await fetch(`${API_BASE}/client/browse`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({target})
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

export async function deleteScreen() {
    await fetch(`${API_BASE}/client/screen`, {
        method: "DELETE"
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

export async function removeServer(id: number) {
    await fetch(`${API_BASE}/client/servers/remove`, {
        method: "DELETE",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({id})
    });
}

export async function addServer(name: string, address: string, serverResourcePacks: string) {
    await fetch(`${API_BASE}/client/servers/add`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({name, address, serverResourcePacks})
    });
}

export async function editServer(id: number, name: string, address: string, resourcePackPolicy: string) {
    await fetch(`${API_BASE}/client/servers/edit`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({id, name, address, resourcePackPolicy})
    });
}

export async function orderServers(order: number[]) {
    await fetch(`${API_BASE}/client/servers/order`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({order})
    });
}

export async function getProtocols(): Promise<Protocol[]> {
    const response = await fetch(`${API_BASE}/client/protocols`);
    const data: Protocol[] = await response.json();

    return data;
}

export async function getSelectedProtocol(): Promise<Protocol> {
    const response = await fetch(`${API_BASE}/client/protocols/protocol`);
    const data: Protocol = await response.json();

    return data;
}

export async function setSelectedProtocol(protocol: Protocol) {
    await fetch(`${API_BASE}/client/protocols/protocol`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({version: protocol.version})
    });
}

export async function restoreSession() {
    await fetch(`${API_BASE}/client/account/restore`, {
        method: "POST",
    });
}

export async function orderAccounts(order: number[]) {
    await fetch(`${API_BASE}/client/accounts/order`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({order})
    });
}


export async function addCrackedAccount(username: string, online: boolean) {
    await fetch(`${API_BASE}/client/accounts/new/cracked`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({username, online})
    });
}

export async function addSessionAccount(token: string) {
    await fetch(`${API_BASE}/client/accounts/new/session`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({token})
    });
}

export async function addAlteningAccount(token: string) {
    await fetch(`${API_BASE}/client/accounts/new/altening`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({token})
    });
}

export async function addMicrosoftAccount() {
    await fetch(`${API_BASE}/client/accounts/new/microsoft`, {
        method: "POST",
    });
}

export async function addMicrosoftAccountCopyUrl() {
    await fetch(`${API_BASE}/client/accounts/new/microsoft/clipboard`, {
        method: "POST",
    });
}

export async function setAccountFavorite(id: number, favorite: boolean) {
    if (favorite) {
        await fetch(`${API_BASE}/client/account/favorite`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({id})
        });
    } else {
        await fetch(`${API_BASE}/client/account/favorite`, {
            method: "DELETE",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({id})
        });
    }
}

export async function removeAccount(id: number) {
    await fetch(`${API_BASE}/client/account`, {
        method: "DELETE",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({id})
    });
}

export async function loginToAccount(id: number) {
    isLoggingIn.set(true);
    await fetch(`${API_BASE}/client/account/login`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({id})
    }).finally(() => {
        isLoggingIn.set(false);
    });
}

export async function directLoginToCrackedAccount(username: string, online: boolean) {
    await fetch(`${API_BASE}/client/account/login/cracked`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({username, online})
    });
}

export async function directLoginToSessionAccount(token: string) {
    await fetch(`${API_BASE}/client/account/login/session`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({token})
    });
}

export async function getAccounts(): Promise<Account[]> {
    const response = await fetch(`${API_BASE}/client/accounts`);
    const data: Account[] = await response.json();

    return data;
}

export async function getWorlds(): Promise<World[]> {
    const response = await fetch(`${API_BASE}/client/worlds`);
    const data: World[] = await response.json();

    return data;
}

export async function openWorld(name: string) {
    const response = await fetch(`${API_BASE}/client/worlds/join`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({name})
    });
}

export async function editWorld(name: string) {
    const response = await fetch(`${API_BASE}/client/worlds/edit`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({name})
    });
}

export async function removeWorld(name: string) {
    const response = await fetch(`${API_BASE}/client/worlds/delete`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({name})
    });
}

export async function getProxies(): Promise<Proxy[]> {
    const response = await fetch(`${API_BASE}/client/proxies`);
    const data: Proxy[] = await response.json();

    return data;
}

export async function checkProxy(id: number) {
    await fetch(`${API_BASE}/client/proxies/check`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({id})
    });
}

export async function getCurrentProxy(): Promise<Proxy> {
    const response = await fetch(`${API_BASE}/client/proxy`);
    const data: Proxy = await response.json();

    return data;
}

export async function disconnectFromProxy() {
    await fetch(`${API_BASE}/client/proxy`, {
        method: "DELETE",
    });
}

export async function setProxyFavorite(id: number, favorite: boolean) {
    if (favorite) {
        await fetch(`${API_BASE}/client/proxies/favorite`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({id})
        });
    } else {
        await fetch(`${API_BASE}/client/proxies/favorite`, {
            method: "DELETE",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({id})
        });
    }
}

export async function addProxy(host: string, port: number, username: string, password: string, type: string, forwardAuthentication: boolean) {
    await fetch(`${API_BASE}/client/proxies/add`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({host, port, username, password, type, forwardAuthentication})
    });
}

export async function editProxy(id: number, host: string, port: number, username: string, password: string, type: string, forwardAuthentication: boolean) {
    await fetch(`${API_BASE}/client/proxies/edit`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({id, host, port, username, password, type, forwardAuthentication})
    })
}

export async function addProxyFromClipboard() {
    await fetch(`${API_BASE}/client/proxies/add/clipboard`, {
        method: "POST"
    });
}

export async function removeProxy(id: number) {
    await fetch(`${API_BASE}/client/proxies/remove`, {
        method: "DELETE",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({id})
    });
}

export async function connectToProxy(id: number) {
    await fetch(`${API_BASE}/client/proxy`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({id})
    });
}

export async function getGameWindow(): Promise<GameWindow> {
    const response = await fetch(`${API_BASE}/client/window`);
    const data: GameWindow = await response.json();

    return data;
}

export async function getComponents(): Promise<Component[]> {
    const response = await fetch(`${API_BASE}/client/components`);
    return await response.json();
}

export async function getClientInfo(): Promise<ClientInfo> {
    const response = await fetch(`${API_BASE}/client/info`);
    const data: ClientInfo = await response.json();

    return data;
}

export async function getClientUpdate(): Promise<ClientUpdate> {
    const response = await fetch(`${API_BASE}/client/update`);
    const data: ClientUpdate = await response.json();

    return data;
}

export async function reconnectToServer() {
    await fetch(`${API_BASE}/client/reconnect`, {
        method: "POST",
    });
}

export async function toggleBackgroundShaderEnabled() {
    await fetch(`${API_BASE}/client/shader`, {
        method: "POST",
    });
}

export async function getBrowser(): Promise<Browser> {
    const response = await fetch(`${API_BASE}/client/browser`);
    const data: Browser = await response.json();

    return data;
}

export async function browserNavigate(url: string) {
    await fetch(`${API_BASE}/client/browser/navigate`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({url})
    })
}

export async function browserGoForward() {
    await fetch(`${API_BASE}/client/browser/forward`, {
        method: "POST",
    });
}

export async function browserGoBack() {
    await fetch(`${API_BASE}/client/browser/back`, {
        method: "POST",
    });
}

export async function browserReload() {
    await fetch(`${API_BASE}/client/browser/reload`, {
        method: "POST",
    });
}

export async function browserForceReload() {
    await fetch(`${API_BASE}/client/browser/forceReload`, {
        method: "POST",
    });
}

export async function browserClose() {
    await fetch(`${API_BASE}/client/browser/close`, {
        method: "POST",
    });
}

export async function randomUsername(): Promise<string> {
    let response = await fetch(`${API_BASE}/client/account/random-name`, {
        method: "POST",
    });
    let data: GeneratorResult = await response.json();

    return data.name;
}

export async function setTyping(typing: boolean) {
    await fetch(`${API_BASE}/client/typing`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({typing})
    });
}
