<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {
        ModuleSetting,
        ConfigurableSetting,
    } from "../../../integration/types";
    import GenericSetting from "./common/GenericSetting.svelte";
    import ExpandArrow from "./common/ExpandArrow.svelte";

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

    $: localStorage.setItem(thisPath, expanded.toString());
</script>

<div class="setting">
    <div class="head">
        <div class="title">{cSetting.name}</div>
        <ExpandArrow bind:expanded />
    </div>

    {#if expanded}
        <div class="nested-settings">
            {#each cSetting.value as setting (setting.name)}
                <GenericSetting path={thisPath} bind:setting on:change={handleChange}/>
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
  }

  .nested-settings {
    border-left: solid 2px $accent-color;
    padding-left: 7px;
    margin-top: 10px;
  }
</style>
