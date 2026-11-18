<script lang="ts">
    import type {ConfigurableSetting, Module} from "../../integration/types";
    import {getModuleSettings, setModuleEnabled} from "../../integration/rest";
    import {listen} from "../../integration/ws";
    import type {ClickGuiValueChangeEvent, KeyboardKeyEvent, ModuleToggleEvent} from "../../integration/events";
    import {highlightModuleName} from "./clickgui_store";
    import {onMount} from "svelte";
    import {convertToSpacedString, spaceSeperatedNames} from "../../theme/theme_config";
    import {isClickGuiScreen} from "../../util/utils";
    import {escapeLayer} from "../../integration/escape";

    export let modules: Module[];

    let resultElements: HTMLElement[] = [];
    let searchContainerElement: HTMLElement;
    let autoFocus: boolean = true
    let searchInputElement: HTMLElement;
    let query: string;
    let filteredModules: Module[] = [];
    let selectedIndex = 0;
    let hasFocus = false;

    type SearchableModule = {
        raw: Module;
        lowerName: string;
        lowerAliases: string[];
    };

    // Cache lowercase names/aliases to avoid repeated toLowerCase() per keystroke.
    let searchableModules: SearchableModule[] = [];
    $: searchableModules = modules.map(m => ({
        raw: m,
        lowerName: m.name.toLowerCase(),
        lowerAliases: m.aliases.map(a => a.toLowerCase()),
    }));

    function reset() {
        filteredModules = [];
        query = "";
        $highlightModuleName = null;
    }

    const layer = escapeLayer(reset);
    $: layer.setOpen(!!query);

    function filterModules(resetIndex: boolean) {
        if (!query) {
            reset();
            return;
        }

        if (resetIndex) {
            selectedIndex = 0;
        }

        const pureQuery = query.toLowerCase().replaceAll(" ", "");

        filteredModules = searchableModules.filter(({ raw, lowerName, lowerAliases }) => {
            return lowerName.includes(pureQuery)
                || lowerAliases.some(a => a.includes(pureQuery));
        }).map(it => it.raw);
    }

    async function handleKeyDown(e: KeyboardKeyEvent) {
        if (!isClickGuiScreen(e.screen)) {
            return;
        }

        if (filteredModules.length === 0 || e.action === 0) {
            return;
        }

        switch (e.key) {
            case "key.keyboard.down":
                selectedIndex = (selectedIndex + 1) % filteredModules.length;
                break;
            case "key.keyboard.up":
                selectedIndex =
                    (selectedIndex - 1 + filteredModules.length) %
                    filteredModules.length;
                break;
            case "key.keyboard.enter":
                await toggleModule(
                    filteredModules[selectedIndex].name,
                    !filteredModules[selectedIndex].enabled,
                );
                break;
            case "key.keyboard.tab":
                const m = filteredModules[selectedIndex]?.name;
                if (m) {
                    $highlightModuleName = m;
                }
                break;
        }

        resultElements[selectedIndex]?.scrollIntoView({
            behavior: "instant",
            block: "nearest",
        });
    }

    function handleBrowserKeyDown(e: KeyboardEvent) {
        if (e.key === "ArrowDown" || e.key === "ArrowUp" || e.key === "Tab") {
            e.preventDefault();
        }
    }

    async function toggleModule(name: string, enabled: boolean) {
        await setModuleEnabled(name, enabled);
    }

    function handleWindowClick(e: MouseEvent) {
        if (!searchContainerElement.contains(e.target as Node) && !hasFocus) {
            reset();
        }
    }

    function handleMouseOut() {
        hasFocus = false;
        reset();
    }

    function handleWindowKeyDown() {
        if (document.activeElement !== document.body) {
            return;
        }

        if (autoFocus) {
            searchInputElement.focus();
        }
    }

    function applyValues(configurable: ConfigurableSetting) {
        autoFocus = configurable.value.find(v => v.name === "SearchBarAutoFocus")?.value as boolean ?? true;
    }

    onMount(async () => {
        const clickGuiSettings = await getModuleSettings("ClickGUI");
        applyValues(clickGuiSettings);

        if (autoFocus) {
            searchInputElement.focus();
        }
    });

    listen("moduleToggle", (e: ModuleToggleEvent) => {
        const mod = modules.find((m) => m.name === e.moduleName);
        if (!mod) {
            return;
        }
        mod.enabled = e.enabled;

        // Refilter modules to update enabled state
        filterModules(false);
    });

    listen("keyboardKey", handleKeyDown);

    listen("clickGuiValueChange", (e: ClickGuiValueChangeEvent) => {
        applyValues(e.configurable);
    });
