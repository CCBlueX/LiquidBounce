<script lang="ts">
    import {createEventDispatcher, onMount} from "svelte";
    import {slide} from "svelte/transition";
    import type {ModuleSetting, NamedItem, RegistryMutableListSetting} from "../../../../integration/types";
    import ExpandArrow from "../common/ExpandArrow.svelte";
    import {getRegistryItems} from "../../../../integration/rest";
    import SearchableList from "./SearchableList.svelte";
    import SettingButton from "../common/SettingButton.svelte";
    import {setItem} from "../../../../integration/persistent_storage";
    import ListItem from "./ListItem.svelte";
    import RemovableItem from "../common/RemovableItem.svelte";
    import {SortableList} from "@jhubbardsf/svelte-sortablejs";
    import DraggableItem from "../common/DraggableItem.svelte";
    import {escapeLayer} from "../../../../integration/escape";
    import SettingRow from "../common/SettingRow.svelte";

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

    const layer = escapeLayer(() => showChooser = false);
    $: layer.setOpen(showChooser);

    let content: HTMLElement | undefined;

    // A right click on the name line opens the list, inside the list it stays a right click
    function handleContextMenu(e: MouseEvent) {
        if (content?.contains(e.target as Node)) {
            return;
        }

        e.preventDefault();
        expanded = !expanded;
    }

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

<!-- svelte-ignore a11y-no-static-element-interactions -->
<div on:contextmenu={handleContextMenu}>
    <SettingRow {setting}>
        <span class="count">{cSetting.value.length} {cSetting.value.length === 1 ? "entry" : "entries"}</span>
        <ExpandArrow bind:expanded/>

        <svelte:fragment slot="below">
            {#if expanded}
                <div class="content" bind:this={content} transition:slide|global={{duration: 150, axis: "y"}}>
                    {#if selectedItems.length > 0}
                        <div class="selected-items">
                            {#key sortableRenderKey}
                                <SortableList class="" forceFallback={true} fallbackOnBody={true} animation={150}
                                              onEnd={handleSort}>
                                    {#each selectedItems as item, index}
                                        <DraggableItem>
                                            <RemovableItem on:remove={() => handleRemove(index)}>
                                                <ListItem value={item.value} name={item.name} icon={item.icon}
                                                          enabled={false} showEnabledState={false}
                                                          pointerCursor={false}/>
                                            </RemovableItem>
                                        </DraggableItem>
                                    {/each}
                                </SortableList>
                            {/key}
                        </div>
                    {/if}

                    <SettingButton value={showChooser ? "Cancel" : "Add item"}
                                   on:click={() => showChooser = !showChooser}/>

                    {#if showChooser}
                        <div class="chooser">
                            <SearchableList items={selectableItems} let:item>
                                <ListItem value={item.value} name={item.name} icon={item.icon}
                                          enabled={false} showEnabledState={false} on:toggle={handleAdd}/>
                            </SearchableList>
                        </div>
                    {/if}
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

  .content {
    padding-bottom: 8px;
  }

  .selected-items {
    max-height: 200px;
    margin-bottom: 3px;
    overflow-y: auto;

    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-thumb {
      border-radius: 3px;
      background-color: var(--clickgui-control-border-color);
    }
  }

  .chooser {
    margin-top: 6px;
  }
</style>
