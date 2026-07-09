<script lang="ts">
    import {onMount} from "svelte";

    import Hud from "../../../hud/Hud.svelte";
    import {getGameWindow, setHudEditorSelected} from "../../../../integration/rest";
    import {
        HORIZONTAL_ANCHOR_ZONES,
        HUD_EDITOR_GRID_SIZE,
        type HudEditorDragState,
        VERTICAL_ANCHOR_ZONES
    } from "./constants";
    import ComponentDrawer from "./drawer/ComponentDrawer.svelte";
    import {darken} from "../../clickgui_store";
    import {listen} from "../../../../integration/ws";
    import type {ScaleFactorChangeEvent} from "../../../../integration/events";

    let dragState: HudEditorDragState | undefined;
    let scaleFactor = 2;

    function handleDragStateChange(state: HudEditorDragState): void {
        dragState = state.dragging ? state : undefined;
    }

    function toClientCoordinate(hudCoordinate: number): number {
        return hudCoordinate * scaleFactor / 2;
    }

    onMount(() => {
        getGameWindow().then(gameWindow => {
            scaleFactor = gameWindow.scaleFactor;
        });

        $darken = false;
        setHudEditorSelected(true);

        return () => {
            $darken = true;
            setHudEditorSelected(false);
        };
    });

    listen("scaleFactorChange", (event: ScaleFactorChangeEvent) => {
        scaleFactor = event.scaleFactor;
    });
</script>

<div
        class="hud-editor"
        class:grid={dragState !== undefined}
        style="background-size: {HUD_EDITOR_GRID_SIZE}px {HUD_EDITOR_GRID_SIZE}px;"
>
    {#if dragState}
        <div class="anchor-zones">
            {#each VERTICAL_ANCHOR_ZONES as verticalZone}
                {#each HORIZONTAL_ANCHOR_ZONES as horizontalZone}
                    <div
                            class="anchor-zone"
                            class:active={dragState.verticalZone === verticalZone &&
                                dragState.horizontalZone === horizontalZone}
                    >
                        <div class="anchor-point horizontal-{horizontalZone} vertical-{verticalZone}"></div>
                    </div>
                {/each}
            {/each}
        </div>

        {#if dragState.verticalGuide !== undefined}
            <div class="magnetic-guide vertical" style="left: {toClientCoordinate(dragState.verticalGuide)}px"></div>
        {/if}
        {#if dragState.horizontalGuide !== undefined}
            <div class="magnetic-guide horizontal" style="top: {toClientCoordinate(dragState.horizontalGuide)}px"></div>
        {/if}
    {/if}

    <ComponentDrawer/>
    <Hud
            inEditor
            onDragStateChange={handleDragStateChange}
            magneticTargetIds={dragState?.magneticTargetIds ?? []}
    />
</div>

<style lang="scss">
  .hud-editor {
    position: absolute;
    inset: 0;

    &.grid {
      background-image: linear-gradient(to right, var(--clickgui-hud-editor-grid-color) 1px, transparent 1px),
      linear-gradient(to bottom, var(--clickgui-hud-editor-grid-color) 1px, transparent 1px);
    }
  }

  .anchor-zones {
    position: absolute;
    inset: 0;
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    grid-template-rows: repeat(3, 1fr);
    pointer-events: none;
  }

  .anchor-zone {
    position: relative;
    border: solid 1px var(--clickgui-hud-editor-anchor-zone-border-color);
    background-color: var(--clickgui-hud-editor-anchor-zone-background-color);

    &.active {
      border-color: var(--clickgui-hud-editor-anchor-zone-active-border-color);
      background-color: var(--clickgui-hud-editor-anchor-zone-active-background-color);

      .anchor-point {
        width: 14px;
        height: 14px;
        opacity: 1;
        background-color: var(--clickgui-hud-editor-anchor-point-active-color);
        box-shadow: 0 0 10px var(--clickgui-hud-editor-anchor-point-active-shadow-color);
        z-index: 1000;

        &::after {
          opacity: 1;
          transform: scale(1);
        }
      }
    }
  }

  .anchor-point {
    --translate-x: -50%;
    --translate-y: -50%;

    position: absolute;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    opacity: 0.55;
    background-color: var(--clickgui-hud-editor-anchor-point-color);
    transform: translate(var(--translate-x), var(--translate-y));
    transition: width 100ms ease, height 100ms ease, opacity 100ms ease, box-shadow 100ms ease;

    &::after {
      content: "";
      position: absolute;
      inset: -6px;
      border: solid 2px var(--clickgui-hud-editor-anchor-point-active-ring-color);
      border-radius: 50%;
      opacity: 0;
      transform: scale(0.6);
      transition: opacity 100ms ease, transform 100ms ease;
    }

    &.horizontal-left {
      left: 0;
      --translate-x: 0;
    }

    &.horizontal-center {
      left: 50%;
    }

    &.horizontal-right {
      left: 100%;
      --translate-x: -100%;
    }

    &.vertical-upper {
      top: 0;
      --translate-y: 0;
    }

    &.vertical-center {
      top: 50%;
    }

    &.vertical-lower {
      top: 100%;
      --translate-y: -100%;
    }
  }

  .magnetic-guide {
    position: absolute;
    z-index: 1;
    pointer-events: none;
    background-color: var(--clickgui-hud-editor-magnetic-guide-color);
    box-shadow: 0 0 6px var(--clickgui-hud-editor-magnetic-guide-shadow-color);

    &.vertical {
      top: 0;
      bottom: 0;
      width: 1px;
      transform: translateX(-50%);
    }

    &.horizontal {
      right: 0;
      left: 0;
      height: 1px;
      transform: translateY(-50%);
    }
  }
</style>