</script>

<svelte:window on:click={handleWindowClick} on:keydown={handleWindowKeyDown} on:contextmenu={handleWindowClick}/>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<div
        class="search"
        class:has-results={query}
        class:has-focus={hasFocus}
        bind:this={searchContainerElement}
        on:mouseenter={() => hasFocus = true}
        on:mouseleave={handleMouseOut}
>
    <input
            type="text"
            class="search-input"
            placeholder="Search"
            spellcheck="false"
            bind:value={query}
            bind:this={searchInputElement}
            on:input={() => filterModules(true)}
            on:keydown={handleBrowserKeyDown}
    />

    {#if query}
        <div class="results">
            {#if filteredModules.length > 0}
                {#each filteredModules as {name, enabled, aliases}, index (name)}
                    <!-- svelte-ignore a11y-click-events-have-key-events -->
                    <!-- svelte-ignore a11y-no-static-element-interactions -->
                    <div
                            class="result"
                            class:enabled
                            on:click={() => toggleModule(name, !enabled)}
                            on:contextmenu|preventDefault={() => $highlightModuleName = name}
                            class:selected={selectedIndex === index}
                            bind:this={resultElements[index]}
                    >
                        <div class="module-name">
                            {$spaceSeperatedNames ? convertToSpacedString(name) : name}
                        </div>
                        <div class="aliases">
                            {#if aliases.length > 0}
                                (aka {aliases.map(name => $spaceSeperatedNames ? convertToSpacedString(name) : name).join(", ")})
                            {/if}
                        </div>
                    </div>
                {/each}
            {:else}
                <div class="placeholder">No modules found</div>
            {/if}
        </div>
    {/if}
</div>

<style lang="scss">

  .search {
    position: fixed;
    left: 50%;
    top: 70px;
    transform: translateX(-50%);
    background-color: var(--clickgui-search-background-color);
    backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturation));
    width: 600px;
    border: 1px solid var(--accent-border-color);
    border-radius: var(--radius-pill);
    overflow: hidden;
    transition: border-radius var(--duration-base) var(--ease-standard),
    border-color var(--duration-fast) var(--ease-standard),
    box-shadow var(--duration-fast) var(--ease-standard);
    box-shadow: var(--elevation-md) var(--clickgui-search-shadow-color);

    &.has-results {
      border-radius: var(--radius-md);
    }

    &:focus-within,
    &.has-focus {
      z-index: 9999999999;
      border-color: var(--accent-border-strong-color);
    }
  }

  .results {
    border-top: solid 1px var(--clickgui-search-border-color);
    padding: 6px 8px;
    max-height: 250px;
    overflow: auto;

    .result {
      font-size: 14px;
      margin: 2px 0;
      padding: 10px 12px;
      border: 1px solid transparent;
      border-radius: var(--radius-sm);
      transition: background-color var(--duration-fast) var(--ease-standard),
      border-color var(--duration-fast) var(--ease-standard);
      cursor: pointer;
      display: grid;
      grid-template-columns: max-content 1fr max-content;
      align-items: center;

      .module-name {
        color: var(--clickgui-text-dimmed-color);
        font-weight: 500;
        transition: color var(--duration-fast) var(--ease-standard);
      }

      &.enabled {
        .module-name {
          color: var(--clickgui-search-enabled-color);
        }
      }

      .aliases {
        color: var(--clickgui-search-alias-color);
        margin-left: 10px;
      }

      &.selected {
        background-color: var(--accent-subtle-background-color);
        border-color: var(--accent-border-color);
      }

      &:hover {
        color: var(--clickgui-text-color);
        background-color: var(--clickgui-module-hover-background-color);

        &::after {
          content: "Right-click to locate";
          color: var(--clickgui-search-hint-color);
          font-size: 11px;
          margin-left: 12px;
        }
      }
    }

    .placeholder {
      color: var(--clickgui-text-dimmed-color);
      font-size: 14px;
      padding: 12px;
    }

    &::-webkit-scrollbar {
      width: 4px;
    }
  }

  .search-input {
    padding: 16px 20px;
    background-color: transparent;
    border: none;
    font-family: "Inter", sans-serif;
    font-size: 15px;
    color: var(--clickgui-text-color);
    width: 100%;
  }
</style>
