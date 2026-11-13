<script lang="ts">
    import CustomProxies from "./CustomProxies.svelte";
    import LiquidProxy from "./liquidproxy/LiquidProxy.svelte";
    import {setItem} from "../../../integration/persistent_storage";

    type View = "liquidproxy" | "custom";

    const VIEW_KEY = "proxymanager_view";

    let view: View = localStorage.getItem(VIEW_KEY) === "custom" ? "custom" : "liquidproxy";

    async function switchView(to: View) {
        view = to;
        await setItem(VIEW_KEY, to);
    }
</script>

{#if view === "liquidproxy"}
    <LiquidProxy on:switchView={() => switchView("custom")}/>
{:else}
    <CustomProxies on:switchView={() => switchView("liquidproxy")}/>
{/if}
