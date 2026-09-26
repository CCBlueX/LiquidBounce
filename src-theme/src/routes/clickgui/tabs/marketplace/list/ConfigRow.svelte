<script lang="ts">
    import ListRow from "../ui/ListRow.svelte";
    import Badge from "../ui/Badge.svelte";
    import PillButton from "../ui/PillButton.svelte";
    import Address from "../Address.svelte";
    import type {MarketplaceConfig} from "../../../../../integration/types";
    import {ago, count, reports, UNKNOWN_SERVER} from "../marketplace";

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
    <PillButton title="Works" onclick={() => onreport(true)}/>
    <PillButton title="Broken" onclick={() => onreport(false)}/>
{/snippet}

<ListRow image={config.image ?? UNKNOWN_SERVER} onclick={onopen} active={config.tracking !== "None"}
     hover={loggedIn ? report : undefined}>
    {#snippet title()}<Address address={config.address}/>{/snippet}

    {#snippet tags()}
        {#if config.tracking !== "None"}
            <Badge text={config.tracking === "Editing" ? "Edited" : "Tracked"}/>
        {/if}
        {#if config.featured}
            <Badge text="Featured"/>
        {/if}
        {#if config.own}
            <Badge text="Yours"/>
        {/if}
        {#if config.visibility === "unlisted"}
            <Badge text="Unlisted"/>
        {/if}
        {#if config.overlayOn}
            <Badge text="Overlay on {config.overlayOn.address}"/>
        {/if}
        {#if config.binds}
            <Badge text="Binds"/>
        {/if}
        {#each config.tags as tag (tag)}
            <Badge text={tag}/>
        {/each}
    {/snippet}

    {#snippet subtitle()}{details}{/snippet}

    {#snippet meta()}
        <span class="reports">{reports(config.works, config.fails)}</span>
        <span class="downloads">{count(config.downloads)} downloads</span>
    {/snippet}

    {#snippet primary()}
        <PillButton title="Load" primary onclick={onload}/>
    {/snippet}
</ListRow>

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
