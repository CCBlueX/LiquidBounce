<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import {slide} from "svelte/transition";
    import type {ModuleSetting, NamedItem, RegistryMutableListSetting} from "../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";
    import VirtualList from "./VirtualList.svelte";
    import SelectableListItem from "./SelectableListItem.svelte";
    import ExpandArrow from "../common/ExpandArrow.svelte";
    import {onMount} from "svelte";
    import {getRegistryItems} from "../../../../integration/rest";
    import {setItem} from "../../../../integration/persistent_storage";

    export let setting: ModuleSetting;

    const cSetting = setting as RegistryMutableListSetting;
    let items: NamedItem[] = [];
    let allItems: NamedItem[] = [];
    let expanded = localStorage.getItem(cSetting.key) === "true";
    let searchQuery = "";
    let renderedItems: NamedItem[] = [];
    let showChooser = false;

    $: setItem(cSetting.key, expanded.toString());

    onMount(async () => {
        const registryItems = await getRegistryItems(cSetting.registry);
        allItems = Object.entries(registryItems)
            .map(([identifier, item]) => ({
                value: identifier,
                name: item.name,
                icon: item.icon
            })) as NamedItem[];
        allItems = allItems
        updateItems();
    });

    function updateItems() {
        items = cSetting.value.map(id => allItems.find(item => item.value === id)).filter(Boolean) as NamedItem[];
    }

    $: {
        const searchWords = searchQuery.toLowerCase().trim().split(/\s+/).filter(word => word.length > 0);
        let filteredItems = allItems.filter(item => {
            if (cSetting.value.includes(item.value)) return false;
            if (searchWords.length === 0) return true;

            const itemNameLower = item.name.toLowerCase();
            return searchWords.every(word => itemNameLower.includes(word));
        });
        renderedItems = filteredItems;
    }

    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = { ...cSetting };
        dispatch("change");
    }

    function handleAdd(e: CustomEvent<{ value: string }>) {
        cSetting.value = [...cSetting.value, e.detail.value];
        showChooser = false;
        handleChange();
        updateItems();
    }

    function handleRemove(index: number) {
        cSetting.value = cSetting.value.filter((_, i) => i !== index);
        handleChange();
        updateItems();
    }

    function moveUp(index: number) {
        if (index === 0) return;
        const newValue = [...cSetting.value];
        [newValue[index - 1], newValue[index]] = [newValue[index], newValue[index - 1]];
        cSetting.value = newValue;
        handleChange();
        updateItems();
    }

    function moveDown(index: number) {
        if (index === cSetting.value.length - 1) return;
        const newValue = [...cSetting.value];
        [newValue[index], newValue[index + 1]] = [newValue[index + 1], newValue[index]];
        cSetting.value = newValue;
        handleChange();
        updateItems();
    }
</script>

<div class="setting">
    <!-- svelte-ignore a11y-no-static-element-interactions -->
    <div class="head" class:expanded on:contextmenu|preventDefault={() => expanded = !expanded} on:keydown={(e) => { if (e.key === 'Enter' || e.key === ' ') expanded = !expanded; }}>
        <div class="name">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>
        <ExpandArrow bind:expanded/>
    </div>
    {#if expanded}
        <div in:slide|global={{duration: 200, axis: "y"}} out:slide|global={{duration: 200, axis: "y"}}>
            <div class="selected-items">
                {#each items as item, index (item.value)}
                    <div class="item-row">
                        {#if item.icon}
                            <img class="icon" src="{item.icon}" alt={item.value}/>
                        {/if}
                        <div class="name">{item.name}</div>
                        <div class="controls">
                            <div class="arrow-column">
                                {#if index > 0}
                                    <button class="arrow-btn" on:click={() => moveUp(index)} title="Move up">▲</button>
                                {:else}
                                    <span class="arrow-placeholder"></span>
                                {/if}
                                {#if index < items.length - 1}
                                    <button class="arrow-btn" on:click={() => moveDown(index)} title="Move down">▼</button>
                                {:else}
                                    <span class="arrow-placeholder"></span>
                                {/if}
                            </div>
                            <button class="remove-btn" on:click={() => handleRemove(index)} title="Remove">✕</button>
                        </div>
                    </div>
                {/each}
                <button class="add-btn" on:click={() => showChooser = !showChooser}>Add Item</button>
            </div>
            {#if showChooser}
                <div class="chooser">
                    <input type="text" placeholder="Search" class="search-input" bind:value={searchQuery} spellcheck="false">
                    <div class="results">
                        <VirtualList items={renderedItems} let:item>
                            <SelectableListItem value={item.value} name={item.name} icon={item.icon} selected={false} on:select={handleAdd}/>
                        </VirtualList>
                    </div>
                </div>
            {/if}
        </div>
    {/if}
</div>

<style lang="scss">

    .setting {
        padding: 7px 0;
    }

    .head {
        display: flex;
        justify-content: space-between;
        transition: ease margin-bottom .2s;

        &.expanded {
            margin-bottom: 10px;
        }

        .name {
            color: var(--clickgui-text-color);
            font-size: 12px;
            font-weight: 600;
        }
    }

    .selected-items {
        margin-bottom: 10px;

        .item-row {
            display: flex;
            align-items: center;
            gap: 5px;
            padding: 5px;
            background-color: color-mix(in srgb, var(--clickgui-base-color) 10%, transparent);
            border-radius: 3px;
            margin-bottom: 5px;

            .icon {
                height: 20px;
                width: 20px;
            }

            .name {
                flex: 1;
                color: var(--clickgui-text-color);
                font-size: 12px;
            }

            .controls {
                display: flex;
                align-items: center;
                gap: 5px;
            }

            .arrow-column {
                display: flex;
                flex-direction: column;
                gap: 2px;
            }

            .arrow-btn {
                background: none;
                border: none;
                color: var(--accent-color);
                cursor: pointer;
                font-size: 14px;
                padding: 0 5px;
                line-height: 1;
                transition: color 0.2s;

                &:hover {
                    color: color-mix(in srgb, var(--accent-color) 80%, white);
                }
            }

            .arrow-placeholder {
                display: block;
                height: 14px;
                width: 10px;
                visibility: hidden;
            }

            .remove-btn {
                background: none;
                border: none;
                color: #ff4444;
                cursor: pointer;
                font-size: 14px;
                padding: 2px 5px;
                transition: color 0.2s;

                &:hover {
                    color: #ff6666;
                }
            }
        }

        .add-btn {
            width: 100%;
            padding: 8px;
            background-color: var(--accent-subtle-background-color);
            border: none;
            border-radius: 4px;
            color: var(--clickgui-text-color);
            cursor: pointer;
            font-size: 12px;
        }
    }

    .chooser {
        .search-input {
            width: 100%;
            border: none;
            border-bottom: solid 1px var(--accent-color);
            font-family: "Inter", sans-serif;
            font-size: 12px;
            padding: 5px;
            color: var(--clickgui-text-color);
            margin-bottom: 5px;
            background-color: var(--clickgui-input-background-color);
        }

        .results {
            height: 150px;
            overflow-y: auto;
            overflow-x: hidden;
            min-height: 100px;
            max-height: 300px;
            position: relative;
        }
    }
</style>


