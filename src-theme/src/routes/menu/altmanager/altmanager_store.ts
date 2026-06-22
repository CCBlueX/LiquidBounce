import {writable} from "svelte/store";
import {listenAlways} from "../../../integration/ws";

export const isLoggingIn = writable(false);

listenAlways("accountManagerLogin", () => isLoggingIn.set(false));
