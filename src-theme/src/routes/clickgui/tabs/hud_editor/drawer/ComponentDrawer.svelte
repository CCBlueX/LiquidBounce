<script lang="ts">
    import type {HudComponent} from "../../../../../integration/types";
    import {onMount} from "svelte";
    import {getComponents} from "../../../../../integration/rest";
    import DrawerHudComponent from "./DrawerHudComponent.svelte";
    import {fly} from "svelte/transition";

    let drawerShown = $state(false);

    let components: HudComponent[] = $state([]);
    let filteredComponents: HudComponent[] = $state([]);
    let drawerElement: HTMLElement | null = $state(null);

    let query = $state("");

    onMount(async () => {
        components = await getComponents("liquidbounce");
        filteredComponents = components;
    });

    function handleWindowClick(e: MouseEvent) {
        if (!e.target || !drawerElement) return;

        if (!drawerElement.contains(e.target as HTMLBRElement)) {
            drawerShown = false;
        }
    }

    function handleSearch(e: Event & { currentTarget: EventTarget & HTMLInputElement }) {
        filteredComponents = components.filter(c => c.name.toLowerCase().includes(query.toLowerCase()));
    }
</script>

<svelte:window onclick={handleWindowClick}/>

<div class="component-drawer" bind:this={drawerElement}>
    <button class="button-toggle-drawer" onclick={() => drawerShown = !drawerShown}>Add Component</button>

    {#if drawerShown}
        <div class="drawer" transition:fly={{ y: -10, duration: 200 }}>
            <input type="text" class="input-search" placeholder="Search" bind:value={query} oninput={handleSearch}>

            <div class="component-list">
                {#if filteredComponents.length !== 0}
                    {#each filteredComponents as c}
                        <DrawerHudComponent component={c}/>
                    {/each}
                {:else}
                    <span class="no-results">No components found</span>
                {/if}
            </div>
        </div>
    {/if}
</div>

<style lang="scss">

  .component-drawer {
    position: fixed;
    left: 50%;
    transform: translateX(-50%);
    top: 70px;
  }

  .input-search {
    all: unset;
    font-family: "Inter", sans-serif;
    color: var(--text-color);
    font-size: 16px;
    background-color: transparent;
    border-bottom: solid 2px var(--accent-color);
    padding: 15px 25px;
    width: 100%;
    box-sizing: border-box;
  }

  .drawer {
    border-radius: 5px;
    width: 500px;
    position: absolute;
    left: 50%;
    transform: translateX(-50%);
    box-shadow: 0 0 10px var(--clickgui-hud-editor-drawer-shadow-color);
    background-color: var(--clickgui-hud-editor-drawer-background-color);
    margin-top: 20px;

    &::before {
      content: "";
      display: block;
      position: absolute;
      width: 0;
      height: 0;
      border-top: 8px solid transparent;
      border-bottom: 8px solid transparent;
      border-right: 8px solid var(--clickgui-hud-editor-component-settings-background-color);
      left: 50%;
      top: -12px;
      transform: translateX(-50%) rotate(90deg);
      z-index: -1;
    }
  }

  .component-list {
    display: flex;
    flex-direction: column;
    row-gap: 10px;
    max-height: 400px;
    overflow: auto;
    padding: 10px;
  }

  .button-toggle-drawer {
    padding: 8px 15px;
    background-color: var(--clickgui-hud-editor-drawer-toggle-button-background-color);
    color: var(--clickgui-hud-editor-drawer-toggle-button-color);
    border: none;
    font-family: "Inter", sans-serif;
    font-size: 16px;
    cursor: pointer;
    font-weight: 500;
    border-radius: 5px;

    transition: ease background-color .2s;

    &:hover {
      background-color: var(--clickgui-button-hover-background-color);
    }
  }

  .no-results {
    color: var(--clickgui-text-dimmed-color);
    font-size: 14px;
  }
</style>
