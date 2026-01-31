<script lang="ts">
    import {type ComponentType, createEventDispatcher} from "svelte";

    let availableTabsElement: HTMLElement | undefined;

    export let tabs: {
        title: string,
        icon: string,
        component: ComponentType,
    }[];
    export let activeTab = 0;

    const dispatch = createEventDispatcher<{
        changeTab: { activeTab: number }
    }>();

    function setActiveTab(i: number) {
        activeTab = i;
        dispatch("changeTab", {activeTab});
    }
</script>

<div class="tabs">
    <div class="available-tabs" bind:this={availableTabsElement}>
        {#each tabs as {title, icon}, index}
            <button class="tab-button" class:active={tabs[activeTab].title === title}
                    on:click={() => setActiveTab(index)}>
                <img class="icon" src="img/menu/altmanager/{icon}" alt={title}>
                <span>{title}</span>
            </button>
        {/each}
    </div>

    <div style="width: {availableTabsElement?.clientWidth}px">
        <svelte:component this={tabs[activeTab].component}/>
    </div>
</div>

<style lang="scss">
  @use "../../../../colors.scss" as *;

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

    .icon {
      height: 30px;
    }

    &.active {
      border-color: $accent-color;
    }
  }
</style>