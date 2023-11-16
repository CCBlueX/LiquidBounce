declare const client: Client;

type Client = {
    getSessionService: () => SessionService;
    getModuleManager: () => ModuleManager;
    isUpdateAvailable: () => boolean;
};

type SessionService = {
    getUsername: () => string;
    getFaceUrl: () => string;
    getAccountType: () => string;
    getLocation: () => string;
};

type ModuleManager = {
    getCategories: () => string[];
    iterator: () => ModuleIterator;
};

type ModuleIterator = {
    hasNext: () => boolean;
    next: () => Module;
};

type Module = {
    getCategory: () => Category;
    getName: () => string;
    getEnabled: () => boolean;
};

type Category = {
    getReadableName: () => string;
};

declare const screenWidth: number;
declare const screenHeight: number;
declare const windowWidth: number;
declare const windowHeight: number;

declare const events: Events;

type Events = {
    on: (event: "windowResize", callback: (event: WindowResizeEvent) => void) => void;
}

interface ClientEvent {
}

interface WindowResizeEvent extends ClientEvent {
    getWindow(): number;

    getWidth(): number;

    getHeight(): number;
}

declare const viaVersion: ViaVersion | undefined;

type ViaVersion = {
    getSupportedVersions(): number[];
}
