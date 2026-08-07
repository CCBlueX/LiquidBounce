<script lang="ts">
    import {createEventDispatcher} from "svelte";

    let value = "";

    const dispatch = createEventDispatcher<{
        search: { query: string }
    }>();
</script>

<input class="search" type="text" placeholder="Search..." bind:value={value}
       on:input={() => dispatch("search", {query: value})} spellcheck="false">

<style lang="scss">

  .search {
    background-color: var(--menu-search-background-color);
    border: none;
    color: var(--menu-text-color);
    font-family: "Inter", sans-serif;
    font-size: 20px;
    border-radius: 5px;
    border-bottom: solid 4px var(--menu-search-border-color);
    background-image: url("/img/menu/icon-search.svg");
    background-repeat: no-repeat;
    background-position: 20px center;
    padding: 20px 20px 20px 60px;
    flex: 1;
  }
</style>