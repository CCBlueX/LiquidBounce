<script lang="ts">
    /**
     * `author/name` with the author set back, or a bare name.
     */
    let {address}: {
        address: string;
    } = $props();

    const separator = $derived(address.indexOf("/"));
</script>

{#if separator >= 0}<span class="author">{address.slice(0, separator + 1)}</span>{/if}{address.slice(separator + 1)}

<style lang="scss">
  .author {
    font-weight: 500;
    color: var(--clickgui-text-dimmed-color);
  }
</style>
