<script lang="ts">
    import type {Snippet} from "svelte";
    import ListRow from "../../../common/ListRow.svelte";
    import Notice from "../../../common/Notice.svelte";
    import PillButton from "../../../common/PillButton.svelte";

    interface Stat {
        label: string;
        value?: string;
        onclick?: () => void;
    }

    /**
     * The page of a config or item: a large row as its head, then [stats], [description] and [children]. Until
     * it is [loaded] it shows nothing, or [error] with a retry. A stat without a value is left out.
     */
    let {
        noun,
        loaded,
        error,
        onretry,
        onback,
        image,
        title,
        address,
        by,
        badges = [],
        subtitle,
        actions,
        stats = [],
        description,
        children
    }: {
        noun: string;
        loaded: boolean;
        error: string | null;
        onretry: () => void;
        onback: () => void;
        image?: string;
        title?: string;
        address?: string;
        by?: string;
        badges?: string[];
        subtitle?: string;
        actions?: Snippet;
        stats?: Stat[];
        description?: string;
        children?: Snippet;
    } = $props();
</script>

<div class="detail">
    {#if loaded}
        <div class="head">
            <button type="button" class="back" aria-label="Back" onclick={onback}></button>
            <ListRow large {image} {title} {address} {by} {badges} {subtitle} {actions}/>
        </div>

        <div class="body">
            <div class="stats">
                {#each stats.filter(stat => stat.value !== undefined) as stat (stat.label)}
                    <div class="stat">
                        <span class="value">
                            {#if stat.onclick}
                                <button type="button" class="link" onclick={stat.onclick}>{stat.value}</button>
                            {:else}
                                {stat.value}
                            {/if}
                        </span>
                        <span class="label">{stat.label}</span>
                    </div>
                {/each}
            </div>

            {#if description}
                <p class="description">{description}</p>
            {/if}

            {@render children?.()}
        </div>
    {:else if error}
        <div class="head">
            <button type="button" class="back" aria-label="Back" onclick={onback}></button>
            <ListRow large title="{noun[0].toUpperCase()}{noun.slice(1)}"/>
        </div>
        <Notice title="Couldn't open this {noun}">
            {error}
            {#snippet actions()}
                <PillButton title="Retry" primary onclick={onretry}/>
            {/snippet}
        </Notice>
    {/if}
</div>

<style lang="scss">
  .detail {
    flex: 0 1 auto;
    min-height: 0;
    display: flex;
    flex-direction: column;
    background-color: var(--clickgui-window-background-color);
    border-radius: 5px;
    box-shadow: 0 0 10px var(--clickgui-window-shadow-color);
    overflow: hidden;
  }

  .head {
    display: flex;
    align-items: center;
    column-gap: 12px;
    background-color: var(--clickgui-window-header-background-color);
    border-bottom: solid 2px var(--clickgui-window-header-border-color);
    padding: 12px 18px;
  }

  .back {
    width: 24px;
    height: 24px;
    border: none;
    border-radius: 999px;
    cursor: pointer;
    background: url("/img/clickgui/icon-settings-expand.svg") center / 11px no-repeat;
    transform: rotate(90deg);
    opacity: .7;
    transition: ease opacity .2s, ease background-color .2s;

    &:hover {
      opacity: 1;
      background-color: var(--clickgui-tab-hover-background-color);
    }
  }








  .body {
    flex: 0 1 auto;
    min-height: 0;
    overflow: auto;
    padding: 14px 20px 20px;

    &::-webkit-scrollbar {
      width: 2px;
    }
  }

  .stats {
    display: flex;
    flex-wrap: wrap;
    gap: 10px 32px;
    padding: 12px 0 4px;
  }

  .stat {
    display: flex;
    flex-direction: column;
    row-gap: 3px;
  }

  .value {
    font-size: 14px;
    font-weight: 600;
    color: var(--clickgui-text-color);
  }

  .label {
    font-size: 10px;
    font-weight: 600;
    letter-spacing: 1px;
    text-transform: uppercase;
    color: var(--clickgui-text-dimmed-color);
  }

  .link {
    all: unset;
    cursor: pointer;
    color: var(--accent-color);

    &:hover {
      text-decoration: underline;
    }
  }

  .description {
    font-size: 12px;
    font-weight: 500;
    line-height: 1.5;
    color: var(--clickgui-text-dimmed-color);
    white-space: pre-wrap;
    margin: 12px 0 0;
    max-width: 820px;
  }
</style>
