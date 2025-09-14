<script lang="ts">
    import {onDestroy, onMount} from "svelte";
    import ArrayList from "./elements/arrayList/ArrayList.svelte";
    import Notifications from "./elements/notifications/Notifications.svelte";
    import TabGui from "./elements/tabgui/TabGui.svelte";
    import HealthBar from "./elements/HealthBar.svelte";
    import Scoreboard from "./elements/Scoreboard.svelte";
    import Watermark from "./elements/watermark/Watermark.svelte";
    import Logo from "./elements/Logo.svelte";
    import Information from "./elements/Information.svelte";
    import ItemColumnHUD from "./elements/inventory/ItemColumnHUD.svelte";
    import HotBar from "./elements/hotbar/HotBar.svelte";
    import Keystrokes from "./elements/keystrokes/Keystrokes.svelte";
    import BlockCounter from "./elements/BlockCounter.svelte";
    import ArmorItems from "./elements/inventory/ArmorItems.svelte";
    import InventoryContainer from "./elements/inventory/InventoryContainer.svelte";
    import CraftingInput from "./elements/inventory/CraftingInput.svelte";
    import Text from "./elements/Text.svelte";
    import Island from "./elements/island/Island.svelte";
    import StatusBar from "./elements/statusBar/StatusBar.svelte";
    import Message from "./elements/Message.svelte";
    import KeyBinds from "./elements/KeyBinds.svelte";
    import MotionGraph from "./elements/MotionGraph.svelte";
    import TitleControl from "./elements/TitleControl.svelte";
    import SessionInfo from "./elements/sessioninfo/SessionInfo.svelte";
    import PlayerList from "./elements/PlayerList.svelte";
    import ChatHUD from "./elements/chat/Chat.svelte";
    import type {Component, ConfigurableSetting, Metadata, TogglableSetting} from "../../integration/types";
    import type {ClickGuiValueChangeEvent, ComponentsUpdateEvent} from "../../integration/events";
    import {getComponents, getMetadata, getModuleSettings} from "../../integration/rest";
    import {listen} from "../../integration/ws";
    import Vignette from "./elements/Vignette.svelte";
    import {gridSize, snappingEnabled, showGrid,ScaleFactor} from "./Hud_store";
    import {WindowSize} from "../../util/WindowSize";
    import {hudScaleFactor} from "../../util/Theme/ThemeManager";
    import {calcResolutionCoefficient} from "../../util/ResolutionScaler";
    import ProgressBar from "./elements/progressBar/ProgressBar.svelte";
    import SilentHand from "./elements/SilentHand.svelte";
    import TargetHud from "./elements/targethud/TargetHud.svelte";
    import Effects from "./elements/effects/Effects.svelte";
    import DraggableComponent from "./elements/DraggableComponent.svelte";

    const {width, height, destroy} = WindowSize();
    let metadata: Metadata;
    let components: Component[] = [];

    $: ScaleFactor.set($hudScaleFactor * calcResolutionCoefficient());

    /*
    type ComponentWrapperParams = {
        component: Component;
    };
    */
    const applyValues = (configurable: ConfigurableSetting) => {

        const snappingValue = configurable.value.find(v => v.name === "Snapping") as TogglableSetting;
        $snappingEnabled = snappingValue?.value.find(v => v.name === "Enabled")?.value as boolean ?? true;
        $gridSize = snappingValue?.value.find(v => v.name === "GridSize")?.value as number ?? 10;

    };

    async function updateZoom(): Promise<void> {
        $ScaleFactor = $hudScaleFactor  * calcResolutionCoefficient();
    }

    async function preloadComponents() {
        metadata = await getMetadata();
        components = await getComponents(metadata.id);

        for (const component of components) {
            const key = `hud-pos-${component.name.toLowerCase()}`;
            if (!localStorage.getItem(key)) {

                localStorage.setItem(key, JSON.stringify({
                    x: component.settings.x ?? 0,
                    y: component.settings.y ?? 0,
                }));
            }
        }

        components = components ;
    }


    onMount(() => {
        const cleanup = () => window.removeEventListener("resize", updateZoom);

        (async () => {
            await updateZoom();
            const clickGuiSettings = await getModuleSettings("HudLayoutEditor");
            applyValues(clickGuiSettings);
            await preloadComponents();
            window.addEventListener("resize", updateZoom);
        })();

        return cleanup;
    });

    onDestroy(destroy);

    listen("componentsUpdate", (data: ComponentsUpdateEvent) => {
        if (data.id != metadata.id) {
            // reject
            return;
        }

        // force update to re-render
        components = [];
        components = data.components;
    });
    listen("hudLayoutEditorValueChange", (e: ClickGuiValueChangeEvent) => {
        applyValues(e.configurable);
    });
