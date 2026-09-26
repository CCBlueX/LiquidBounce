<script lang="ts">
    import type {Component, Snippet} from "svelte";

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

    let {tabs, activeTab = $bindable(0), onChangeTab}: {
        tabs: Tab[];
        activeTab?: number;
        onChangeTab?: (activeTab: number) => void | Promise<void>;
    } = $props();

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
                {#each tabs[activeTab].content as subTab, index (subTab.title)}
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
    column-gap: 10px;
  }

  .tab-button {
    font-family: "Inter", sans-serif;
    background-color: var(--menu-modal-tab-background-color);
    color: var(--menu-text-color);
    padding: 10px;
    border: solid 2px transparent;
    border-radius: 5px;
    flex-grow: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    row-gap: 10px;
    cursor: pointer;
    transition: ease border-color .2s;
    font-weight: 500;

    .icon {
      height: 30px;
    }

    &.active {
      border-color: var(--menu-modal-tab-active-border-color);
    }
  }

  .available-sub-tabs {
    background-color: var(--menu-modal-sub-tab-background-color);
    border-radius: 5px;
    overflow: hidden;
    margin-top: 15px;
    display: flex;
  }

  .sub-tab-button {
    all: unset;
    flex: 1;
    color: var(--menu-text-color);
    font-family: "Inter", sans-serif;
    text-align: center;
    padding: 10px;
    border-bottom: solid 3px transparent;
    cursor: pointer;
    font-weight: 500;
    font-size: 14px;

    &.active {
      border-color: var(--menu-modal-sub-tab-active-border-color);
    }
  }

  .content {
    margin-top: 40px;
  }
</style>
