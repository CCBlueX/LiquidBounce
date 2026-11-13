<script lang="ts">
    import type {Snippet} from "svelte";
    import Dropdown from "../../../setting/common/Dropdown.svelte";
    import {SORTS} from "../marketplace";
    import SearchInput from "./SearchInput.svelte";

    /**
     * The search and sort order of a list, with the filters and actions of [children] behind them.
     */
    let {query = $bindable(), sort = $bindable(), placeholder, children}: {
        query: string;
        sort: string;
        placeholder: string;
        children?: Snippet;
    } = $props();
</script>

<div class="bar">
    <div class="search">
        <SearchInput bind:query {placeholder}/>
    </div>
    <div class="sort">
        <Dropdown name="Sort" options={Object.keys(SORTS)} bind:value={sort}/>
    </div>
    {@render children?.()}
</div>

<style lang="scss">
  .bar {
    display: flex;
    align-items: center;
    column-gap: 14px;
  }

  .search {
    flex: 1;
  }

  .sort {
    width: 120px;
  }
</style>
