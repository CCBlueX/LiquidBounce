<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {
        ModuleSetting,
        ConfigurableSetting,
    } from "../../../integration/types";
    import GenericSetting from "./common/GenericSetting.svelte";
    import ExpandArrow from "./common/ExpandArrow.svelte";
    import { setItem } from "../../../integration/persistent_storage";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";

    export let setting: ModuleSetting;
    export let path: string;

    const cSetting = setting as ConfigurableSetting;
    const thisPath = `${path}.${cSetting.name}`;

    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = {...cSetting};
        dispatch("change");
    }

    let expanded = localStorage.getItem(thisPath) === "true";
    let skipAnimationDelay = false;

    $: setItem(thisPath, expanded.toString());

    function toggleExpanded() {
        expanded = !expanded;
        skipAnimationDelay = true;
    }
</script>

<div class="setting">
    <!-- svelte-ignore a11y-no-static-element-interactions -->
    <div class="head" class:expanded on:contextmenu|preventDefault={toggleExpanded}>
        <div class="title">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>
        <ExpandArrow bind:expanded on:click={() => skipAnimationDelay = true} />
    </div>

    {#if expanded}
        <div class="nested-settings">
            {#each cSetting.value as setting (setting.name)}
                <GenericSetting {skipAnimationDelay} path={thisPath} bind:setting on:change={handleChange}/>
            {/each}
        </div>
    {/if}
</div>

<style lang="scss">
  @import "../../../colors.scss";

  .setting {
    padding: 7px 0;
  }

  .title {
    color: $clickgui-text-color;
    font-size: 12px;
    font-weight: 600;
  }

  .head {
    display: flex;
    justify-content: space-between;
    transition: ease margin-bottom .2s;

    &.expanded {
      margin-bottom: 10px;
    }
  }

  .nested-settings {
    border-left: solid 2px $accent-color;
    padding-left: 7px;
  }
</style>
