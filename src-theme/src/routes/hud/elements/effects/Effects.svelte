<script lang="ts">
    import Modern_Effects from "./mode/Modern_Effects.svelte";
    import Simple_Effects from "./mode/Simple_Effects.svelte";
    import Hanabi_Effects from "./mode/Hanabi_Effects.svelte";
    import {forcedEnglish} from "./Effects";

    export let settings: { [name: string]: any };

    const modes: Record<string, any> = {
        Modern: Modern_Effects,
        Simple: Simple_Effects,
        Hanabi: Hanabi_Effects,
    };

    let ModeComponent: any;
    $: forcedEnglish.set(settings?.forcedenglish ?? false);
    $: ModeComponent = modes[settings?.mode] ?? Simple_Effects;
</script>

<svelte:component this={ModeComponent} {settings} />
