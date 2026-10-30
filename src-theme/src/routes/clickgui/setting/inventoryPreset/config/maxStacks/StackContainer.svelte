<script lang="ts">
    import type {MaxStacksGroup} from "../../../../../../integration/types";
    import noUiSlider, {type API} from "nouislider";
    import ValueInput from "../../../common/ValueInput.svelte";
    import {createEventDispatcher, onMount} from "svelte";
    import ItemGroupSelector from "../ItemGroupSelector.svelte";

    export let group: MaxStacksGroup;

    const dispatch = createEventDispatcher();

    let slider: HTMLElement;
    let apiSlider: API;

    onMount(() => {
        apiSlider = noUiSlider.create(slider, {
            start: group.itemCount,
            connect: "lower",
            range: {
                'min': [0, 1],
                '15%': [5, 1],
                '30%': [16, 16],
                '50%': [2 * 64, 64],
                '75%': [5 * 64, 64],
                'max': [64 * 9 * 4],
            },
            step: 1,
        });

        apiSlider.on("update", (values) => {
            group.itemCount = parseInt(values[0].toString());
        });

        apiSlider.on("set", () => {
            dispatch("change");
        });
    });

    function handleChange() {
        dispatch("change");
    }

    function handleDelete() {
        dispatch("delete");
    }

    $: nStacks = (group.itemCount / 64) | 0;
    $: nItems = (group.itemCount % 64) | 0;

    function updateItemCount(stacks: number, left: number) {
        apiSlider.set(stacks * 64 + left);
    }
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<!-- svelte-ignore a11y-no-noninteractive-element-interactions -->
<div class="container-wrapper">
    <div class="container">
        <ItemGroupSelector bind:items={group.items} on:change={handleChange} style="width: 309px; max-height: 70px;"/>

        <div class="slider-wrapper">
            <div class="slider-top">
                <span class="slider-left">Limit</span>
                <div class="slider-right">
                    {#if group.itemCount < 9 * 4 * 64}
                        <span class:muted={group.itemCount < 64}>64 x </span>
                        <ValueInput valueType="int" bind:value={nStacks}
                                    on:change={(e) => updateItemCount(e.detail.value, nItems)}/>
                        <span class:muted={group.itemCount < 64}> + </span>
                        <ValueInput valueType="int" bind:value={nItems}
                                    on:change={(e) => updateItemCount(nStacks, e.detail.value)}/>
                    {:else}
                        <span>&infin;</span>
                    {/if}
                </div>
            </div>
            <div bind:this={slider} class="slider"></div>
        </div>
    </div>
    <div class="delete">
        <img src="img/menu/icon-exit-danger.svg" alt="exit" on:click={handleDelete}>
    </div>
</div>

<style lang="scss">
  @use "sass:color";
  @use "../../../../../../colors.scss" as *;
  @use "../select" as *;
  @use "../item" as *;

  .muted {
    color: grey;
  }

  .container-wrapper {
    display: flex;
  }

  .slider-top {
    display: flex;
    font-size: 12px;
  }

  .slider-right {
    margin-left: auto;
    color: white;

    & > span {
      line-height: 18px;
    }
  }

  .slider-wrapper {
    flex-grow: 1;
    max-height: 35px;
    display: flex;
    flex-direction: column;
    justify-content: center;
  }

  .delete {
    flex-shrink: 0;
    height: 35px;
    width: 35px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;

    & > img {
      width: 16px;
      height: 16px;
      opacity: 0.5;
      transition: opacity 0.3s ease;

      &:hover {
        opacity: 1;
      }
    }
  }

  .item-group-selector {
    width: 307px;
    max-height: 70px;
  }

  .container {
    gap: 10px;
    display: flex;
    width: 100%;
  }

  .slider {
    margin-top: 3px;
    padding-right: 10px;
  }
</style>
