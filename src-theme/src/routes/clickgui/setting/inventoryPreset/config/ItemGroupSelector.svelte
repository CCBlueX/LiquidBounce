<script lang="ts">
    import type {PresetItem} from "../../../../../integration/types";
    import {createEventDispatcher, onDestroy, onMount} from "svelte";
    import PresetItemSelector from "./presetItem/PresetItemSelector.svelte";
    import ItemImage from "../ItemImage.svelte";
    import {scaleFactor} from "../../../clickgui_store";
    import {clickOutside, portal} from "../../../../../util/utils";
    import {scale} from "svelte/transition";

    export let items: PresetItem[]
    export let parentExpanded: boolean = true
    export let useAccentColorOutline: boolean = false

    const dispatch = createEventDispatcher();
    let expanded: boolean = false

    let addRef: HTMLElement;
    let addElementPosition = { top: 0, left: 0, width: 0, height: 0 };
    const resizeObserver = new ResizeObserver(updatePosition);

    let choiceItems: string[];

    function updatePosition() {
        if (!addRef) return;
        const rect = addRef.getBoundingClientRect();
        addElementPosition = {
            top: rect.top + window.scrollY,
            left: rect.left + window.scrollX,
            width: rect.width,
            height: rect.height
        };
    }

    onMount(() => {
        window.addEventListener('resize', updatePosition);
        if (addRef) resizeObserver.observe(addRef);
        updatePosition();
    })

    onDestroy(() => {
        window.removeEventListener('resize', updatePosition);
        resizeObserver.disconnect();
    });

    function handleChange() {
        dispatch("change")
    }

    function setItem(item: PresetItem) {
        if (!items.some(
            existingItem => JSON.stringify(existingItem) === JSON.stringify(item)
        )) {
            items = [...items, item];
            handleChange()
        }
    }

    function removeItem(removedId: number) {
        items = items.filter((_, id) => id != removedId)
        handleChange()
    }

    $: choiceItems = items.filter(it => it.type == "SINGLE")
        .flatMap(it => it.item);

    $: {
        if (expanded) {
            updatePosition()
        }
    }

    $: if (!parentExpanded) {
        expanded = false
    }
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<!-- svelte-ignore a11y-no-noninteractive-element-interactions -->
<div class="items-container" class:accentColorOutline={useAccentColorOutline} {...$$restProps}>
    <div class="items">
        <div bind:this={addRef}>
            <div class="item-container add" on:click={() => expanded = !expanded}>
                <img src="img/menu/icon-plus.svg" alt="Add">
            </div>

            {#if expanded}
                <div
                        class="group-candidate-selector selector-container-wrapper selector-container"
                        style="
                                    transform: translateX(-50%) scale({$scaleFactor * 50}%);
                                    top: {addElementPosition.top + addElementPosition.height + 15}px;
                                    left: {addElementPosition.left + addElementPosition.width / 2}px
                                "
                        transition:scale={{duration: 200, start: 0.9}}
                        use:clickOutside={() => expanded = false}
                        use:portal
                >
                    <PresetItemSelector
                            setItem={setItem}
                            filter={it => !choiceItems.includes(it)}
                    />
                </div>
            {/if}
        </div>

        {#each items as item, idx}
            <div class="item-background item-container" on:click={() => removeItem(idx)}>
                <div class="item">
                    <ItemImage bind:item={item} />
                </div>
            </div>
        {/each}
    </div>
</div>

<style lang="scss">
  @use "sass:color";
  @use "../../../../../colors.scss" as *;
  @use "select" as *;
  @use "item" as *;

  .selector-container {
    height: 450px;
    transform-origin: top;
  }

  .items-container {
    border-radius: 6px;
    overflow-y: scroll;
    flex-shrink: 0;
    background-color: color-mix(in srgb, var(--clickgui-base-color) 85%, transparent);
    outline: 1px solid color-mix(in srgb, var(--clickgui-text-color) 15%, black);

    &.accentColorOutline {
      outline-color: color-mix(in srgb, var(--accent-color) 30%, transparent);
    }
  }

  .items {
    display: flex;
    gap: 9px;
    padding: 5px;
    flex-wrap: wrap;
  }

  .items-container::-webkit-scrollbar {
    width: 2px;
  }

  .item-container {
    width: 25px;
    height: 25px;
    border-radius: 3px;
    border: none;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: background-color 0.3s ease;

    &:hover {
      background-color: color-mix(in srgb, var(--menu-error-color) 70%, black);
    }

    & > .item {
      width: 16px;
      height: 16px;
    }
  }

  .add {
    background: var(--accent-color);
    position: relative;
    outline: none;

    &:hover {
      background-color: color-mix(in srgb, var(--accent-color) 90%, black);
    }

    & > img {
      width: 16px;
      height: 16px;
    }
  }
</style>
