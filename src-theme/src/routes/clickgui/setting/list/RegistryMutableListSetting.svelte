<script lang="ts">
    import {createEventDispatcher, onMount} from "svelte";
    import {slide} from "svelte/transition";
    import type {ModuleSetting, NamedItem, RegistryMutableListSetting} from "../../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";
    import ExpandArrow from "../common/ExpandArrow.svelte";
    import {getRegistryItems} from "../../../../integration/rest";
    import SearchableList from "./SearchableList.svelte";
    import SettingButton from "../common/SettingButton.svelte";
    import {setItem} from "../../../../integration/persistent_storage";
    import ListItem from "./ListItem.svelte";
    import RemovableItem from "../common/RemovableItem.svelte";
    import {SortableList} from "@jhubbardsf/svelte-sortablejs";
    import DraggableItem from "../common/DraggableItem.svelte";

    export let setting: ModuleSetting;
    export let path: string;

    const cSetting = setting as RegistryMutableListSetting;

    const thisPath = `${path}.${cSetting.name}`;
    let expanded = localStorage.getItem(thisPath) === "true";

    $: setItem(thisPath, expanded.toString());

    let allItems: NamedItem[] = [];
    let selectedItems: NamedItem[] = [];
    let selectableItems: NamedItem[] = [];

    let showChooser = false;
    let sortableRenderKey = 0;

    type SortEvent = {
        oldIndex?: number | null;
        newIndex?: number | null;
        oldDraggableIndex?: number | null;
        newDraggableIndex?: number | null;
    };

    onMount(async () => {
        const registryItems = await getRegistryItems(cSetting.registry);
        allItems = Object.entries(registryItems)
            .map(([identifier, item]) => ({
                value: identifier,
                name: item.name,
                icon: item.icon
            }));
        updateItems();
    });

    function updateItems() {
        selectedItems = cSetting.value.map(id => allItems.find(item => item.value === id))
            .filter(Boolean) as NamedItem[];
        selectableItems = allItems.filter(item => !cSetting.value.includes(item.value));
    }

    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = {...cSetting};
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

    function handleSort(e: SortEvent) {
        const oldIndex = e.oldDraggableIndex ?? e.oldIndex;
        const newIndex = e.newDraggableIndex ?? e.newIndex;

        if (
            oldIndex === undefined ||
            oldIndex === null ||
            newIndex === undefined ||
            newIndex === null ||
            oldIndex === newIndex
        ) {
            return;
        }

        const items = [...selectedItems];

        if (oldIndex < 0 || oldIndex >= items.length || newIndex < 0 || newIndex >= items.length) {
            return;
        }

        const [movedItem] = items.splice(oldIndex, 1);

        if (!movedItem) {
            return;
        }

        items.splice(newIndex, 0, movedItem);

        cSetting.value = items.map(i => i.value);
        
        handleChange();
        updateItems();
        sortableRenderKey++;
    }
</script>

<div class="setting">
    <!-- svelte-ignore a11y-no-static-element-interactions -->
    <div class="head" class:expanded on:contextmenu|preventDefault={() => expanded = !expanded}>
        <div class="name">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>
        <ExpandArrow bind:expanded/>
    </div>
    {#if expanded}
        <div in:slide|global={{duration: 200, axis: "y"}} out:slide|global={{duration: 200, axis: "y"}}>
            <div class="selected-items">
                {#key sortableRenderKey}
                    <SortableList class="" forceFallback={true} fallbackOnBody={true} animation={150} onEnd={handleSort}>
                        {#each selectedItems as item, index (item.value)}
                            <DraggableItem>
                                <RemovableItem on:remove={() => handleRemove(index)}>
                                    <ListItem value={item.value} name={item.name} icon={item.icon} enabled={false}
                                              showEnabledState={false} pointerCursor={false}/>
                                </RemovableItem>
                            </DraggableItem>
                        {/each}
                    </SortableList>
                {/key}
            </div>

            <SettingButton value={showChooser ? "Cancel" : "Add item"} on:click={() => showChooser = !showChooser}/>

            {#if showChooser}
                <div class="list-item-list-wrapper">
                    <SearchableList items={selectableItems} let:item>
                        <ListItem value={item.value} name={item.name} icon={item.icon}
                                  enabled={false} showEnabledState={false} on:toggle={handleAdd}/>
                    </SearchableList>
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
    max-height: 200px;
    margin-bottom: 10px;
    overflow-y: auto;
  }

  .list-item-list-wrapper {
    margin-top: 10px;
  }
</style>

