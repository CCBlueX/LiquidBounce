<script lang="ts">
    import type {Component, Snippet} from "svelte";

    interface Props {
        tabs: Tab[];
        activeTab?: number;
        onChangeTab?: (activeTab: number) => void | Promise<void>;
    }

    type Content = Component | Snippet;

    type SubTab = {
        title: string;
        content: Content;
    };

    type Tab = {
        title: string;
        icon: string;
        content: Content | SubTab[];
    };

    let availableTabsElement = $state<HTMLElement | undefined>();
    let activeSubTabs = $state<Record<number, number>>({});

    let {tabs, activeTab = $bindable(0), onChangeTab}: Props = $props();

    let subTabs = $derived(tabs[activeTab]?.content ?? []) as SubTab[];

    const ActiveContent = $derived.by(() => {
        const content = tabs[activeTab]?.content;

        if (Array.isArray(content)) {
            if (content.length === 0) {
                return undefined;
            }

            const activeSubTab = activeSubTabs[activeTab] ?? 0;
            return content[Math.min(activeSubTab, content.length - 1)]?.content;
        }

        return content;
    });

    function setActiveTab(i: number) {
        activeTab = i;
        onChangeTab?.(activeTab);
    }

    function setActiveSubTab(i: number) {
        activeSubTabs[activeTab] = i;
    }
</script>

<div class="tabs">
    <div class="available-tabs" bind:this={availableTabsElement}>
        {#each tabs as {title, icon}, index}
            <button
                    class="tab-button"
                    class:active={index === activeTab}
                    onclick={() => setActiveTab(index)}
                    type="button"
            >
                <img class="icon" src="img/menu/altmanager/{icon}" alt={title}>
                <span>{title}</span>
            </button>
        {/each}
    </div>

    <div style="width: {availableTabsElement?.clientWidth}px">
        {#if Array.isArray(tabs[activeTab]?.content)}
            <div class="available-sub-tabs">
                {#each subTabs as subTab, index (subTab.title)}
                    <button
                            class="sub-tab-button"
                            class:active={index === (activeSubTabs[activeTab] ?? 0)}
                            onclick={() => setActiveSubTab(index)}
                            type="button"
                    >
                        {subTab.title}
                    </button>
                {/each}
            </div>
        {/if}

        <div class="content">
            {#if ActiveContent}
                <ActiveContent/>
            {/if}
        </div>
    </div>
</div>

<style lang="scss">

  .available-tabs {
    display: flex;
    column-gap: var(--space-2);
  }

  .tab-button {
    font-family: "Inter", sans-serif;
    background-color: var(--menu-modal-tab-background-color);
    color: var(--menu-text-dimmed-color);
    padding: 12px 10px;
    border: solid 1px var(--surface-border-color);
    border-radius: var(--radius-sm);
    flex-grow: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    row-gap: var(--space-2);
    cursor: pointer;
    transition: border-color var(--duration-fast) var(--ease-standard),
    background-color var(--duration-fast) var(--ease-standard),
    color var(--duration-fast) var(--ease-standard);
    font-weight: 500;

    .icon {
      height: 28px;
    }

    &:hover {
      color: var(--menu-text-color);
      border-color: var(--accent-border-color);
    }

    &.active {
      color: var(--menu-text-color);
      background-color: var(--accent-subtle-background-color);
      border-color: var(--menu-modal-tab-active-border-color);
    }
  }

  .available-sub-tabs {
    background-color: var(--menu-modal-sub-tab-background-color);
    border: 1px solid var(--surface-border-color);
    border-radius: var(--radius-sm);
    overflow: hidden;
    margin-top: 15px;
    display: flex;
  }

  .sub-tab-button {
    all: unset;
    flex: 1;
    color: var(--menu-text-dimmed-color);
    font-family: "Inter", sans-serif;
    text-align: center;
    padding: 11px 10px;
    border-bottom: solid 2px transparent;
    cursor: pointer;
    font-weight: 500;
    font-size: 14px;
    transition: color var(--duration-fast) var(--ease-standard),
    background-color var(--duration-fast) var(--ease-standard),
    border-color var(--duration-fast) var(--ease-standard);

    &:hover {
      color: var(--menu-text-color);
      background-color: var(--surface-hover-color);
    }

    &:focus-visible {
      outline: var(--focus-ring-width) solid var(--accent-color);
      outline-offset: -3px;
    }

    &.active {
      color: var(--menu-text-color);
      background-color: var(--accent-subtle-background-color);
      border-color: var(--menu-modal-sub-tab-active-border-color);
    }
  }

  .content {
    margin-top: 32px;
  }
</style>
