<script lang="ts">
    import {portal} from "../../../../../util/utils"
    import type {InventoryPreset} from "../../../../../integration/types";
    import {createEventDispatcher} from "svelte";
    import {fly, scale} from "svelte/transition"
    import {backOut} from "svelte/easing";
    import ItemsGroup from "./presetItem/Items.svelte";
    import {scaleFactor} from "../../../clickgui_store";
    import MaxStacksContainer from "./maxStacks/MaxStacksContainer.svelte";
    import PresetTooltip from "./PresetTooltip.svelte";

    export let preset: InventoryPreset

    const dispatch = createEventDispatcher();

    function handleChange() {
        dispatch("change")
    }

    function handleClose() {
        dispatch("close")
    }

    function createNewMaxStacksGroup() {
        if (preset.maxStacks.length >= 50) {
            return
        }

        preset.maxStacks = [...preset.maxStacks, {
            itemCount: 0,
            items: []
        }]

        handleChange()
    }
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="modal" use:portal on:click={handleClose} transition:fly={{duration: 200}}>
    <div class="container" on:click|stopPropagation transition:scale={{duration: 200, easing: backOut, start: 0.9}} style="transform: translateX(-50%) scale({$scaleFactor * 50}%)">
        <div class="title">
            <span>Preset editor</span>
        </div>

        <div class="items-container">
            <div class="header muted">HOTBAR <PresetTooltip align="bottom_center" text="Items that will be in the hotbar, each item has its own slot selected"/></div>

            <ItemsGroup
                    bind:items={preset.items}
                    on:change={handleChange}
            />
        </div>

        <div class="throws-items-container">
            <div class="header">
                <span class="muted">LIMITS <PresetTooltip align="bottom_center" text="Limits on items, items that exceed this limit will be thrown away"/></span>
                <span class="createMaxStacksRule"
                      class:disabled={preset.maxStacks.length >= 50}
                      on:click={createNewMaxStacksGroup}
                >
                    CREATE A NEW LIMIT
                </span>
            </div>

            <MaxStacksContainer
                    bind:groups={preset.maxStacks}
                    on:change={handleChange}
            />
        </div>
    </div>
</div>

<style lang="scss">
  @use "sass:color";
  @use "../../../../../colors.scss" as *;

  .createMaxStacksRule {
    color: white;
    background-color: $accent-color;
    padding: 2px;
    font-size: 8px;
    border-radius: 3px;
    cursor: pointer;
    transition: background-color 0.3s ease;
    margin-left: 10px;

    &:hover {
      background-color: color.adjust(color.adjust($accent-color, $saturation: -30%), $lightness: -10%);
    }
  }

  .container {
    transform-origin: top;
    position: fixed;
    border-radius: 3px;
    width: 700px;
    min-width: 700px;
    box-shadow: 0 0 10px rgba($clickgui-base-color, 0.5);
    background-color: rgba($clickgui-base-color, 0.9);
    left: 50%;
    top: 20px;
  }

  .modal {
    z-index: 9999;
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba($clickgui-base-color, 0.3);
    backdrop-filter: blur(5px);
    color: $clickgui-text-color;
  }

  .disabled {
    cursor: not-allowed;
    opacity: 0.3;
  }

  .right {
    margin-left: auto;
  }

  .items-container {
    padding: 20px;
  }

  .throws-items-container {
    padding: 0 20px 20px;
  }
  
  .clear-throws {
    cursor: pointer;
    text-decoration: underline dotted;
    font-size: 12px;
  }

  .header {
    margin-bottom: 10px;
    font-size: 16px;
    font-weight: 600;
    text-transform: uppercase;
    color: $clickgui-text-color;
    display: flex;
    align-items: center;
  }

  .muted {
    color: rgba($clickgui-text-dimmed-color, 0.6);
  }

  .title {
    padding: 0 20px;
    height: 60px;
    position: relative;
    border-bottom: $accent-color solid 1px;
    background-color: rgba($clickgui-base-color, 0.5);
    color: $menu-text-color;
    border-radius: 3px 3px 0 0;

    display: flex;
    align-items: center;

    & > span {
      font-weight: 500;
      letter-spacing: 1px;
      font-size: 16px;
    }
  }
</style>
