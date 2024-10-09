<script lang="ts">
    import ArrayList from "./elements/ArrayList.svelte";
    import TargetHud from "./elements/targethud/TargetHud.svelte";
    import Watermark from "./elements/Watermark.svelte";
    import Notifications from "./elements/notifications/Notifications.svelte";
    import TabGui from "./elements/tabgui/TabGui.svelte";
    import HotBar from "./elements/hotbar/HotBar.svelte";
    import Scoreboard from "./elements/Scoreboard.svelte";
    import { onMount } from "svelte";
    import {moveComponent, getComponents, getGameWindow} from "../../integration/rest";
    import {listen, listenAlways} from "../../integration/ws";
    import { type AlignmentSetting, type Component, HorizontalAlignment } from "../../integration/types";
    import Taco from "./elements/taco/Taco.svelte";
    import type { ComponentsUpdateEvent, ScaleFactorChangeEvent } from "../../integration/events";
    import Keystrokes from "./elements/keystrokes/Keystrokes.svelte";
    import Effects from "./elements/Effects.svelte";
    import { VerticalAlignment } from "../../integration/types.js";

    let zoom = 100;
    let components: Component[] = [];
    let draggingComponent: Component | null = null;

    let editor = false;
    let startX = 0;
    let startY = 0;

    onMount(async () => {
        const gameWindow = await getGameWindow();
        zoom = gameWindow.scaleFactor * 50;

        components = await getComponents();
    });

    listen("scaleFactorChange", (data: ScaleFactorChangeEvent) => {
        zoom = data.scaleFactor * 50;
    });

    listen("componentsUpdate", async (data: ComponentsUpdateEvent) => {
        // todo: fix this
        // components = data.components;
        components = await getComponents();
    });

    listen("virtualScreen", async (event: any) => {
        editor = event.action == "open" && event.screenName == "editor";
    });

    const startDrag = (component: Component, event: MouseEvent) => {
        draggingComponent = component;
        startX = event.clientX - component.settings.alignment.horizontalOffset;
        startY = event.clientY - component.settings.alignment.verticalOffset;
        document.addEventListener("mousemove", onDrag);
        document.addEventListener("mouseup", stopDrag);
    };

    const onDrag = (event: MouseEvent) => {
        if (draggingComponent) {
            // todo: implement that top and bottom as well as left and right behaves differently

            draggingComponent.settings.alignment = {
                ...draggingComponent.settings.alignment,
                horizontalOffset: event.clientX - startX,
                verticalOffset: event.clientY - startY
            };
            components = [...components];
        }
    };

    // Stop dragging and log the final position
    const stopDrag = async () => {
        if (draggingComponent) {
            await moveComponent(draggingComponent.id, draggingComponent.settings.alignment);
            draggingComponent = null;
            document.removeEventListener("mousemove", onDrag);
            document.removeEventListener("mouseup", stopDrag);
        }
    };

    function toStyle(alignment: AlignmentSetting): string {
        const { horizontal, vertical, horizontalOffset, verticalOffset } = alignment;

        const horizontalStyle = (() => {
            switch (horizontal) {
                case HorizontalAlignment.LEFT:
                    return `left: ${horizontalOffset}px;`;
                case HorizontalAlignment.RIGHT:
                    return `right: ${horizontalOffset}px;`;
                case HorizontalAlignment.CENTER:
                case HorizontalAlignment.CENTER_TRANSLATED:
                    return `left: calc(50% + ${horizontalOffset}px);`;
                default:
                    return '';
            }
        })();

        const verticalStyle = (() => {
            switch (vertical) {
                case VerticalAlignment.TOP:
                    return `top: ${verticalOffset}px;`;
                case VerticalAlignment.BOTTOM:
                    return `bottom: ${verticalOffset}px;`;
                case VerticalAlignment.CENTER:
                case VerticalAlignment.CENTER_TRANSLATED:
                    return `top: calc(50% + ${verticalOffset}px);`;
                default:
                    return '';
            }
        })();

        const transformStyle = `transform: translate(${horizontal === HorizontalAlignment.CENTER_TRANSLATED ? "-50%" : "0"}, ${vertical === VerticalAlignment.CENTER_TRANSLATED ? "-50%" : "0"});`;
        return `position: fixed; ${horizontalStyle} ${verticalStyle} ${transformStyle}`;
    }
</script>

<div class="hud" style="zoom: {zoom}%">
    {#each components as c}
        <div class="component"
             style="border: {editor ? '4px solid white' : 'none'}; {toStyle(c.settings.alignment)}"
             on:mousedown={(event) => startDrag(c, event)}>
            {#if c.name === "Watermark"}
                <Watermark/>
            {:else if c.name === "ArrayList"}
                <ArrayList/>
            {:else if c.name === "TabGui"}
                <TabGui/>
            {:else if c.name === "Notifications"}
                <Notifications/>
            {:else if c.name === "TargetHud"}
                <TargetHud/>
            {:else if c.name === "Hotbar"}
                <HotBar/>
            {:else if c.name === "Scoreboard"}
                <Scoreboard/>
            {:else if c.name === "Taco"}
                <Taco/>
            {:else if c.name === "Keystrokes"}
                <Keystrokes/>
            {:else if c.name === "Effects"}
                <Effects />
            {:else if c.name === "Text"}
                <p>{c.settings.text}</p>
            {:else if c.name === "Image"}
                <img alt="" src={c.settings.src} style="scale: {c.settings.scale};">
            {/if}
        </div>
    {/each}
</div>

<style lang="scss">
  .hud {
    height: 100vh;
    width: 100vw;
    position: relative;
  }

  .component {
    cursor: move;
    position: fixed;
  }
</style>
