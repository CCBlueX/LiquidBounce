<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {BooleanSetting, ModuleSetting} from "../../../integration/types";
    import ExpandArrow from "./common/ExpandArrow.svelte";
    import SettingGroup from "./common/SettingGroup.svelte";
    import SettingRow from "./common/SettingRow.svelte";
    import Switch from "./common/Switch.svelte";

    /**
     * A group with a switch. The `control` slot replaces its name and switch, for a host with its own way of
     * turning the group off.
     */
    export let setting: ModuleSetting;
    export let path: string;

    const dispatch = createEventDispatcher();

    $: enabled = (setting.value as ModuleSetting[])[0] as BooleanSetting;
    $: empty = (setting.value as ModuleSetting[]).length === 1;

    function handleChange() {
        setting = setting;
        dispatch("change");
    }

    function disable() {
        enabled.value = false;
        handleChange();
    }
</script>

{#if empty && !$$slots.control}
    <!-- Nothing to open: a switch like any other -->
    <SettingRow {setting}>
        <Switch bind:value={enabled.value} on:change={handleChange}/>
    </SettingRow>
{:else}
    <SettingGroup {path} bind:setting skip={1} off={!enabled.value} customTitle={$$slots.control} on:change>
        <svelte:fragment slot="title" let:label let:toggle let:expanded>
            {#if $$slots.control}
                <div class="custom">
                    <slot name="control" {disable} {label}/>
                    {#if !empty}
                        <ExpandArrow {expanded} on:click={toggle}/>
                    {/if}
                </div>
            {/if}
        </svelte:fragment>
        <svelte:fragment slot="end">
            {#if !$$slots.control}
                <Switch bind:value={enabled.value} on:change={handleChange}/>
            {/if}
        </svelte:fragment>
    </SettingGroup>
{/if}

<style lang="scss">
  .custom {
    flex: 1;
    min-width: 0;
    display: grid;
    grid-template-columns: 1fr max-content;
    align-items: center;
  }
</style>
