<script lang="ts">
    import {createEventDispatcher} from "svelte";

    const dispatch = createEventDispatcher<{
        toggle: { value: string, enabled: boolean }
    }>();

    export let value: string;
    export let name: string;
    export let icon: string | undefined;
    export let enabled: boolean;
</script>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<!-- svelte-ignore a11y-click-events-have-key-events -->
<div class="item" class:has-icon={icon !== undefined} on:click={() => dispatch("toggle", {enabled: !enabled, value:value})}>
    {#if icon}
        <img class="icon" src="{icon}" alt={value}/>
    {/if}
    <div class="name">{name}</div>
    <div class="tick">
        {#if enabled}
            <img src="img/clickgui/icon-tick-checked.svg" alt="enabled">
        {:else}
            <img src="img/clickgui/icon-tick.svg" alt="disabled">
        {/if}
    </div>
</div>

<style lang="scss">
  @use "../../../../colors.scss" as *;

  .item {
    display: grid;
    grid-template-columns: 1fr max-content;
    align-items: center;
    column-gap: 5px;
    cursor: pointer;
    margin: 2px 5px 2px 0;

    &.has-icon {
      grid-template-columns: max-content 1fr max-content;
    }
  }

  .icon {
    height: 25px;
    width: 25px;
  }
  .name {
    font-size: 12px;
    color: $clickgui-text-color;
    text-overflow: ellipsis;
    white-space: nowrap;
    overflow: hidden;
  }
</style>
