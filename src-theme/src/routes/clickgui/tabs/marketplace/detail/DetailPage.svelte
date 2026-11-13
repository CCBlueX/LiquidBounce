<script lang="ts">
    import type {Snippet} from "svelte";
    import Button from "../../../common/Button.svelte";
    import Gallery from "../../../common/Gallery.svelte";
    import Markdown from "../../../common/Markdown.svelte";
    import Address from "../Address.svelte";
    import Placeholder from "../Placeholder.svelte";
    import {openMarketplaceLink} from "../../../../../integration/rest";
    import {parseMarkdown} from "../../../../../util/markdown";

    /**
     * The page of the config or item [id]: its head, the images of its description as a gallery, the
     * description, then [children]. Until [address] is known it shows nothing, or [error] with a retry.
     */
    let {
        id,
        noun,
        error,
        onretry,
        address,
        name,
        image,
        fallbackImage,
        description = "",
        badges,
        subtitle,
        actions,
        children
    }: {
        id: number;
        noun: string;
        error: string | null;
        onretry: () => void;
        address?: string;
        name?: string;
        image?: string;
        fallbackImage: string;
        description?: string;
        badges?: Snippet;
        subtitle?: Snippet;
        actions?: Snippet;
        children?: Snippet;
    } = $props();

    let imageFailed = $state(false);

    const markdown = $derived(parseMarkdown(description, name));
</script>

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

        <Gallery images={markdown.images}/>
        {#if markdown.blocks.length > 0}
            <!-- The client tells the player when it refuses a link -->
            <Markdown tokens={markdown.blocks} onlink={url => openMarketplaceLink(id, url).catch(() => null)}/>
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
  .page {
    flex: 1;
    display: flex;
    flex-direction: column;
    row-gap: 16px;
    min-height: 0;
    padding: 14px 14px 18px;
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
    width: 48px;
    height: 48px;
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
    margin-top: 5px;
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
</style>
