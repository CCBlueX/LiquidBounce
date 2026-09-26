<script lang="ts">
    import Row from "./Row.svelte";
    import Tag from "./Tag.svelte";
    import Button from "./Button.svelte";
    import Address from "./Address.svelte";
    import type {MarketplaceConfig} from "../../../../integration/types";
    import {ago, count, reports, UNKNOWN_SERVER} from "./marketplace";

    let {config, loggedIn, onload, onopen, onreport}: {
        config: MarketplaceConfig;
        loggedIn: boolean;
        onload: () => void;
        onopen: () => void;
        onreport: (works: boolean) => void;
    } = $props();

    const details = $derived([
        `Updated ${ago(config.updatedAt)}`,
        config.servers.length > 1 ? `${config.servers[0]} +${config.servers.length - 1}` : config.servers[0] ?? "any server",
        config.protocol && !config.protocolMatches ? `made on ${config.protocol}` : config.protocol
    ].filter(Boolean).join(" · "));
</script>

{#snippet report()}
    <Button title="Works" onclick={() => onreport(true)}/>
    <Button title="Broken" onclick={() => onreport(false)}/>
{/snippet}

<Row image={config.image ?? UNKNOWN_SERVER} onclick={onopen} active={config.tracking !== "None"}
     hover={loggedIn ? report : undefined}>
    {#snippet title()}<Address address={config.address}/>{/snippet}

    {#snippet tags()}
        {#if config.tracking !== "None"}
            <Tag text={config.tracking === "Editing" ? "Edited" : "Tracked"}/>
        {/if}
        {#if config.featured}
            <Tag text="Featured"/>
        {/if}
        {#if config.own}
            <Tag text="Yours"/>
        {/if}
        {#if config.visibility === "unlisted"}
            <Tag text="Unlisted"/>
        {/if}
        {#if config.overlayOn}
            <Tag text="Overlay on {config.overlayOn.address}"/>
        {/if}
        {#if config.binds}
            <Tag text="Binds"/>
        {/if}
        {#each config.tags as tag (tag)}
            <Tag text={tag}/>
        {/each}
    {/snippet}

    {#snippet subtitle()}{details}{/snippet}

    {#snippet meta()}
        <span class="reports">{reports(config.works, config.fails)}</span>
        <span class="downloads">{count(config.downloads)} downloads</span>
    {/snippet}

    {#snippet primary()}
        <Button title="Load" primary onclick={onload}/>
    {/snippet}
</Row>

<style lang="scss">
  .reports {
    min-width: 110px;
    text-align: right;
  }

  .downloads {
    min-width: 90px;
    text-align: right;
  }
</style>
