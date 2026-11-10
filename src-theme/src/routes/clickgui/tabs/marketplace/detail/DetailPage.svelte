<script lang="ts" module>
    export interface Fact {
        label: string;
        value?: string;
        onclick?: () => void;
    }
</script>

<script lang="ts">
    import type {Snippet} from "svelte";
    import Button from "../../../common/Button.svelte";
    import Gallery from "../../../common/Gallery.svelte";
    import Markdown from "../../../common/Markdown.svelte";
    import Address from "../Address.svelte";
    import Placeholder from "../Placeholder.svelte";
    import {parseMarkdown} from "../../../../../util/markdown";

    /**
     * The page of a config or item: its head, [facts], the description with its images as a gallery, then
     * [children]. Until [address] is known it shows nothing, or [error] with a retry. A fact without a value
     * is left out.
     */
    let {
        noun,
        error,
        onretry,
        onback,
        address,
        name,
        image,
        fallbackImage,
        description = "",
        facts = [],
        badges,
        subtitle,
        actions,
        children
    }: {
        noun: string;
        error: string | null;
        onretry: () => void;
        onback: () => void;
        address?: string;
        name?: string;
        image?: string;
        fallbackImage: string;
        description?: string;
        facts?: Fact[];
        badges?: Snippet;
        subtitle?: Snippet;
        actions?: Snippet;
        children?: Snippet;
    } = $props();

    let imageFailed = $state(false);

    const markdown = $derived(parseMarkdown(description, name));
</script>

<div class="bar">
    <button type="button" class="back" onclick={onback}>Back</button>
</div>

<div class="page">
    {#if address}
        <div class="head">
            <img class="image" src={imageFailed || !image ? fallbackImage : image} alt="" draggable="false"
                 onerror={() => imageFailed = true}/>
            <div class="text">
                <div class="heading">
                    <h2 class="name"><Address {address}/></h2>
                    {@render badges?.()}
                </div>
                {#if subtitle}
                    <div class="subtitle">{@render subtitle()}</div>
                {/if}
            </div>
            {#if actions}
                <div class="actions">{@render actions()}</div>
            {/if}
        </div>

        <dl class="facts">
            {#each facts.filter(fact => fact.value !== undefined) as fact (fact.label)}
                <div class="fact">
                    <dt>{fact.label}</dt>
                    <dd>
                        {#if fact.onclick}
                            <button type="button" class="link" onclick={fact.onclick}>{fact.value}</button>
                        {:else}
                            {fact.value}
                        {/if}
                    </dd>
                </div>
            {/each}
        </dl>

        <Gallery images={markdown.images}/>
        {#if markdown.blocks.length > 0}
            <Markdown tokens={markdown.blocks}/>
        {/if}

        {@render children?.()}
    {:else if error}
        <Placeholder title="Couldn't open this {noun}" text={error}>
            {#snippet actions()}
                <Button onclick={onretry}>Retry</Button>
            {/snippet}
        </Placeholder>
    {/if}
</div>

<style lang="scss">
  .bar {
    padding: 14px 14px 10px;
  }

  .back {
    display: flex;
    align-items: center;
    column-gap: 8px;
    padding: 0;
    border: none;
    background: none;
    font-family: "Inter", sans-serif;
    font-size: 12px;
    font-weight: 500;
    color: var(--clickgui-text-dimmed-color);
    cursor: pointer;
    transition: ease color 0.2s;

    &::before {
      content: "";
      width: 14px;
      height: 14px;
      background: url("/img/menu/icon-back.svg") center / contain no-repeat;
      opacity: 0.8;
    }

    &:hover, &:focus-visible {
      color: var(--clickgui-text-color);
    }
  }

  .page {
    flex: 1;
    display: flex;
    flex-direction: column;
    row-gap: 14px;
    min-height: 0;
    padding: 0 14px 18px;
    overflow-y: auto;
    scrollbar-gutter: stable;

    > :global(*) {
      flex: none;
    }

    &::-webkit-scrollbar {
      width: 4px;
    }
  }

  .head {
    display: flex;
    align-items: center;
    column-gap: 14px;
  }

  .image {
    flex: none;
    width: 56px;
    height: 56px;
    border-radius: 5px;
    object-fit: cover;
  }

  .text {
    flex: 1;
    min-width: 0;
  }

  .heading {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 6px;
  }

  .name {
    font-size: 18px;
    font-weight: 600;
    color: var(--clickgui-text-color);
    overflow-wrap: anywhere;
  }

  .subtitle {
    margin-top: 6px;
    font-size: 12px;
    font-weight: 500;
    color: var(--clickgui-text-dimmed-color);
  }

  .actions {
    flex: none;
    display: flex;
    align-items: center;
    column-gap: 6px;
  }

  .facts {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 10px 16px;
    padding: 12px 0;
    border-top: solid 1px var(--clickgui-marketplace-divider-color);
    border-bottom: solid 1px var(--clickgui-marketplace-divider-color);
  }

  dt {
    margin-bottom: 3px;
    font-size: 12px;
    font-weight: 500;
    color: var(--clickgui-text-dimmed-color);
  }

  dd {
    font-size: 13px;
    font-weight: 600;
    color: var(--clickgui-text-color);
    overflow-wrap: anywhere;
  }

  .link {
    padding: 0;
    border: none;
    background: none;
    font: inherit;
    color: var(--clickgui-marketplace-link-color);
    cursor: pointer;

    &:hover, &:focus-visible {
      text-decoration: underline;
    }
  }
</style>
