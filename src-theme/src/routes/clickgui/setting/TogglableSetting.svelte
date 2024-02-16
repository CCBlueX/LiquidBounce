<script lang="ts">
    import { createEventDispatcher } from "svelte";
    import type {
        ModuleSetting,
        TogglableSetting,
        BooleanSetting as TBooleanSetting,
    } from "../../../integration/types";
    import ExpandArrow from "./common/ExpandArrow.svelte";
    import GenericSetting from "./common/GenericSetting.svelte";
    import Switch from "./common/Switch.svelte";

    export let setting: ModuleSetting;

    const cSetting = setting as TogglableSetting;

    const dispatch = createEventDispatcher();

    const enabledSetting = cSetting.value[0] as TBooleanSetting;

    let nestedSettings = cSetting.value.slice(1);

    let expanded = false;

    function handleChange() {
        setting = { ...cSetting };
        dispatch("change");
    }
</script>

<div class="setting">
    {#if nestedSettings.length > 0}
        <div class="head expand">
            <Switch
                name={cSetting.name}
                bind:value={enabledSetting.value}
                on:change={handleChange}
            />
            <ExpandArrow bind:expanded />
        </div>
    {:else}
        <div class="head">
            <Switch
                name={cSetting.name}
                bind:value={enabledSetting.value}
                on:change={handleChange}
            />
        </div>
    {/if}

    {#if expanded}
        <div class="nested-settings">
            {#each nestedSettings as setting (setting.name)}
                <GenericSetting bind:setting on:change={handleChange} />
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
        padding: 10px 0 10px 7px;
        margin-top: 10px;
    }
</style>
