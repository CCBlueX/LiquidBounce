<script lang="ts">
    import Key from "./Key.svelte";
    import {onMount} from "svelte";
    import {getMinecraftKeybinds} from "../../../../integration/rest";
    import type {MinecraftKeybind} from "../../../../integration/types";
    import {listen} from "../../../../integration/ws";

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

    listen("keybindChange", updateKeybinds)
</script>

<div class="keystrokes">
    <Key key={keyForward} gridArea="a" />
    <Key key={keyLeft} gridArea="b" />
    <Key key={keyBack} gridArea="c" />
    <Key key={keyRight} gridArea="d" />
    <Key key={keyJump} gridArea="e" />
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