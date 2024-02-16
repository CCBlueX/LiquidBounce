<script>
    import ArrayList from "./elements/ArrayList.svelte";
    import TargetHud from "./elements/targethud/TargetHud.svelte";
    import Watermark from "./elements/Watermark.svelte";
    import Notifications from "./elements/notifications/Notifications.svelte";
    import TabGui from "./elements/tabgui/TabGui.svelte";
    import HotBar from "./elements/hotbar/HotBar.svelte";
</script>

<div class="hud">
    <Watermark />
    <TabGui />
    <ArrayList />
    <Notifications />
<!--     <TargetHud />
    <HotBar /> -->
</div>

<style lang="scss">
    .hud {
        height: 100vh;
        width: 100vw;
    }
</style>
