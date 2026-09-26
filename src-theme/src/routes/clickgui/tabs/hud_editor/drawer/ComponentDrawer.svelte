<script lang="ts">
    import type {HudComponentCatalogEntry, Metadata} from "../../../../../integration/types";
    import {onMount, tick} from "svelte";
    import {addComponent, getComponentCatalog, getMetadata} from "../../../../../integration/rest";
    import DrawerHudComponent from "./DrawerHudComponent.svelte";
    import {fly} from "svelte/transition";

    let metadata: Metadata;

    let drawerShown = $state(false);
    let components: HudComponentCatalogEntry[] = $state([]);
    let filteredComponents: HudComponentCatalogEntry[] = $state([]);
    let drawerElement: HTMLElement | null = $state(null);
    let searchInput: HTMLInputElement | null = $state(null);
    let query = $state("");

    onMount(async () => {
        metadata = await getMetadata();
        await refreshComponents();
    });

    async function refreshComponents() {
        components = (await getComponentCatalog(metadata.id)).sort((a, b) => a.name.localeCompare(b.name));
        filteredComponents = components.filter(c => c.name.toLowerCase().includes(query.toLowerCase()));
    }

    function handleWindowClick(e: MouseEvent) {
        if (!e.target || !drawerElement) return;

        if (!drawerElement.contains(e.target as HTMLBRElement)) {
            drawerShown = false;
        }
    }

    function handleSearch() {
        filteredComponents = components.filter(c => c.name.toLowerCase().includes(query.toLowerCase()));
    }

    async function toggleDrawer() {
        drawerShown = !drawerShown;
        query = "";

        if (drawerShown) {
            await refreshComponents();
            await tick();
            searchInput?.focus();
        }
    }

    async function handleAddComponent(component: HudComponentCatalogEntry) {
        await addComponent(component.id);
        if (component.singleton) {
            component.canAdd = false;
        }
        drawerShown = false;
        query = "";
    }
</script>

<svelte:window onclick={handleWindowClick}/>

<div class="component-drawer" bind:this={drawerElement}>
    <button class="button-toggle-drawer" onclick={toggleDrawer}>Add Component</button>

    {#if drawerShown}
        <div class="drawer" transition:fly={{ y: -10, duration: 200 }}>
            <input bind:this={searchInput} type="text" class="input-search" placeholder="Search" bind:value={query}
                   oninput={handleSearch}>

            <div class="component-list">
                {#if filteredComponents.length !== 0}
                    {#each filteredComponents as c}
                        <DrawerHudComponent component={c} onselect={handleAddComponent}/>
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
    z-index: 9999;
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
    width: 600px;
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
    padding: 15px;
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
    padding: 0 10px;
    font-size: 14px;
  }
</style>
