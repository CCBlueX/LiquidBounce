<script lang="ts">
    import type {ComponentType} from "svelte";

    export let tabs: {
        title: string,
        icon: string,
        component: ComponentType,
    }[];

    let activeTab = 0;
</script>

<div class="tabs">
    <div class="available-tabs">
        {#each tabs as {title, icon}, index}
            <button class="tab-button" class:active={tabs[activeTab].title === title}
                    on:click={() => activeTab = index}>
                <img src="img/menu/altmanager/{icon}" alt={title}>
                <span>{title}</span>
            </button>
        {/each}
    </div>

    <svelte:component this={tabs[activeTab].component} on:modify/>
</div>

<style lang="scss">
  @import "../../../../colors.scss";

  .available-tabs {
    display: flex;
    column-gap: 10px;
    margin-bottom: 40px;
  }

  .tab-button {
    font-family: "Inter", sans-serif;
    background-color: rgba($menu-base-color, .36);
    color: $menu-text-color;
    padding: 10px;
    border: solid 2px transparent;
    border-radius: 5px;
    flex-grow: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    row-gap: 10px;
    cursor: pointer;
    transition: ease border-color .2s;

    &.active {
      border-color: $accent-color;
    }
  }
</style>