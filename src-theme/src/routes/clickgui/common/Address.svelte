<script lang="ts">
    let {address}: { address: string } = $props();

    const slash = $derived(address.indexOf("/"));
</script>

{#if slash >= 0}<span class="author">{address.slice(0, slash + 1)}</span>{address.slice(slash + 1)}{:else}{address}{/if}

<style lang="scss">
  .author {
    font-weight: 500;
    color: var(--clickgui-text-dimmed-color);
  }
</style>
