<script lang="ts">
    import ArrayList from "./elements/ArrayList.svelte";
    import TargetHud from "./elements/targethud/TargetHud.svelte";
    import Watermark from "./elements/Watermark.svelte";
    import Notifications from "./elements/notifications/Notifications.svelte";
    import TabGui from "./elements/tabgui/TabGui.svelte";
    import HotBar from "./elements/hotbar/HotBar.svelte";
    import Scoreboard from "./elements/Scoreboard.svelte";
    import {onMount} from "svelte";
    import {getClientInfo, getComponents, getGameWindow, getMetadata} from "../../integration/rest";
    import {listen} from "../../integration/ws";
    import type {HudComponent, Metadata} from "../../integration/types";
    import Taco from "./elements/taco/Taco.svelte";
    import type {ComponentsUpdateEvent, ScaleFactorChangeEvent} from "../../integration/events";
    import Keystrokes from "./elements/keystrokes/Keystrokes.svelte";
    import Effects from "./elements/Effects.svelte";
    import BlockCounter from "./elements/BlockCounter.svelte";
    import Text from "./elements/Text.svelte";
    import DraggableComponent from "./elements/DraggableComponent.svelte";
    import KeyBinds from "./elements/KeyBinds.svelte";
    import GenericPlayerInventory from "./elements/inventory/GenericPlayerInventory.svelte";
    import {os} from "../clickgui/clickgui_store";

    let zoom = 100;
    let metadata: Metadata;
    let components: HudComponent[] = [];

    onMount(async () => {
        $os = (await getClientInfo()).os;

        const gameWindow = await getGameWindow();
        zoom = gameWindow.scaleFactor * 50;

        metadata = await getMetadata();
        components = await getComponents(metadata.id);
    });

    listen("scaleFactorChange", (data: ScaleFactorChangeEvent) => {
        zoom = data.scaleFactor * 50;
    });

    listen("componentsUpdate", (data: ComponentsUpdateEvent) => {
        if (data.id != metadata.id) {
            // reject
            return;
        }

        // force update to re-render
        components = [];
        components = data.components;
    });
</script>

<div class="hud" style="zoom: {zoom}%">
    {#each components as c}
        {#if c.settings.enabled}
            <DraggableComponent alignment={c.settings.alignment} >
                {#if c.name === "Watermark"}
                    <Watermark/>
                {:else if c.name === "ArrayList"}
                    <ArrayList settings={c.settings}/>
                {:else if c.name === "TabGui"}
                    <TabGui/>
                {:else if c.name === "Notifications"}
                    <Notifications/>
                {:else if c.name === "TargetHud"}
                    <TargetHud/>
                {:else if c.name === "BlockCounter"}
                    <BlockCounter settings={c.settings}/>
                {:else if c.name === "Hotbar"}
                    <HotBar/>
                {:else if c.name === "Scoreboard"}
                    <Scoreboard settings={c.settings}/>
                {:else if c.name === "ArmorItems"}
                    <GenericPlayerInventory
                            rowLength={1}
                            backgroundColor="transparent"
                            gap="2px"
                            getRenderedStacks={it => Array.from(it.armor).reverse()}
                    />
                {:else if c.name === "Inventory"}
                    <GenericPlayerInventory rowLength={9} getRenderedStacks={it => it.main.slice(9)} />
                {:else if c.name === "CraftingInventory"}
                    <GenericPlayerInventory rowLength={2} getRenderedStacks={it => it.crafting} />
                {:else if c.name === "EnderChestInventory"}
                    <GenericPlayerInventory rowLength={9} getRenderedStacks={it => it.enderChest} />
                {:else if c.name === "Taco"}
                    <Taco/>
                {:else if c.name === "Keystrokes"}
                    <Keystrokes/>
                {:else if c.name === "Effects"}
                    <Effects/>
                {:else if c.name === "Text"}
                    <Text settings={c.settings} />
                {:else if c.name === "Image"}
                    <img alt="" src="{c.settings.uRL}" style="scale: {c.settings.scale};">
                {:else if c.name === "KeyBinds"}
                    <KeyBinds/>
                {/if}
            </DraggableComponent>
        {/if}
    {/each}
</div>

<style lang="scss">
  .hud {
    height: 100vh;
    width: 100vw;
  }
</style>
