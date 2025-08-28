<script lang="ts">
    import Key from "./Key.svelte";
    import {onMount} from "svelte";
    import {getMinecraftKeybinds} from "../../../../integration/rest";
    import type {MinecraftKeybind} from "../../../../integration/types";
    import {listen} from "../../../../integration/ws";
    import {expoInOut} from "svelte/easing";
    import {fly} from "svelte/transition";
    import type {EventMap} from "../../../../integration/events";

    let keyForward: MinecraftKeybind | undefined;
    let keyBack: MinecraftKeybind | undefined;
    let keyLeft: MinecraftKeybind | undefined;
    let keyRight: MinecraftKeybind | undefined;
    let keyJump: MinecraftKeybind | undefined;

    async function updateKeybinds() {
        const keybinds = await getMinecraftKeybinds();

        keyForward = keybinds.find(k => k.bindName === "key.forward");
        keyBack = keybinds.find(k => k.bindName === "key.back");
        keyLeft = keybinds.find(k => k.bindName === "key.left");
        keyRight = keybinds.find(k => k.bindName === "key.right");
        keyJump = keybinds.find(k => k.bindName === "key.jump");
    }

    onMount(updateKeybinds);

    listen("keybindChange" as keyof EventMap, updateKeybinds)
</script>

<div class="keystrokes" transition:fly|global={{duration: 500, x: -50, easing: expoInOut}}>
    <Key gridArea="a" key={keyForward}/>
    <Key gridArea="b" key={keyLeft}/>
    <Key gridArea="c" key={keyBack}/>
    <Key gridArea="d" key={keyRight}/>
    <Key asBar={true} gridArea="e" key={keyJump}/>
</div>

<style lang="scss">
  .keystrokes {
    display: grid;
    grid-template-areas:
        ". a ."
        "b c d"
        "e e e";
    grid-template-columns: repeat(3, 50px);
    gap: 5px;
  }
</style>
