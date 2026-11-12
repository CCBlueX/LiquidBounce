<script lang="ts" module>
    export interface NavItem {
        id: string;
        label: string;
        icon?: string;
        /**
         * Shown at the end of the item, accented unless it is zero.
         */
        count?: number;
        /**
         * Marks the item as holding something that differs from its default.
         */
        changed?: boolean;
        /**
         * Sets the item off from the ones above it.
         */
        separated?: boolean;
    }
</script>

<script lang="ts">
    /**
     * The navigation at the left of an area. With icons on every item it folds into a rail of icons when the
     * window is narrow, without them it only gets slimmer.
     */
    let {items, active, label, onselect}: {
        items: NavItem[];
        active: string;
        label: string;
        onselect: (id: string) => void;
    } = $props();

    const rail = $derived(items.every(item => item.icon));

    function showFallbackIcon(event: Event) {
        (event.currentTarget as HTMLImageElement).src = "img/clickgui/icon-client.svg";
    }
</script>

<nav class="nav" class:rail aria-label={label}>
    {#each items as item (item.id)}
        {#if item.separated}
            <div class="separator"></div>
        {/if}
        <button type="button" class="item" class:active={item.id === active} title={item.label}
                aria-current={item.id === active} onclick={() => onselect(item.id)}>
            {#if item.icon}
                <img src={item.icon} alt="" onerror={showFallbackIcon}>
            {/if}
            <span class="label">{item.label}</span>
            {#if item.changed}
                <span class="changed" title="Changed from default"></span>
            {/if}
            {#if item.count !== undefined}
                <span class="count" class:none={item.count === 0}>{item.count}</span>
            {/if}
        </button>
    {/each}
</nav>

<style lang="scss">
  .nav {
    flex: none;
    width: 196px;
    display: flex;
    flex-direction: column;
    padding: 10px 0;
    border-right: 1px solid var(--clickgui-divider-color);
    overflow-y: auto;
    scrollbar-width: none;
  }

  .item {
    position: relative;
    flex: none;
    display: flex;
    align-items: center;
    gap: 11px;
    width: 100%;
    height: 38px;
    padding: 0 14px 0 16px;
    border: none;
    border-left: 2px solid transparent;
    background: none;
    font-family: "Inter", sans-serif;
    font-size: 13px;
    font-weight: 500;
    color: var(--clickgui-text-dimmed-color);
    text-align: left;
    cursor: pointer;
    transition: ease background-color .15s, ease color .15s, ease border-color .15s;

    img {
      flex: none;
      width: 16px;
      height: 16px;
      opacity: .9;
    }

    &:hover, &:focus-visible {
      color: var(--clickgui-text-color);
      background-color: var(--clickgui-row-hover-background-color);
    }

    &.active {
      color: var(--clickgui-text-color);
      background-color: var(--accent-subtle-background-color);
      border-left-color: var(--accent-color);
    }
  }

  .label {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .count {
    font-size: 11px;
    font-weight: 600;
    color: var(--accent-color);

    &.none {
      font-weight: 500;
      color: var(--clickgui-text-muted-color);
    }
  }

  .changed {
    flex: none;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background-color: var(--clickgui-changed-color);
  }

  .separator {
    flex: none;
    height: 1px;
    margin: 8px 14px;
    background-color: var(--clickgui-divider-color);
  }

  @container workspace (max-width: 1099px) {
    .nav {
      width: 148px;
    }

    .item {
      gap: 8px;
      padding: 0 10px 0 12px;
    }

    .nav.rail {
      width: 52px;
      padding: 8px 0;

      .item {
        justify-content: center;
        padding: 0 2px 0 0;
      }

      .label {
        display: none;
      }

      .count, .changed {
        position: absolute;
        right: 6px;
        top: 5px;
        font-size: 10px;
      }

      .count.none {
        display: none;
      }

      .separator {
        margin: 6px 10px;
      }
    }
  }
</style>
