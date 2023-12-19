import Splashscreen from "./routes/splashscreen/Splashscreen.svelte";
import Title from './routes/title/Title.svelte';
import Hud from './routes/hud/Hud.svelte';
import ClickGui from "./routes/clickgui/ClickGui.svelte";
import AltManager from "./routes/altmanager/AltManager.svelte";
import ProxyManager from "./routes/proxymanager/ProxyManager.svelte";
import Inventory from "./routes/inventory/Inventory.svelte";

export const routes = {
    '/': Splashscreen,
    '/title': Title,
    '/hud': Hud,
    '/clickgui': ClickGui,
    '/altmanager': AltManager,
    '/proxymanager': ProxyManager,
    '/inventory': Inventory
}
