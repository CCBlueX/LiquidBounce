<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import {slide} from "svelte/transition";
    import type {ListSetting, ModuleSetting, NamedItem} from "../../../../integration/types";
    import ExpandArrow from "../common/ExpandArrow.svelte";
    import {setItem} from "../../../../integration/persistent_storage";
    import ListItem from "./ListItem.svelte";
    import SearchableList from "./SearchableList.svelte";
    import SettingRow from "../common/SettingRow.svelte";
    import TextButton from "../common/TextButton.svelte";

    export let setting: ModuleSetting;
    export let path: string;
    export let items: NamedItem[];

    const cSetting = setting as ListSetting;
    const thisPath = `${path}.${cSetting.name}`;

    const dispatch = createEventDispatcher();
    let expanded = localStorage.getItem(thisPath) === "true";

    $: setItem(thisPath, expanded.toString());

    const MAX_THUMBNAILS = 3;

    let content: HTMLElement | undefined;
    let filtered: NamedItem[] = [];

    $: thumbnails = items
        .filter(item => item.icon !== undefined && cSetting.value.includes(item.value))
        .slice(0, MAX_THUMBNAILS);

    function commit() {
        setting = {...cSetting};
        dispatch("change");
    }

    /**
     * Enables or disables every item the search currently shows.
     */
    function setFiltered(enabled: boolean) {
        const shown = new Set(filtered.map(item => item.value));
        const others = cSetting.value.filter(value => !shown.has(value));

        cSetting.value = enabled ? [...others, ...shown] : others;
        commit();
    }

    // A right click on the name line opens the list, inside the list it stays a right click
    function handleContextMenu(e: MouseEvent) {
        if (content?.contains(e.target as Node)) {
            return;
        }

        e.preventDefault();
        expanded = !expanded;
    }

    function handleItemToggle(e: CustomEvent<{ value: string, enabled: boolean }>) {
        if (e.detail.enabled) {
            cSetting.value = [...cSetting.value, e.detail.value];
        } else {
            cSetting.value = cSetting.value.filter(b => b !== e.detail.value);
        }

        commit();
    }
</script>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<div on:contextmenu={handleContextMenu}>
    <SettingRow {setting}>
        {#if expanded}
            <TextButton on:click={() => setFiltered(true)}>All</TextButton>
            <TextButton on:click={() => setFiltered(false)}>None</TextButton>
        {:else}
            {#each thumbnails as item (item.value)}
                <img class="thumbnail" src={item.icon} alt={item.name} title={item.name}>
            {/each}
        {/if}
        <span class="count">
            {cSetting.value.length}{#if items.length > 0}&nbsp;of {items.length}{/if}
        </span>
        <ExpandArrow bind:expanded/>

        <svelte:fragment slot="below">
            {#if expanded}
                <div bind:this={content} transition:slide|global={{duration: 150, axis: "y"}}>
                    <SearchableList {items} bind:filtered let:item>
                        <ListItem value={item.value} name={item.name} icon={item.icon}
                                  enabled={cSetting.value.includes(item.value)} on:toggle={handleItemToggle} />
                    </SearchableList>
                </div>
            {/if}
        </svelte:fragment>
    </SettingRow>
</div>

<style lang="scss">
  .count {
    font-size: 11px;
    color: var(--clickgui-text-muted-color);
    white-space: nowrap;
  }

  .thumbnail {
    flex: none;
    width: 20px;
    height: 20px;
    image-rendering: pixelated;
  }
</style>
