<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import {slide} from "svelte/transition";
    import type {ListSetting, ModuleSetting, NamedItem} from "../../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";
    import ExpandArrow from "../common/ExpandArrow.svelte";
    import {setItem} from "../../../../integration/persistent_storage";
    import ListItem from "./ListItem.svelte";
    import SearchableList from "./SearchableList.svelte";

    export let setting: ModuleSetting;
    export let path: string;
    export let items: NamedItem[];

    const cSetting = setting as ListSetting;
    const thisPath = `${path}.${cSetting.name}`;

    const dispatch = createEventDispatcher();
    let expanded = localStorage.getItem(thisPath) === "true";

    $: setItem(thisPath, expanded.toString());

    function handleItemToggle(e: CustomEvent<{ value: string, enabled: boolean }>) {
        if (e.detail.enabled) {
            cSetting.value = [...cSetting.value, e.detail.value];
        } else {
            cSetting.value = cSetting.value.filter(b => b !== e.detail.value);
        }

        setting = {...cSetting};
        dispatch("change");
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
            <SearchableList {items} let:item>
                <ListItem value={item.value} name={item.name} icon={item.icon}
                          enabled={cSetting.value.includes(item.value)} on:toggle={handleItemToggle} />
            </SearchableList>
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
</style>
