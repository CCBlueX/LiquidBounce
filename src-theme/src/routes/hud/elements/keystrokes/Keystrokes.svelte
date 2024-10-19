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
    let keyAttack: MinecraftKeybind | undefined;
    let keyUse: MinecraftKeybind | undefined;

    async function updateKeybinds() {
        const keybinds = await getMinecraftKeybinds();

        keyForward = keybinds.find(k => k.bindName === "key.forward");
        keyBack = keybinds.find(k => k.bindName === "key.back");
        keyLeft = keybinds.find(k => k.bindName === "key.left");
        keyRight = keybinds.find(k => k.bindName === "key.right");
        keyJump = keybinds.find(k => k.bindName === "key.jump");
        keyAttack = keybinds.find(k => k.bindName === "key.attack");
        keyUse = keybinds.find(k => k.bindName === "key.use");
    }

    onMount(updateKeybinds);

    listen("keybindChange", updateKeybinds)
</script>

<div class="keystrokes">
    <div class="nil"></div>
    <Key key={keyForward}/>
    <div class="nil"></div>
    <Key key={keyLeft}/>
    <Key key={keyBack}/>
    <Key key={keyRight}/>
    <Key key={keyJump} flexBasis="100%"/>
    <Key key={keyAttack} flexBasis="calc(50% - 2.5px)" showCPS/>
    <Key key={keyUse} flexBasis="calc(50% - 2.5px)" showCPS/>
</div>

<style lang="scss">
  .keystrokes {
    display: flex;
    flex-wrap: wrap;
    width: calc(50px * 3 + 5px * 2);
    gap: 5px;
  }

  .nil {
    width: 50px;
  }
</style>
