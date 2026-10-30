<script lang="ts">
    import type {PresetItem} from "../../../../../../integration/types";
    import ItemGroupSelector from "../ItemGroupSelector.svelte";
    import {createEventDispatcher, onMount} from "svelte";
    import PresetTooltip from "../PresetTooltip.svelte";

    export let items: PresetItem[];
    export let parentExpanded: boolean

    const dispatch = createEventDispatcher();
    let forceShowItemSelect = false

    function handleChange() {
        dispatch("change")
    }

    function handleChoiceChange(choice: "any" | "ignore" | "items") {
        forceShowItemSelect = false

        if (choice === "any") {
            items = []
        } else if (choice === "ignore") {
            items = [{type: "IGNORE"}]
        } else {
            if (!showingItems) {
                items = []
                forceShowItemSelect = true
            }
        }

        handleChange()
    }

    onMount(() => {
        if (showingItems) {
            forceShowItemSelect = true
        }
    })

    $: any = items.length === 0 && !forceShowItemSelect
    $: ignore = items.length === 1 && items[0].type === "IGNORE" && !forceShowItemSelect
    $: showingItems = forceShowItemSelect || items.length >= 1 && items[0].type !== "IGNORE"
</script>

<div class="wrapper">
    <div class="select-title">
        <span>Preference</span>
    </div>

    <div class="choices">
        <button class:active={any}
                onclick={() => !any && handleChoiceChange("any")}>
            Any
            <span>
                <PresetTooltip align="bottom_center" text="You have no content preferences for this slot"/>
            </span>
        </button>

        <button class:active={ignore}
                onclick={() => !ignore && handleChoiceChange("ignore")}>
            Ignore
            <span>
                <PresetTooltip align="bottom_center" text="InventoryCleaner cannot touch anything that is in this slot."/>
            </span>
        </button>

        <button class:active={showingItems}
                onclick={() => !showingItems && handleChoiceChange("items")}>
            Items
            <span>
                <PresetTooltip align="bottom_center" text="All possible items that will be in this slot, the first item, the higher its importance, if there is no item in the slot, the lower one will be selected in importance"/>
            </span>
        </button>
    </div>

    {#if showingItems}
        <!-- todo: fix the css max-height -->
        <ItemGroupSelector
                bind:parentExpanded
                bind:items={items}
                on:change={handleChange}
                useAccentColorOutline={true}
                style="margin-top: 15px; width: 100%; max-height: calc(450px - 30px - 45px - 20px - 20px)"
        />
    {/if}
</div>

<style lang="scss">
  @use "sass:color";
  @use "../../../../../../colors" as *;
  @use "../select" as *;
  @use "../item" as *;

  .choices {
    margin-top: 20px;
    display: flex;
    gap: 2px;
    outline: 1px solid color-mix(in srgb, var(--accent-color) 30%, transparent);
    border-radius: 3px;
    padding: 2px;

    & > button {
      width: 100%;
      border: none;
      background-color: color-mix(in srgb, var(--clickgui-base-color) 30%, transparent);
      color: var(--clickgui-text-dimmed-color);
      height: 25px;
      border-radius: 3px;
      cursor: pointer;
      transition: background-color 0.3s ease;

      &:hover {
        color: var(--clickgui-text-color);
      }

      &.active {
        background-color: color-mix(in srgb, var(--accent-color) 20%, transparent);
        color: var(--accent-color);
      }

      & > span {
        opacity: 0.3;
        transition: opacity 0.3s ease;
      }

      & > span:hover {
        opacity: 1;
      }
    }
  }

  .candidates {
    margin-top: 20px;
    height: calc(100% - 40px);
  }

  .wrapper {
    position: relative;
    height: 100%;
  }
</style>
