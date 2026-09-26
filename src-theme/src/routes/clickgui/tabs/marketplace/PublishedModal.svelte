<script lang="ts">
    import Dialog from "./Dialog.svelte";
    import Button from "./Button.svelte";
    import Tag from "./Tag.svelte";
    import Address from "./Address.svelte";
    import {copyMarketplaceShareCode} from "../../../../integration/rest";
    import type {MarketplacePublished} from "../../../../integration/types";
    import {attempt, notify} from "./marketplace";

    let {open = $bindable(), published, onopen}: {
        open: boolean;
        published: MarketplacePublished | null;
        onopen: (id: number) => void;
    } = $props();

    async function copy() {
        if (published && await attempt(() => copyMarketplaceShareCode(published!!.id))) {
            notify(`Copied ${published.shareCode}`);
        }
    }

    function openPage() {
        if (published) {
            open = false;
            onopen(published.id);
        }
    }
</script>

<Dialog bind:open title="Published" width={440}>
    {#if published}
        <div class="head">
            <span class="address"><Address address={published.address}/></span>
            {#if published.shareCode}
                <Tag text="Unlisted"/>
            {/if}
        </div>
        {#if published.shareCode}
            <div class="code">{published.shareCode}</div>
        {/if}
    {/if}

    {#snippet footer()}
        <Button title="Open" onclick={openPage}/>
        {#if published?.shareCode}
            <Button title="Copy code" primary onclick={copy}/>
        {:else}
            <Button title="Done" primary onclick={() => open = false}/>
        {/if}
    {/snippet}
</Dialog>

<style lang="scss">
  .head {
    display: flex;
    align-items: center;
    column-gap: 8px;
    padding-top: 12px;
  }

  .address {
    font-size: 14px;
    font-weight: 600;
    color: var(--clickgui-text-color);
  }

  .code {
    margin-top: 12px;
    font-family: monospace;
    font-size: 22px;
    font-weight: 600;
    letter-spacing: 3px;
    text-align: center;
    color: var(--clickgui-text-color);
    background-color: var(--clickgui-input-background-color);
    border-bottom: solid 2px var(--clickgui-input-border-color);
    border-radius: 3px;
    padding: 12px;
  }
</style>
