<script lang="ts">
    import {slide, fade} from "svelte/transition";
    import {quintOut} from "svelte/easing";
    import {createEventDispatcher} from "svelte";

    export let options: string[];
    export let value: string;

    const dispatch = createEventDispatcher<{
        change: { value: string }
    }>();

    let expanded = false;
    let selectElement: HTMLElement;

    function handleWindowClick(e: MouseEvent) {
        if (!selectElement.contains(e.target as Node)) {
            expanded = false;
        }
    }
</script>

<svelte:window on:click={handleWindowClick}/>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="single-select" on:click={() => expanded = !expanded} bind:this={selectElement} class:expanded>
    <div class="header">
        <span class="title">{value}</span>
        <img src="img/menu/icon-select-arrow.svg" alt="expand">
    </div>

    {#if expanded}
        <div class="options" transition:fade|global={{ duration: 200, easing: quintOut }}>
            {#each options as o}
                <div on:click={() => dispatch("change", {value: o})} class="option" class:active={o === value}
                     transition:slide|global={{ duration: 200, easing: quintOut }}>{o}</div>
            {/each}
        </div>
    {/if}
</div>

<style lang="scss">
  @import "../../../../colors.scss";

  .single-select {
    cursor: pointer;
    min-width: 250px;
    position: relative;

    &.expanded {
      .header {
        border-radius: 5px 5px 0 0;
      }
    }
  }

  .header {
    background-color: $accent-color;
    padding: 20px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-radius: 5px;
    transition: ease border-radius .2s;

    .title {
      font-weight: 500;
      color: $menu-text-color;
      font-size: 20px;
    }
  }

  .options {
    position: absolute;
    z-index: 1000;
    width: 100%;
    border-radius: 0 0 5px 5px;
    max-height: 250px;
    overflow: auto;
    background-color: rgba($menu-base-color, 0.9);

    .option {
      font-weight: 500;
      color: $menu-text-dimmed-color;
      font-size: 20px;
      padding: 15px 20px;
      transition: ease color .2s;

      &:hover {
        color: $menu-text-color;
      }

      &.active {
        color: $accent-color;
      }
    }
  }
</style>