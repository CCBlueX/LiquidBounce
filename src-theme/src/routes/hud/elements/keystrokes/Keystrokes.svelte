<script lang="ts">
    import Key from "./Key.svelte";
    import {onMount} from "svelte";
    import {getMinecraftKeybinds} from "../../../../integration/rest";
    import type {MinecraftKeybind} from "../../../../integration/types";
    import {listen} from "../../../../integration/ws";

    export let settings: { [name: string]: any };

    onMount(() => {
        console.log(settings);
    })

    let keyForward: MinecraftKeybind | undefined;
    let keyBack: MinecraftKeybind | undefined;
    let keyLeft: MinecraftKeybind | undefined;
    let keyRight: MinecraftKeybind | undefined;
    let keyJump: MinecraftKeybind | undefined;
    let keyAttack: MinecraftKeybind | undefined;
    let keyUse: MinecraftKeybind | undefined;
    let keySprint: MinecraftKeybind | undefined;
    let keySneak: MinecraftKeybind | undefined;

    async function updateKeybinds() {
        const keybinds = await getMinecraftKeybinds();

        keyForward = keybinds.find(k => k.bindName === "key.forward");
        keyBack = keybinds.find(k => k.bindName === "key.back");
        keyLeft = keybinds.find(k => k.bindName === "key.left");
        keyRight = keybinds.find(k => k.bindName === "key.right");
        keyJump = keybinds.find(k => k.bindName === "key.jump");
        keyAttack = keybinds.find(k => k.bindName === "key.attack");
        keyUse = keybinds.find(k => k.bindName === "key.use");
        keySprint = keybinds.find(k => k.bindName === "key.sprint");
        keySneak = keybinds.find(k => k.bindName === "key.sneak");
    }

    onMount(updateKeybinds);

    listen("keybindChange", updateKeybinds)
</script>

<div class="keystrokes">
    <div class="nil"></div>
    <Key key={keyForward} showName/>
    <div class="nil"></div>
    <Key key={keyLeft} showName/>
    <Key key={keyBack} showName/>
    <Key key={keyRight} showName/>
    {#if settings.showJumpKey}
        <Key key={keyJump} flexBasis="100%" showName/>
    {/if}
    {#if settings.showAttackAndUseKey}
        <Key key={keyAttack} flexBasis="calc(50% - 2.5px)" showCPS/>
        <Key key={keyUse} flexBasis="calc(50% - 2.5px)" showCPS/>
    {/if}
    {#if settings.showSprintAndSneakKey}
        <Key key={keySprint} flexBasis="calc(50% - 2.5px)" showName/>
        <Key key={keySneak} flexBasis="calc(50% - 2.5px)" showName/>
    {/if}
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
