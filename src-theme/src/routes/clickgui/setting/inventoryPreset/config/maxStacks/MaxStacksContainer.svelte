<script lang="ts">
    import type {MaxStacksGroup} from "../../../../../../integration/types";
    import StackContainer from "./StackContainer.svelte";
    import {createEventDispatcher} from "svelte";

    export let groups: MaxStacksGroup[];

    const dispatch = createEventDispatcher();

    function handleChange() {
        dispatch("change");
    }

    function handleDelete(deletedId: number) {
        groups = groups.filter((_, id) => id != deletedId);
        handleChange();
    }
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
{#if groups.length > 0}
    <div class="container">
        <div class="stacks">
            {#each groups as group, idx}
                <StackContainer
                        group={group}
                        on:change={handleChange}
                        on:delete={() => handleDelete(idx)}
                />
            {/each}
        </div>
    </div>
{/if}

<style lang="scss">
  @use "sass:color";
  @use "../../../../../../colors.scss" as *;

  .stacks {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .container {
    border-radius: 6px;
    outline: 1px solid rgba(color.adjust($clickgui-text-color, $lightness: -85%), 0.3);
    padding: 5px;
    max-height: 300px;
    overflow-y: scroll;
  }

  .container::-webkit-scrollbar {
    width: 2px;
  }
</style>
