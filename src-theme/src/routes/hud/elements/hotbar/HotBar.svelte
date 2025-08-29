<script lang="ts">
    import ModernHotBar from "./mode/Modern_HotBar.svelte";
    import RiseHotBar from "./mode/Rise_HotBar.svelte";
    import SimpleHotBar from "./mode/Simple_HotBar.svelte";
    import Hanabi_HotBar from "./mode/Hanabi_HotBar.svelte";
    export let settings: { [name: string]: any };

    const modes: Record<string, any> = {
        Modern: ModernHotBar,
        Rise:RiseHotBar,
        Simple:SimpleHotBar,
        Hanabi:Hanabi_HotBar
    };

    let ModeComponent: any;

    $: ModeComponent = modes[settings?.mode] ?? ModernHotBar;
</script>

<svelte:component this={ModeComponent} {settings} />
