<script lang="ts">
    import { createEventDispatcher, onMount } from "svelte";
    import type { Module } from "../../integration/types";
    import { setModuleEnabled } from "../../integration/rest";
    import { listen } from "../../integration/ws";

    export let modules: Module[];

    const dispatch = createEventDispatcher<{
        highlightModule: { name: string };
    }>();

    let resultElements: HTMLElement[] = [];
    let searchContainerElement: HTMLElement;
    let searchInputElement: HTMLElement;
    let query: string;
    let filteredModules: Module[] = [];
    let selectedIndex = 0;

    function reset() {
        filteredModules = [];
        query = "";
        dispatch("highlightModule", { name: "" });
    }

    function filterModules() {
        if (!query) {
            reset();
            return;
        }

        selectedIndex = 0;

        filteredModules = modules.filter((m) =>
            m.name.toLowerCase().startsWith(query.toLowerCase()),
        );
    }

    async function handleKeyDown(e: KeyboardEvent) {
        if (filteredModules.length === 0) {
            return;
        }

        switch (e.key) {
            case "ArrowDown":
                e.preventDefault(); // Prevent arrow up/down from messing with cursor position
                selectedIndex = (selectedIndex + 1) % filteredModules.length;
                break;
            case "ArrowUp":
                e.preventDefault();
                selectedIndex =
                    (selectedIndex - 1 + filteredModules.length) %
                    filteredModules.length;
                break;
            case "Enter":
                await toggleModule(
                    filteredModules[selectedIndex].name,
                    !filteredModules[selectedIndex].enabled,
                );
                break;
        }

        resultElements[selectedIndex].scrollIntoView({
            behavior: "smooth",
            block: "nearest",
        });
    }

    async function toggleModule(name: string, enabled: boolean) {
        await setModuleEnabled(name, enabled);
    }

    function handleWindowClick(e: MouseEvent) {
        if (!searchContainerElement.contains(e.target as Node)) {
            reset();
        }
    }

    onMount(() => {
        searchInputElement.focus();
    });

    listen("toggleModule", (e: any) => {
        const mod = filteredModules.find((m) => m.name === e.moduleName);
        if (!mod) {
            return;
        }
        mod.enabled = e.enabled;
        filteredModules = filteredModules;
    });
</script>

<svelte:window on:click={handleWindowClick} on:contextmenu={handleWindowClick} />

<div
    class="search"
    class:has-results={query}
    bind:this={searchContainerElement}
>
    <input
        type="text"
        class="search-input"
        placeholder="Search"
        spellcheck="false"
        bind:value={query}
        bind:this={searchInputElement}
        on:input={filterModules}
        on:keydown={handleKeyDown}
    />

    {#if query}
        <div class="results">
            {#if filteredModules.length > 0}
                {#each filteredModules as { name, enabled }, index (name)}
                    <!-- svelte-ignore a11y-click-events-have-key-events -->
                    <!-- svelte-ignore a11y-no-static-element-interactions -->
                    <div
                        class="result"
                        class:enabled
                        on:click={() => toggleModule(name, !enabled)}
                        on:contextmenu|preventDefault={() =>
                            dispatch("highlightModule", { name })}
                        class:selected={selectedIndex === index}
                        bind:this={resultElements[index]}
                    >
                        {name}
                    </div>
                {/each}
            {:else}
                <div class="placeholder">No modules found</div>
            {/if}
        </div>
    {/if}
</div>

<style lang="scss">
    @import "../../colors.scss";

    .search {
        position: fixed;
        left: 50%;
        top: 50px;
        transform: translateX(-50%);
        background-color: rgba($clickgui-base-color, 0.9);
        width: 600px;
        border-radius: 30px;
        overflow: hidden;
        transition: ease 0.2s border-radius;
        box-shadow: 0 0 10px rgba($clickgui-base-color, 0.5);
        z-index: 999999999;

        &.has-results {
            border-radius: 10px;
        }
    }

    .results {
        border-top: solid 2px $accent-color;
        padding: 5px 25px;
        max-height: 250px;
        overflow: auto;

        .result {
            color: $clickgui-text-dimmed-color;
            font-size: 16px;
            padding: 10px 0;
            transition:
                ease 0.2s color,
                ease 0.2s padding-left;
            cursor: pointer;
            display: grid;
            grid-template-columns: 1fr max-content;

            &.selected {
                padding-left: 10px;
            }

            &:hover {
                color: $clickgui-text-color;

                &::after {
                    content: "Right-click to locate";
                    color: rgba($clickgui-text-color, 0.4);
                    font-size: 12px;
                }
            }

            &.enabled {
                color: $accent-color;
            }
        }

        .placeholder {
            color: $clickgui-text-dimmed-color;
            font-size: 16px;
            padding: 10px 0;
        }

        &::-webkit-scrollbar {
            width: 0;
        }
    }

    .search-input {
        padding: 15px 25px;
        background-color: transparent;
        border: none;
        font-family: "Inter", sans-serif;
        font-size: 16px;
        color: $clickgui-text-color;
    }
</style>
