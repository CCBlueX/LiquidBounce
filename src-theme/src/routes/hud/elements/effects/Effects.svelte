<script lang="ts">
    import Modern_Effects from "./mode/Modern_Effects.svelte";
    import Simple_Effects from "./mode/Simple_Effects.svelte";

    export let settings: { [name: string]: any };

    const modes: Record<string, any> = {
        Modern: Modern_Effects,
        Simple: Simple_Effects
    };

    let ModeComponent: any;

    $: ModeComponent = modes[settings?.mode] ?? Simple_Effects;
</script>

<svelte:component this={ModeComponent} {settings} />
