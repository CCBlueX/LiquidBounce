<script lang="ts">
    import type {GroupedModules, Module} from "../../integration/types";
    import Panel from "./Panel.svelte";
    import Search from "./Search.svelte";
    import Description from "./Description.svelte";
    import {fade} from "svelte/transition";
    import {onMount} from "svelte";
    import {get} from "svelte/store";
    import {getModules} from "../../integration/rest";
    import {groupByCategory} from "../../integration/util";
    import {animatePanels, gridSize, panelHandles, scaleFactor, showGrid} from "./clickgui_store";
    import ScaledClickGuiContent from "./ScaledClickGuiContent.svelte";

    let categories = $state<GroupedModules>({});
    let modules = $state<Module[]>([]);

    type AlignZone = "left" | "right" | "center";

    const ALIGN_GAP = 15;
    const EDGE_MARGIN = 15;
    const CENTER_TOP = 150;

    onMount(async () => {
        modules = await getModules();
        categories = groupByCategory(modules);
    });

    function detectZone(nx: number): AlignZone {
        if (nx < 0.4) return "left";
        if (nx > 0.6) return "right";
        return "center";
    }

    function handleBackgroundDblClick(e: MouseEvent) {
        const nx = e.clientX / window.innerWidth;

        arrangePanels(detectZone(nx));
    }

    function arrangePanels(zone: AlignZone) {
        const handles = get(panelHandles).slice().sort((a, b) => a.index - b.index);
        if (handles.length === 0) {
            return;
        }

        const areaWidth = document.documentElement.clientWidth * (2 / $scaleFactor);
        const areaHeight = document.documentElement.clientHeight * (2 / $scaleFactor);

        const items = handles.map((handle) => ({handle, ...handle.getSize()}));

        $animatePanels = true;

        switch (zone) {
            case "left":
            case "right": {
                let columnEdge = zone === "left" ? EDGE_MARGIN : areaWidth - EDGE_MARGIN;
                let y = EDGE_MARGIN;
                let columnWidth = 0;

                for (const {handle, width, height} of items) {
                    if (y + height > areaHeight - EDGE_MARGIN && y > EDGE_MARGIN) {
                        columnEdge += (zone === "left" ? 1 : -1) * (columnWidth + ALIGN_GAP);
                        y = EDGE_MARGIN;
                        columnWidth = 0;
                    }
                    handle.setPosition(zone === "left" ? columnEdge : columnEdge - width, y);
                    y += height + ALIGN_GAP;
                    columnWidth = Math.max(columnWidth, width);
                }
                break;
            }
            case "center": {
                const layout: { left: number; top: number }[] = [];
                let x = 0;
                let y = 0;
                let rowHeight = 0;
                let blockWidth = 0;

                for (const {width, height} of items) {
                    if (x + width > areaWidth && x > 0) {
                        y += rowHeight + ALIGN_GAP;
                        x = 0;
                        rowHeight = 0;
                    }
                    layout.push({left: x, top: y});
                    x += width + ALIGN_GAP;
                    rowHeight = Math.max(rowHeight, height);
                    blockWidth = Math.max(blockWidth, x - ALIGN_GAP);
                }

                const offsetX = Math.max(0, (areaWidth - blockWidth) / 2);

                items.forEach(({handle}, i) => {
                    handle.setPosition(layout[i].left + offsetX, layout[i].top + CENTER_TOP);
                });
                break;
            }
        }

        setTimeout(() => ($animatePanels = false), 300);
    }
</script>

<ScaledClickGuiContent>
    <div
            class="clickgui"
            class:grid={$showGrid}
            style="background-size: {$gridSize}px {$gridSize}px;"
            transition:fade|global={{duration: 200}}
    >
        <!-- svelte-ignore a11y_no_static_element_interactions -->
        <div
                class="align-catcher"
                ondblclick={handleBackgroundDblClick}
        ></div>

        <Description/>
        <Search modules={structuredClone($state.snapshot(modules))}/>

        {#each Object.entries(categories) as [category, modules], panelIndex (category)}
            <Panel {category} {modules} {panelIndex}/>
        {/each}

        <div class="align-hint">Double-click empty space to align panels</div>
    </div>
</ScaledClickGuiContent>

<style lang="scss">
  .clickgui {
    position: absolute;
    inset: 0;

    &.grid {
      background-image: linear-gradient(to right, var(--clickgui-grid-color) 1px, transparent 1px),
      linear-gradient(to bottom, var(--clickgui-grid-color) 1px, transparent 1px);
    }
  }

  .align-catcher {
    position: fixed;
    inset: 0;
    z-index: 0;
  }

  .align-hint {
    position: fixed;
    bottom: 22px;
    left: 50%;
    z-index: 100000;
    transform: translateX(-50%) translateY(6px);
    padding: 7px 15px;
    border-radius: 999px;
    background-color: var(--clickgui-base-90-color);
    color: var(--clickgui-text-dimmed-color);
    font-size: 12px;
    font-weight: 500;
    letter-spacing: 0.2px;
    white-space: nowrap;
    pointer-events: none;
    box-shadow: 0 0 10px var(--clickgui-base-50-color);
    opacity: 0;
    transition: opacity 0.2s ease, transform 0.2s ease;
  }

  .align-catcher:hover ~ .align-hint {
    opacity: 0.85;
    transform: translateX(-50%) translateY(0);
    transition: opacity 0.3s ease 0.5s, transform 0.3s ease 0.5s;
  }
</style>
