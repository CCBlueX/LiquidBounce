<script lang="ts">
    import type {Snippet} from "svelte";

    /**
     * Lines up the columns of the RevisionEntry rows in it.
     */
    let {children}: {
        children: Snippet;
    } = $props();
</script>

<div class="revisions">
    {@render children()}
</div>

<style lang="scss">
  .revisions {
    display: grid;
    grid-template-columns: max-content max-content minmax(0, 1fr) max-content;
    column-gap: 12px;
  }
</style>
