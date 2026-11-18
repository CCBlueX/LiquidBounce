<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import {escapeLayer} from "../../../integration/escape";

    let value = "";

    const dispatch = createEventDispatcher<{
        search: { query: string }
    }>();

    const layer = escapeLayer(() => {
        value = "";
        dispatch("search", {query: value});
    });
    $: layer.setOpen(value !== "");
</script>

<input class="search" type="text" placeholder="Search..." bind:value={value}
       on:input={() => dispatch("search", {query: value})} spellcheck="false">

<style lang="scss">

  .search {
    background-color: var(--menu-search-background-color);
    border: 1px solid var(--accent-border-color);
    color: var(--menu-text-color);
    font-family: "Inter", sans-serif;
    font-size: 18px;
    border-radius: var(--radius-md);
    background-image: url("/img/menu/icon-search.svg");
    background-repeat: no-repeat;
    background-position: 20px center;
    padding: 18px 20px 18px 56px;
    flex: 1;
    transition: border-color var(--duration-fast) var(--ease-standard),
    box-shadow var(--duration-fast) var(--ease-standard),
    background-color var(--duration-fast) var(--ease-standard);

    &:hover {
      background-color: var(--menu-base-50-color);
    }

    &:focus-visible {
      border-color: var(--menu-search-border-color);
      box-shadow: 0 0 0 3px var(--accent-subtle-background-color);
    }
  }
</style>