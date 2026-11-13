<script lang="ts">
    import type {Snippet} from "svelte";

    let {title, children}: {
        title: string;
        children: Snippet;
    } = $props();
</script>

<section>
    <h3 class="title">{title}</h3>
    {@render children()}
</section>

<style lang="scss">
  .title {
    margin-bottom: 8px;
    font-size: 12px;
    font-weight: 600;
    color: var(--clickgui-text-color);
  }
</style>
