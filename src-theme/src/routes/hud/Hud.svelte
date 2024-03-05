<script lang="ts">
    import ArrayList from "./elements/ArrayList.svelte";
    import TargetHud from "./elements/targethud/TargetHud.svelte";
    import Watermark from "./elements/Watermark.svelte";
    import Notifications from "./elements/notifications/Notifications.svelte";
    import TabGui from "./elements/tabgui/TabGui.svelte";
    import HotBar from "./elements/hotbar/HotBar.svelte";
    import Scoreboard from "./elements/Scoreboard.svelte";
    import {onMount} from "svelte";
    import {getGameWindow} from "../../integration/rest";

    let zoom = 100;

    onMount(async () => {
        const gameWindow = await getGameWindow();

        zoom = gameWindow.scaleFactor * 50;
    });
</script>

<div class="hud" style="zoom: {zoom}%">
    <Watermark />
    <TabGui />
    <ArrayList />
    <Notifications />
    <TargetHud />
    <HotBar />
    <Scoreboard />
</div>

<style lang="scss">
    .hud {
        height: 100vh;
        width: 100vw;
    }
</style>
