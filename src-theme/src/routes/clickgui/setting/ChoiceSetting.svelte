<script lang="ts">
    import { createEventDispatcher } from "svelte";
    import type {
        ModuleSetting,
        ChoiceSetting,
    } from "../../../integration/types";
    import Dropdown from "./common/Dropdown.svelte";
    import ExpandArrow from "./common/ExpandArrow.svelte";
    import GenericSetting from "./common/GenericSetting.svelte";

    export let setting: ModuleSetting;

    const cSetting = setting as ChoiceSetting;

    const dispatch = createEventDispatcher();
    const options = Object.keys(cSetting.choices);
    let expanded = false;

    let nestedSettings = cSetting.choices[cSetting.active]
        .value as ModuleSetting[];
    $: nestedSettings = cSetting.choices[cSetting.active]
        .value as ModuleSetting[];

    function handleChange() {
        setting = { ...cSetting };
        dispatch("change");
    }
</script>

<div class="setting">
    {#if nestedSettings.length > 0}
        <div class="head expand">
            <Dropdown
                bind:value={cSetting.active}
                {options}
                name={cSetting.name}
                on:change={handleChange}
            />
            <ExpandArrow bind:expanded />
        </div>
    {:else}
        <div class="head">
            <Dropdown
                bind:value={cSetting.active}
                {options}
                name={cSetting.name}
                on:change={handleChange}
            />
        </div>
    {/if}

    {#if expanded && nestedSettings.length > 0}
        <div class="nested-settings">
            {#each nestedSettings as setting (setting.name)}
                <GenericSetting bind:setting={setting} on:change={handleChange} />
            {/each}
        </div>
    {/if}
</div>

<style lang="scss">
    @import "../../../colors.scss";

    .setting {
        padding: 7px 0px;
    }

    .head.expand {
        display: grid;
        grid-template-columns: 1fr max-content;
    }

    .nested-settings {
        border-left: solid 2px $accent-color;
        padding-left: 7px;
        margin-top: 10px;
    }
</style>
