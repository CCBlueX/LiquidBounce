<script lang="ts">
    import type {Module} from "../../../../integration/types";
    import {setModuleEnabled} from "../../../../integration/rest";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";
    import {byName, modules, searchModules, searchSettings, selectModule, type SettingHit} from "./modules.svelte";

    const SETTING_LIMIT = 300;

    /**
     * Every result that can be walked with the arrow keys, in the order it is shown.
     */
    type Target = { module: Module; hit?: SettingHit };

    let active = $state(0);
    let list = $state<HTMLElement>();

    const found = $derived(searchModules(modules.query));
    const hits = $derived(searchSettings(modules.query, SETTING_LIMIT));

    const groups = $derived.by(() => {
        const byModule = new Map<string, SettingHit[]>();
        for (const hit of hits) {
            byModule.set(hit.module, [...byModule.get(hit.module) ?? [], hit]);
        }

        return modules.categories
            .map(category => ({
                category,
                modules: modules.list
                    .filter(module => module.category === category.name && byModule.has(module.name))
                    .map(module => ({module, hits: byModule.get(module.name)!})),
            }))
            .filter(group => group.modules.length > 0);
    });

    const targets = $derived<Target[]>([
        ...found.map(module => ({module})),
        ...groups.flatMap(group => group.modules.flatMap(entry => entry.hits.map(hit => ({module: entry.module, hit})))),
    ]);

    $effect(() => {
        modules.query;
        active = 0;
    });

    function spaced(name: string) {
        return $spaceSeperatedNames ? convertToSpacedString(name) : name;
    }

    function open(target: Target) {
        // Where the list is once the search is over
        modules.category = target.module.category;
        selectModule(target.module.name, target.hit?.trail ?? [], target.hit?.name ?? null);
    }

    function indexOf(module: Module, hit?: SettingHit) {
        return targets.findIndex(target => target.module === module && target.hit === hit);
    }

    function handleKeyDown(event: KeyboardEvent) {
        if (targets.length === 0 || document.querySelector("dialog[open]")) {
            return;
        }

        const target = targets[active];
        switch (event.key) {
            case "ArrowDown":
                active = (active + 1) % targets.length;
                break;
            case "ArrowUp":
                active = (active - 1 + targets.length) % targets.length;
                break;
            case "Enter":
                // Like a click: a module is switched, a setting is shown
                if (target.hit) {
                    open(target);
                } else {
                    setModuleEnabled(target.module.name, !target.module.enabled);
                }
                break;
            case "Tab":
                open(target);
                break;
            default:
                return;
        }

        event.preventDefault();
        list?.querySelector(`[data-result="${active}"]`)?.scrollIntoView({block: "nearest"});
    }
</script>

<svelte:window onkeydown={handleKeyDown}/>

<header class="head">
    <h2>Results for "{modules.query}"</h2>
    <span class="meta">
        {found.length} {found.length === 1 ? "module" : "modules"},
        {hits.length}{hits.length === SETTING_LIMIT ? "+" : ""} {hits.length === 1 ? "setting" : "settings"}
    </span>
</header>

