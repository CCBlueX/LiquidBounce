<script lang="ts">
    import Button from "../../../common/Button.svelte";
    import Modal from "../../../common/Modal.svelte";
    import Address from "../Address.svelte";
    import {copyMarketplaceShareCode} from "../../../../../integration/rest";
    import type {MarketplacePublishedConfig} from "../../../../../integration/types";

    let {config, onopen, onclose}: {
        config: MarketplacePublishedConfig;
        onopen: () => void;
        onclose: () => void;
    } = $props();
</script>

<Modal title="Published" width={420} {onclose}>
    <p class="address"><Address address={config.address}/></p>
    {#if config.shareCode}
        <p class="hint">It is unlisted. Others load it with this share code.</p>
        <code class="code">{config.shareCode}</code>
    {/if}

    {#snippet footer()}
        {#if config.shareCode}
            <Button variant="secondary" onclick={() => copyMarketplaceShareCode(config.id).catch(() => null)}>
                Copy share code
            </Button>
        {/if}
        <Button variant="secondary" onclick={onopen}>Open</Button>
        <Button autofocus onclick={onclose}>Done</Button>
    {/snippet}
</Modal>

<style lang="scss">
  .address {
    font-size: 14px;
    font-weight: 600;
  }

  .hint {
    line-height: 1.5;
    color: var(--clickgui-text-dimmed-color);
  }

  .code {
    align-self: flex-start;
    padding: 7px 10px;
    border-radius: 3px;
    background-color: var(--clickgui-input-background-color);
    font-family: monospace;
    font-size: 14px;
    letter-spacing: 2px;
    user-select: text;
  }
</style>
