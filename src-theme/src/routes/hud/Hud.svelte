<script lang="ts">
    import ArrayList from "./elements/ArrayList.svelte";
    import TargetHud from "./elements/targethud/TargetHud.svelte";
    import Watermark from "./elements/Watermark.svelte";
    import Notifications from "./elements/notifications/Notifications.svelte";
    import TabGui from "./elements/tabgui/TabGui.svelte";
    import HotBar from "./elements/hotbar/HotBar.svelte";
    import Scoreboard from "./elements/Scoreboard.svelte";
    import {onMount, setContext} from "svelte";
    import {
        getClientInfo,
        getComponents,
        getGameWindow,
        getMetadata,
        getNativeComponents
    } from "../../integration/rest";
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
    import InventoryStatistics from "./elements/inventory/InventoryStatistics.svelte";
    import {
        HUD_EDITOR_ELEMENTS_CONTEXT,
        type HudEditorDragState
    } from "../clickgui/tabs/hud_editor/constants";
    import Image from "./elements/Image.svelte";

    export let inEditor = false;
    export let onDragStateChange: ((state: HudEditorDragState) => void) | undefined = undefined;
    export let magneticTargetIds: string[] = [];

    let zoom = 100;
    let metadata: Metadata;
    let nativeComponents: HudComponent[] = [];
    let themeComponents: HudComponent[] = [];

    $: renderedComponents = inEditor ? [...nativeComponents, ...themeComponents] : themeComponents;

    setContext(HUD_EDITOR_ELEMENTS_CONTEXT, new Map<string, HTMLElement>());

    onMount(async () => {
        $os = (await getClientInfo()).os;

        const gameWindow = await getGameWindow();
        zoom = gameWindow.scaleFactor * 50;

        metadata = await getMetadata();
        [nativeComponents, themeComponents] = await Promise.all([
            inEditor ? getNativeComponents() : Promise.resolve([]),
            getComponents(metadata.id)
        ]);
    });

    listen("scaleFactorChange", (data: ScaleFactorChangeEvent) => {
        zoom = data.scaleFactor * 50;
    });

    listen("componentsUpdate", (event: ComponentsUpdateEvent) => {
        if (inEditor && event.source === "native") {
            nativeComponents = event.components;
        }

        if (event.source === "theme" && event.themeId === metadata?.id) {
            themeComponents = event.components;
        }
    });
</script>

<div class="hud" style="zoom: {zoom}%">
    {#each renderedComponents as c (c.id)}
        {#if c.settings.enabled}
            <DraggableComponent
                    {inEditor}
                    {onDragStateChange}
                    componentId={c.id}
                    componentName={c.name}
                    alignment={c.settings.alignment}
                    zIndex={c.settings.zIndex ?? 0}
                    magneticallyReferenced={magneticTargetIds.includes(c.id)}
                    width={c.width}
                    height={c.height}
            >
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
                            rowLength={c.settings.layout === "Horizontal" ? 4 : 1}
                            backgroundColor="transparent"
                            gap="2px"
                            getRenderedStacks={it => Array.from(it.armor).reverse()}
                    />
                {:else if c.name === "InventoryStatistics"}
                    <InventoryStatistics settings={c.settings}/>
                {:else if c.name === "Inventory"}
                    <GenericPlayerInventory rowLength={9} getRenderedStacks={it => it.main.slice(9)}/>
                {:else if c.name === "CraftingInventory"}
                    <GenericPlayerInventory rowLength={2} getRenderedStacks={it => it.crafting}/>
                {:else if c.name === "EnderChestInventory"}
                    <GenericPlayerInventory rowLength={9} getRenderedStacks={it => it.enderChest}/>
                {:else if c.name === "Taco"}
                    <Taco/>
                {:else if c.name === "Keystrokes"}
                    <Keystrokes/>
                {:else if c.name === "Effects"}
                    <Effects/>
                {:else if c.name === "Text"}
                    <Text settings={c.settings}/>
                {:else if c.name === "Image"}
                    <Image settings={c.settings}/>
                {:else if c.name === "KeyBinds"}
                    <KeyBinds/>
                {:else if c.width !== undefined && c.height !== undefined}
                    <div></div>
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
