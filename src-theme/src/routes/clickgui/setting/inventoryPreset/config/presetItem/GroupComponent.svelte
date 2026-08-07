<script lang="ts">
    import type {PresetItemGroup} from "../../../../../../integration/types";
    import {clickOutside} from "../../../../../../util/utils";
    import {scale} from "svelte/transition";
    import {createEventDispatcher} from "svelte";
    import GroupPreview from "./GroupPreview.svelte";
    import GroupPreferenceSelector from "./GroupPreferenceSelector.svelte";

    const dispatch = createEventDispatcher();

    export let group: PresetItemGroup;
    export let idx: number;

    let expanded = false;

    function handleChange() {
        dispatch("change")
    }

    function handleClickOutside(event: MouseEvent) {
        const target = event.target as HTMLElement
        if (!target.closest(".group-candidate-selector")) {
            expanded = false
        }
    }
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="wrapper">
    <div class="item-container item-background"
         class:active={expanded}
         class:hided={!expanded}
         on:click|preventDefault={() => expanded = !expanded}
    >
        <div class="image-wrapper">
            <GroupPreview bind:group />
        </div>
    </div>

    {#if expanded}
        <div
                class="selector-container-wrapper selector-container"
                transition:scale={{duration: 200, start: 0.9}}
                on:click|preventDefault
                use:clickOutside={handleClickOutside}
        >
            <GroupPreferenceSelector
                    bind:parentExpanded={expanded}
                    bind:items={group}
                    on:change={handleChange}
            />

            <div class="slot">
                <span>{idx === 0 ? "Offhand" : idx}</span>
            </div>
        </div>
    {/if}
</div>


<style lang="scss">
  @use "sass:color";
  @use "../../../../../../colors" as *;
  @use "../select" as *;
  @use "../item" as *;

  .wrapper {
    position: relative;
  }

  .item-container {
    position: relative;
    width: 48px;
    height: 48px;
    border-radius: 6px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: 0.3s all ease;

    &:hover {
      &.hided .delete {
        opacity: 1;
        pointer-events: all;
      }
    }
  }

  .active {
    outline: 1px solid $accent-color !important;

    & > .image-wrapper {
      filter: opacity(0.5);
    }
  }

  .selector-container {
    left: 50%;
    top: 50%;
    cursor: auto;
    transform-origin: left top;
  }

  .slot {
    position: absolute;
    font-size: 12px;
    font-weight: 500;
    left: 0;
    top: 0;
    padding: 0 5px;
    outline: 1px solid color.adjust($clickgui-text-color, $lightness: -85%);
    min-width: 20px;
    height: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 3px 0;
  }

  .image-wrapper {
    width: 32px;
    height: 32px;
  }
</style>
