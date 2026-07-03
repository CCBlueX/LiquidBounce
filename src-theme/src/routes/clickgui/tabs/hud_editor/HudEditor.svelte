<script lang="ts">
    import {onMount} from "svelte";

    import Hud from "../../../hud/Hud.svelte";
    import {setHudEditorOpen} from "../../clickgui_store";
    import {
        HORIZONTAL_ANCHOR_ZONES,
        HUD_EDITOR_GRID_SIZE,
        type HudEditorDragState,
        VERTICAL_ANCHOR_ZONES
    } from "./constants";

    let dragState: HudEditorDragState | undefined;

    function handleDragStateChange(state: HudEditorDragState): void {
        dragState = state.dragging ? state : undefined;
    }

    onMount(() => {
        setHudEditorOpen(true);

        return () => {
            setHudEditorOpen(false);
        };
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
    {/if}

    <Hud inEditor onDragStateChange={handleDragStateChange}/>
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
        background-color: var(--clickgui-hud-editor-anchor-point-active-color);
        box-shadow: 0 0 8px var(--clickgui-hud-editor-anchor-point-active-color);
      }
    }
  }

  .anchor-point {
    position: absolute;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background-color: var(--clickgui-hud-editor-anchor-point-color);
    transform: translate(-50%, -50%);

    &.horizontal-left {
      left: 0;
    }

    &.horizontal-center {
      left: 50%;
    }

    &.horizontal-right {
      left: 100%;
    }

    &.vertical-upper {
      top: 0;
    }

    &.vertical-center {
      top: 50%;
    }

    &.vertical-lower {
      top: 100%;
    }
  }
</style>
