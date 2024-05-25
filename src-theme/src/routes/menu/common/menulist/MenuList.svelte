<script lang="ts">
    import {fly} from "svelte/transition";
    import {SortableList} from "@jhubbardsf/svelte-sortablejs";
    import "./menulist.scss";
    import {createEventDispatcher} from "svelte";

    export let sortable = false;

    interface MenuListSortEvent {
        oldIndex: number;
        newIndex: number;
    }

    const dispatch = createEventDispatcher<{
        sort: MenuListSortEvent
    }>();

    function handleChange(e: any) {
        dispatch("sort", {
            newIndex: e.newIndex,
            oldIndex: e.oldIndex
        });
    }
</script>

<div class="menu-list" transition:fly|global={{duration: 700, x: 1000}}>
    {#if sortable}
        <SortableList class="menu-list-items" onSort={handleChange} forceFallback={true} animation={150}>
            <slot/>
        </SortableList>
    {:else}
        <div class="menu-list-items">
            <slot/>
        </div>
    {/if}
</div>

<style lang="scss">
  @import "../../../../colors";

  .menu-list {
    background-color: rgba($menu-base-color, 0.68);
    flex: 1;
    border-radius: 5px;
    margin-bottom: 25px;
    position: relative;
  }
</style>
