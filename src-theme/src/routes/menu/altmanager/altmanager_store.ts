import {writable} from "svelte/store";
import {listen} from "../../../integration/ws";

export const isLoggingIn = writable(false);

listen("accountManagerLogin", () => isLoggingIn.set(false));
