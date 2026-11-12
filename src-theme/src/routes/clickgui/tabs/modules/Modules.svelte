<script lang="ts">
    import {onMount} from "svelte";
    import {fly} from "svelte/transition";
    import {cubicOut} from "svelte/easing";
    import {listen} from "../../../../integration/ws";
    import type {ModuleActivationEvent, ModuleToggleEvent} from "../../../../integration/events";
    import SideNav, {type NavItem} from "../../common/SideNav.svelte";
    import ModuleList from "./ModuleList.svelte";
    import ModulePane from "./ModulePane.svelte";
    import SearchResults from "./SearchResults.svelte";
    import {applyToggle, BINDS, isBound, loadModules, modules, selectCategory} from "./modules.svelte";

    const items = $derived<NavItem[]>([
        ...modules.categories.map(category => ({
            id: category.name,
            label: category.name,
            icon: category.icon ?? `img/clickgui/icon-${category.name.toLowerCase()}.svg`,
            count: modules.list.filter(module => module.category === category.name && module.enabled).length,
        })),
        {
            id: BINDS,
            label: BINDS,
            icon: "img/hud/keybinds/icon-keybinds.svg",
            count: modules.list.filter(isBound).length,
            separated: true,
        },
    ]);

    // Out of the layout while it leaves, so the list takes its room at once and not when it is gone
    function leave(node: HTMLElement) {
        const width = node.offsetWidth;

        return {
            duration: 100,
            css: (t: number) => `position: absolute; top: 0; right: 0; bottom: 0; width: ${width}px; opacity: ${t}; `
                + `transform: translateX(${(1 - t) * 24}px);`,
        };
    }

    onMount(() => loadModules(false));

    listen("moduleToggle", (event: ModuleToggleEvent) => applyToggle(event.moduleName, event.enabled));

    // The page stays loaded while the ClickGUI is closed, and binds can change in the meantime
    listen("moduleActivation", (event: ModuleActivationEvent) => {
        if (event.moduleName === "ClickGUI") {
            loadModules(false);
        }
    });
    listen("configTrackerChange", () => loadModules(true));
</script>

<div class="modules" class:with-pane={modules.selected !== null}>
    <SideNav {items} active={modules.query ? "" : modules.category} label="Categories" onselect={selectCategory}/>

    <section class="list">
        {#if modules.query}
            <SearchResults/>
        {:else}
            <ModuleList/>
        {/if}
    </section>

    {#if modules.selected !== null}
        <aside class="pane" in:fly={{x: 24, duration: 200, easing: cubicOut}} out:leave>
            <ModulePane name={modules.selected}/>
        </aside>
    {/if}
</div>

<style lang="scss">
  .modules {
    position: relative;
    flex: 1;
    min-width: 0;
    display: flex;
  }

  .list {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
  }

  .pane {
    flex: none;
    width: 460px;
    display: flex;
    flex-direction: column;
    border-left: 1px solid var(--clickgui-divider-color);
    background-color: var(--clickgui-pane-background-color);
  }

  @container workspace (max-width: 1099px) {
    .pane {
      width: 420px;
    }
  }

  // No room for the list beside the pane: the pane takes its place and its header leads back
  @container workspace (max-width: 819px) {
    .with-pane .list {
      display: none;
    }

    .pane {
      flex: 1;
      width: auto;
      border-left: none;
    }
  }
</style>