</script>
<div class="snap" class:grid={$showGrid}
     style="background-size: {$gridSize}px {$gridSize}px;width: { $width }px; height: { $height }px;">
    <div class="hud" style="--hud-zoom: {$ScaleFactor}">
        {#each components as c (c.id)}
            {#if c.settings.enabled}
                <DraggableComponent name={c.name} id={c.id} alignment={c.settings.alignment}>
                    {#if c.name === 'Text'}
                        <Text settings={c.settings}/>
                    {:else if c.name === 'Hotbar'}
                        <HotBar settings={c.settings}/>
                    {:else if c.name === 'Effects'}
                        <Effects settings={c.settings}/>
                    {:else if c.name === 'Image'}
                        <img alt="" src={c.settings.uRL} style="scale: {c.settings.scale};"/>
                    {:else if c.name === 'ArmorItems'}
                        <ArmorItems settings={c.settings}/>
                    {:else if c.name === 'ArrayList'}
                        <ArrayList/>
                    {:else if c.name === 'BlockCounter'}
                        <BlockCounter/>
                    {:else if c.name === 'ChatHUD'}
                        <ChatHUD/>
                    {:else if c.name === "CraftingInventory"}
                        <CraftingInput/>
                    {:else if c.name === 'HealthBar'}
                        <HealthBar/>
                    {:else if c.name === 'Information'}
                        <Information settings={c.settings}/>
                    {:else if c.name === 'Inventory'}
                        <InventoryContainer settings={c.settings}/>
                    {:else if c.name === 'Island'}
                        <Island/>
                    {:else if c.name === 'ItemColumnHUD'}
                        <ItemColumnHUD/>
                    {:else if c.name === 'KeyBinds'}
                        <KeyBinds settings={c.settings}/>
                    {:else if c.name === 'Keystrokes'}
                        <Keystrokes settings={c.settings}/>
                    {:else if c.name === 'Logo'}
                        <Logo  settings={c.settings}/>
                    {:else if c.name === 'Message'}
                        <Message/>
                    {:else if c.name === 'MotionGraph'}
                        <MotionGraph/>
                    {:else if c.name === 'Notifications'}
                        <Notifications/>
                    {:else if c.name === 'ProgressBar'}
                        <ProgressBar/>
                    {:else if c.name === 'PlayerList'}
                        <PlayerList settings={c.settings}/>
                    {:else if c.name === 'Scoreboard'}
                        <Scoreboard/>
                    {:else if c.name === 'SessionInfo'}
                        <SessionInfo settings={c.settings}/>
                    {:else if c.name === 'SilentHand'}
                        <SilentHand/>
                    {:else if c.name === 'StatusBar'}
                        <StatusBar/>
                    {:else if c.name === 'TabGui'}
                        <TabGui/>
                    {:else if c.name === 'TargetHud'}
                        <TargetHud settings={c.settings}/>
                    {:else if c.name === 'TitleControl'}
                        <TitleControl/>
                    {:else if c.name === 'Watermark'}
                        <Watermark settings={c.settings}/>
                    {/if}
                </DraggableComponent>
            {/if}
        {/each}
    </div>
</div>
<Vignette/>



<style lang="scss">
  @use "../../colors" as *;

  $GRID_SIZE: 10px;
  .hud {
    height: 100vh;
    width: 100vw;
    zoom: var(--hud-zoom);
  }

  .snap {
    overflow: hidden;
    position: absolute;
    will-change: opacity;
    top: 0;
    left: 0;
    transform-origin: left top;

    &.grid {
      background-image: linear-gradient(to right, rgba($clickgui-grid-color, 0.3) 1px, transparent 1px),
      linear-gradient(to bottom, rgba($clickgui-grid-color, 0.3) 1px, transparent 1px);
      background-size: #{$GRID_SIZE}px #{$GRID_SIZE}px;
    }
  }
</style>
