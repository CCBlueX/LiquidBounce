<script lang="ts">
    import Button from "../../common/Button.svelte";
    import Chip from "../../common/Chip.svelte";
    import Switch from "../../setting/common/Switch.svelte";
    import {loginClientUser} from "../../../../integration/rest";
    import {
        type ConfigFilters,
        currentServer,
        dialog,
        type ListedType,
        SORTS,
        TYPE_TITLES,
        user
    } from "./marketplace";

    /**
     * What the marketplace lists and how. The filters only show while [browsing].
     */
    let {type, filters = $bindable(), tags, browsing, onselect}: {
        type: ListedType;
        filters: ConfigFilters;
        tags: string[];
        browsing: boolean;
        onselect: (type: ListedType) => void;
    } = $props();

    function toggleTag(tag: string) {
        filters.tags = filters.tags.includes(tag)
            ? filters.tags.filter(other => other !== tag)
            : [...filters.tags, tag];
    }
</script>

<aside class="sidebar">
    <nav class="types">
        {#each Object.entries(TYPE_TITLES) as [listed, title] (listed)}
            <button type="button" class="type" class:active={listed === type}
                    onclick={() => onselect(listed as ListedType)}>
                {title}
            </button>
        {/each}
    </nav>

    {#if browsing}
        <div class="filters">
            <div class="filter">
                <span class="label">Sort by</span>
                <div class="chips">
                    {#each Object.keys(SORTS) as sort (sort)}
                        <Chip active={filters.sort === sort} onclick={() => filters.sort = sort}>{sort}</Chip>
                    {/each}
                </div>
            </div>
            {#if type === "Config"}
                <Switch name="Featured only" bind:value={filters.featured}/>
                {#if $currentServer}
                    <Switch name="For {$currentServer}" bind:value={filters.onServer}/>
                {/if}
                {#if tags.length > 0}
                    <div class="filter">
                        <span class="label">Tags</span>
                        <div class="chips">
                            {#each tags as tag (tag)}
                                <Chip active={filters.tags.includes(tag)} onclick={() => toggleTag(tag)}>
                                    {tag}
                                </Chip>
                            {/each}
                        </div>
                    </div>
                {/if}
            {/if}
        </div>
    {/if}

    <div class="account">
        {#if !$user}
            <p class="hint">Log in to report and publish configs.</p>
            <Button variant="secondary" onclick={() => loginClientUser()}>Log in</Button>
        {:else if type === "Config"}
            <Button onclick={() => $dialog = {kind: "publish"}}>Publish config...</Button>
        {/if}
    </div>
</aside>

<style lang="scss">
  .sidebar {
    display: flex;
    flex-direction: column;
    row-gap: 14px;
    min-height: 0;
    padding: 14px;
    border-right: solid 1px var(--clickgui-marketplace-divider-color);
    overflow-y: auto;

    &::-webkit-scrollbar {
      width: 0;
    }
  }

  .types {
    display: flex;
    flex-direction: column;
    row-gap: 2px;
  }

  .type {
    padding: 9px 12px;
    border: none;
    border-left: solid 2px transparent;
    border-radius: 3px;
    background-color: transparent;
    font-family: "Inter", sans-serif;
    font-size: 13px;
    font-weight: 500;
    text-align: left;
    color: var(--clickgui-text-dimmed-color);
    cursor: pointer;
    transition: ease background-color 0.2s, ease color 0.2s;

    &:hover, &:focus-visible {
      color: var(--clickgui-text-color);
      background-color: var(--clickgui-marketplace-nav-hover-background-color);
    }

    &.active {
      color: var(--clickgui-text-color);
      background-color: var(--clickgui-marketplace-nav-active-background-color);
      border-left-color: var(--clickgui-marketplace-nav-active-border-color);
    }
  }

  .filters {
    display: flex;
    flex-direction: column;
    row-gap: 12px;
    padding-top: 14px;
    border-top: solid 1px var(--clickgui-marketplace-divider-color);
  }

  .filter {
    display: flex;
    flex-direction: column;
    row-gap: 7px;
  }

  .label {
    font-size: 12px;
    font-weight: 600;
    color: var(--clickgui-text-color);
  }

  .chips {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  .account {
    display: flex;
    flex-direction: column;
    row-gap: 8px;
    margin-top: auto;
  }

  .hint {
    font-size: 12px;
    font-weight: 500;
    line-height: 1.5;
    color: var(--clickgui-text-dimmed-color);
  }
</style>
