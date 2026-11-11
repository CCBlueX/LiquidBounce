<script lang="ts">
    import type {Module} from "../../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";
    import Segmented from "../../setting/common/Segmented.svelte";
    import ModuleRow from "./ModuleRow.svelte";
    import {BINDS, isBound, type ListFilter, modules} from "./modules.svelte";

    const FILTERS: ListFilter[] = ["All", "Enabled", "Bound"];

    let grid = $state<HTMLElement>();
    let hovered = $state<Module | null>(null);

    const inCategory = $derived(modules.category === BINDS
        ? modules.list.filter(isBound)
        : modules.list.filter(module => module.category === modules.category));

    const shown = $derived(inCategory.filter(module => modules.filter === "All"
        || (modules.filter === "Enabled" && module.enabled)
        || (modules.filter === "Bound" && isBound(module))));

    const enabled = $derived(inCategory.filter(module => module.enabled).length);

    function spaced(name: string) {
        return $spaceSeperatedNames ? convertToSpacedString(name) : name;
    }

    type Place = { left: number; top: number; column: number };

    /**
     * Where every row was when the list was last laid out. The columns change when the pane opens or closes
     * and when the filter changes. A row that keeps its column then glides to its new place; one that lands in
     * another column would cross the others on its way, so it fades in where it belongs.
     */
    let places = new Map<string, Place>();
    let placedCategory = "";
    let placedWidth = 0;

    function settle() {
        if (!grid) {
            return;
        }

        const rows = [...grid.children] as HTMLElement[];
        const columns = [...new Set(rows.map(row => row.offsetLeft))].sort((left, right) => left - right);
        const next = new Map<string, Place>();
        const sameCategory = placedCategory === modules.category;
        // A row that fades in waits until the gliding ones have passed its place. Wider means the pane is
        // leaving, and it fades out over the columns that rows now move into, which takes longer.
        const delay = grid.clientWidth > placedWidth ? 90 : 60;

        for (const row of rows) {
            const name = row.dataset.module!;
            const place = {left: row.offsetLeft, top: row.offsetTop, column: columns.indexOf(row.offsetLeft)};
            next.set(name, place);

            const before = places.get(name);
            // Another category is another list, nothing in it has moved
            if (!sameCategory || (before && before.left === place.left && before.top === place.top)) {
                continue;
            }

            if (before && before.column === place.column) {
                row.animate([
                    {transform: `translate(${before.left - place.left}px, ${before.top - place.top}px)`},
                    {transform: "none"},
                ], {duration: 200, easing: "cubic-bezier(0.23, 1, 0.32, 1)"});
            } else {
                row.animate([{opacity: 0}, {opacity: 1}], {duration: 140, delay, easing: "ease-out", fill: "backwards"});
            }
        }

        places = next;
        placedCategory = modules.category;
        placedWidth = grid.clientWidth;
    }

    $effect(() => {
        // Rows are in the page by the time an effect runs
        shown;
        settle();
    });

    $effect(() => {
        if (!grid) {
            return;
        }

        // Runs after layout and before the frame is painted, so a moved row is never seen at its new place first
        const observer = new ResizeObserver(settle);
        observer.observe(grid);
        return () => observer.disconnect();
    });
</script>

<header class="head">
    <h2>{modules.category}</h2>
    <span class="meta">{inCategory.length} modules, {enabled} enabled</span>
    <div class="filter">
        <Segmented options={FILTERS} bind:value={modules.filter}/>
    </div>
</header>

<div class="scroller">
    <div class="grid" role="list" bind:this={grid}>
        {#each shown as module (module.name)}
            <ModuleRow {module} onhover={next => hovered = next}/>
        {/each}
    </div>
    {#if shown.length === 0}
        <p class="empty">
            {modules.filter === "Enabled" ? "Nothing here is enabled." : "Nothing here has a key."}
        </p>
    {/if}
</div>

<footer class="foot">
    {#if hovered}
        <b>{spaced(hovered.name)}</b>
        <span class="description">{hovered.description}</span>
        {#if hovered.aliases.length > 0}
            <span class="aliases">Also found as {hovered.aliases.map(spaced).join(", ")}</span>
        {/if}
    {:else}
        <span class="aliases">Left click turns a module on or off, right click opens its settings.</span>
    {/if}
</footer>

<style lang="scss">
  .head {
    flex: none;
    height: 56px;
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 0 16px 0 22px;

    h2 {
      font-size: 18px;
      font-weight: 600;
    }
  }

  .meta {
    font-size: 12px;
    color: var(--clickgui-text-muted-color);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .filter {
    flex: none;
    margin-left: auto;
  }

  .scroller {
    container: module-list / inline-size;
    flex: 1;
    min-height: 0;
    // Rows on their way to a new place must not make the list scroll sideways
    overflow-x: hidden;
    overflow-y: auto;
    padding: 2px 12px 0;
  }

  .grid {
    column-count: 1;
    column-gap: 6px;
  }

  @container module-list (min-width: 380px) {
    .grid {
      column-count: 2;
    }
  }

  @container module-list (min-width: 560px) {
    .grid {
      column-count: 3;
    }
  }

  @container module-list (min-width: 900px) {
    .grid {
      column-count: 4;
    }
  }

  .empty {
    padding: 24px 10px;
    font-size: 12px;
    color: var(--clickgui-text-muted-color);
  }

  .foot {
    flex: none;
    height: 46px;
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 0 22px;
    border-top: 1px solid var(--clickgui-divider-color);
    font-size: 12px;
    color: var(--clickgui-text-dimmed-color);
    white-space: nowrap;

    b {
      flex: none;
      font-weight: 600;
      color: var(--clickgui-text-color);
    }
  }

  .description {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .aliases {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    color: var(--clickgui-text-muted-color);
  }

  @container workspace (max-width: 819px) {
    .head {
      padding: 0 10px 0 14px;
    }

    .meta {
      display: none;
    }
  }
</style>
