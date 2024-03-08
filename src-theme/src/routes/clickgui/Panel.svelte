<script lang="ts">
    import { afterUpdate, onMount } from "svelte";
    import type { Module as TModule } from "../../integration/types";
    import { listen } from "../../integration/ws";
    import Module from "./Module.svelte";

    export let category: string;
    export let modules: TModule[];
    export let maxZIndex: number;
    export let panelIndex: number;
    export let highlightModuleName: string;

    let panelElement: HTMLElement;
    let modulesElement: HTMLElement;

    interface PanelConfig {
        top: number;
        left: number;
        expanded: boolean;
        scrollTop: number;
    }

    function clamp(number: number, min: number, max: number) {
        return Math.max(min, Math.min(number, max));
    }

    function loadPanelConfig(): PanelConfig {
        const localStorageItem = localStorage.getItem(
            `clickgui.panel.${category}`,
        );

        if (!localStorageItem) {
            return {
                top: panelIndex * 50 + 20,
                left: 20,
                expanded: false,
                scrollTop: 0,
            };
        } else {
            const config = JSON.parse(localStorageItem);

            if (config.expanded) {
                renderedModules = modules;
            }

            return config;
        }
    }

    function savePanelConfig() {
        localStorage.setItem(
            `clickgui.panel.${category}`,
            JSON.stringify(panelConfig),
        );
    }

    function fixPosition() {
        panelConfig.left = clamp(panelConfig.left, 0, document.documentElement.clientWidth - panelElement.offsetWidth);
        panelConfig.top = clamp(panelConfig.top, 0, document.documentElement.clientHeight -panelElement.offsetHeight);
    }

    let renderedModules: TModule[] = [];

    let moving = false;
    let prevX = 0;
    let prevY = 0;
    let zIndex = maxZIndex;

    function onMouseDown() {
        moving = true;

        zIndex = ++maxZIndex;
    }

    function onMouseMove(e: MouseEvent) {
        if (moving) {
            panelConfig.left += e.screenX - prevX;
            panelConfig.top += e.screenY - prevY;
        }

        prevX = e.screenX;
        prevY = e.screenY;

        fixPosition();
        savePanelConfig();
    }

    function onMouseUp() {
        moving = false;
    }

    function toggleExpanded() {
        if (panelConfig.expanded) {
            renderedModules = [];
        } else {
            renderedModules = modules;
        }

        panelConfig.expanded = !panelConfig.expanded;

        setTimeout(() => {
            fixPosition();
            savePanelConfig();
        }, 500);
    }

    function handleModulesScroll() {
        panelConfig.scrollTop = modulesElement.scrollTop;
        savePanelConfig();
    }

    afterUpdate(() => {
        const highlightModule = modules.find(
            (m) => m.name === highlightModuleName,
        );
        if (highlightModule) {
            panelConfig.expanded = true;
            renderedModules = modules;
            savePanelConfig();
        }
    });

    listen("toggleModule", (e: any) => {
        const moduleName = e.moduleName;
        const moduleEnabled = e.enabled;

        const mod = modules.find((m) => m.name === moduleName);
        if (!mod) return;

        mod.enabled = moduleEnabled;
        modules = modules;
        if (panelConfig.expanded) {
            renderedModules = modules;
        }
    });

    onMount(() => {
        setTimeout(() => {
            if (!modulesElement) {
                return;
            }

            modulesElement.scrollTo({
                top: panelConfig.scrollTop,
                behavior: "smooth"
            })
        }, 500);
    });

    const panelConfig = loadPanelConfig();
</script>

<svelte:window on:mouseup={onMouseUp} on:mousemove={onMouseMove} />

<div
    class="panel"
    style="left: {panelConfig.left}px; top: {panelConfig.top}px; z-index: {zIndex};"
    bind:this={panelElement}
>
    <!-- svelte-ignore a11y-no-static-element-interactions -->
    <div
        class="title"
        on:mousedown={onMouseDown}
        on:contextmenu|preventDefault={toggleExpanded}
    >
        <img
            class="icon"
            src="img/clickgui/icon-{category.toLowerCase()}.svg"
            alt="icon"
        />
        <span class="category">{category}</span>

        <button class="expand-toggle" on:click={toggleExpanded}>
            <div class="icon" class:expanded={panelConfig.expanded}></div>
        </button>
    </div>

    <div class="modules" on:scroll={handleModulesScroll} bind:this={modulesElement}>
        {#each renderedModules as { name, enabled, description } (name)}
            <Module {name} {enabled} {description} highlight={name === highlightModuleName} />
        {/each}
    </div>
</div>

<style lang="scss">
    @import "../../colors.scss";

    .panel {
        border-radius: 5px;
        width: 225px;
        position: absolute;
        overflow: hidden;
        box-shadow: 0 0 10px rgba($clickgui-base-color, 0.5);
    }

    .title {
        display: grid;
        grid-template-columns: max-content 1fr max-content;
        align-items: center;
        column-gap: 12px;
        background-color: rgba($clickgui-base-color, 0.9);
        border-bottom: solid 2px $accent-color;
        padding: 10px 15px;
        cursor: grab;

        .category {
            font-size: 14px;
            color: $clickgui-text-color;
            font-weight: 500;
        }
    }

    .modules {
        max-height: 545px;
        overflow-y: auto;
        overflow-x: hidden;
        background-color: rgba($clickgui-base-color, 0.8);
    }

    .modules::-webkit-scrollbar {
        width: 0;
    }

    .expand-toggle {
        background-color: transparent;
        border: none;
        cursor: pointer;

        .icon {
            height: 12px;
            width: 12px;
            position: relative;

            &::before {
                content: "";
                position: absolute;
                background-color: white;
                transition: transform 0.4s ease-out;
                top: 0;
                left: 50%;
                width: 2px;
                height: 100%;
                margin-left: -1px;
            }

            &::after {
                content: "";
                position: absolute;
                background-color: white;
                transition: transform 0.4s ease-out;
                top: 50%;
                left: 0;
                width: 100%;
                height: 2px;
                margin-top: -1px;
            }

            &.expanded {
                &::before {
                    transform: rotate(90deg);
                }

                &::after {
                    transform: rotate(180deg);
                }
            }
        }
    }
</style>