<div class="results" bind:this={list}>
    {#if targets.length === 0}
        <p class="empty">Nothing found.</p>
    {/if}

    {#if found.length > 0}
        <div class="group">
            <img src="img/clickgui/icon-client.svg" alt="">
            Modules
            <span>{found.length}</span>
        </div>
        {#each found as module (module.name)}
            {@const index = indexOf(module)}
            <button type="button" class="module" class:enabled={module.enabled} class:active={index === active}
                    data-result={index} onclick={() => setModuleEnabled(module.name, !module.enabled)}
                    oncontextmenu={event => { event.preventDefault(); open({module}); }}>
                <b>{spaced(module.name)}</b>
                {#if module.aliases.length > 0}
                    <small>also {module.aliases.map(spaced).join(", ")}</small>
                {/if}
                <span class="category">{module.category}</span>
            </button>
        {/each}
    {/if}

    {#each groups as group (group.category.name)}
        <div class="group">
            <img src={group.category.icon ?? `img/clickgui/icon-${group.category.name.toLowerCase()}.svg`} alt="">
            {group.category.name}
            <span>{group.modules.reduce((count, entry) => count + entry.hits.length, 0)}</span>
        </div>
        {#each group.modules as entry (entry.module.name)}
            <div class="settings">
                <button type="button" class="owner" class:enabled={byName(entry.module.name)?.enabled}
                        onclick={() => open({module: entry.module})}>
                    {spaced(entry.module.name)}
                </button>
                <div class="hits">
                    {#each entry.hits as hit (hit)}
                        {@const index = indexOf(entry.module, hit)}
                        <button type="button" class="hit" class:active={index === active} data-result={index}
                                onclick={() => open({module: entry.module, hit})}>
                            {#if hit.trail.length > 0}
                                <span class="trail">{hit.trail.map(spaced).join(" / ")} /</span>
                            {/if}
                            <b>{spaced(hit.name)}</b>
                        </button>
                    {/each}
                </div>
            </div>
        {/each}
    {/each}
</div>

<style lang="scss">
  .head {
    flex: none;
    height: 56px;
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 0 16px 0 22px;

    h2 {
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 18px;
      font-weight: 600;
    }
  }

  .meta {
    flex: none;
    font-size: 12px;
    color: var(--clickgui-text-muted-color);
  }

  .results {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    padding-bottom: 10px;
  }

  .empty {
    padding: 24px 22px;
    font-size: 12px;
    color: var(--clickgui-text-muted-color);
  }

  .group {
    position: sticky;
    top: 0;
    z-index: 1;
    display: flex;
    align-items: center;
    gap: 9px;
    height: 34px;
    padding: 0 22px;
    margin-top: 6px;
    font-size: 12px;
    font-weight: 600;
    background-color: var(--clickgui-window-header-background-color);
    border-bottom: 2px solid var(--clickgui-window-header-border-color);

    &:first-child {
      margin-top: 0;
    }

    img {
      width: 14px;
      height: 14px;
    }

    span {
      margin-left: auto;
      font-size: 11px;
      font-weight: 500;
      color: var(--clickgui-text-muted-color);
    }
  }

  .module {
    display: flex;
    align-items: center;
    gap: 10px;
    width: 100%;
    height: 34px;
    padding: 0 22px;
    border: none;
    border-bottom: 1px solid var(--clickgui-divider-color);
    background: none;
    font-family: "Inter", sans-serif;
    font-size: 12.5px;
    color: var(--clickgui-text-dimmed-color);
    text-align: left;
    cursor: pointer;
    transition: ease background-color .12s;

    b {
      flex: none;
      font-weight: 600;
      color: var(--clickgui-text-color);
    }

    &.enabled b {
      color: var(--clickgui-module-enabled-color);
    }

    small {
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 11px;
      color: var(--clickgui-text-muted-color);
    }

    &:hover, &.active {
      background-color: var(--clickgui-row-hover-background-color);
    }
  }

  .category {
    flex: none;
    margin-left: auto;
    font-size: 11px;
    color: var(--clickgui-text-muted-color);
  }

  .settings {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    padding: 6px 22px;
    border-bottom: 1px solid var(--clickgui-divider-color);
  }

  .owner {
    flex: none;
    width: 150px;
    padding: 3px 0 0;
    border: none;
    background: none;
    font-family: "Inter", sans-serif;
    font-size: 12.5px;
    font-weight: 600;
    color: var(--clickgui-text-color);
    text-align: left;
    cursor: pointer;

    &.enabled {
      color: var(--clickgui-module-enabled-color);
    }

    &:hover, &:focus-visible {
      text-decoration: underline;
    }
  }

  .hits {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-wrap: wrap;
    gap: 5px;
  }

  .hit {
    max-width: 100%;
    padding: 3px 8px;
    border: none;
    border-radius: 3px;
    background-color: var(--clickgui-segmented-background-color);
    font-family: "Inter", sans-serif;
    font-size: 11.5px;
    font-weight: 500;
    color: var(--clickgui-text-dimmed-color);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    cursor: pointer;
    transition: ease background-color .12s, ease color .12s;

    b {
      font-weight: 600;
      color: var(--clickgui-text-color);
    }

    &:hover {
      background-color: var(--clickgui-control-hover-background-color);
    }

    &.active {
      background-color: var(--clickgui-selected-background-color);
      color: var(--clickgui-text-color);
    }
  }

  .trail {
    opacity: .8;
  }

  @container workspace (max-width: 819px) {
    .head, .group, .module, .settings {
      padding-left: 14px;
      padding-right: 12px;
    }

    .owner {
      width: 110px;
    }
  }
</style>
