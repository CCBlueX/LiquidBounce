<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {ChoiceSetting, ModuleSetting} from "../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import ChoicePicker from "./common/ChoicePicker.svelte";
    import GenericSetting from "./common/GenericSetting.svelte";
    import SettingRow from "./common/SettingRow.svelte";
    import {enterGroup} from "./pages";

    /**
     * A mode and, right under it, the settings of the mode that is chosen.
     */
    export let setting: ModuleSetting;
    export let path: string;

    const thisPath = `${path}.${setting.name}`;
    const {asPage, open} = enterGroup(setting.name);
    const dispatch = createEventDispatcher();

    $: choice = setting as ChoiceSetting;
    $: options = Object.keys(choice.choices);
    $: settings = (choice.choices[choice.active]?.value ?? []) as ModuleSetting[];
    $: active = $spaceSeperatedNames ? convertToSpacedString(choice.active) : choice.active;

    function handleChange() {
        setting = setting;
        dispatch("change");
    }
</script>

<SettingRow {setting}>
    <ChoicePicker {options} bind:value={choice.active} on:change={handleChange}/>
</SettingRow>

{#if settings.length > 0}
    {#if asPage}
        <button type="button" class="more" on:click={open}>
            <span>{active}</span>
            <span class="count">{settings.length}</span>
            <span class="arrow"></span>
        </button>
    {:else}
        <div class="settings">
            {#each settings as child (`${choice.active}.${child.name}`)}
                <GenericSetting path={thisPath} bind:setting={child} on:change={handleChange}/>
            {/each}
        </div>
    {/if}
{/if}

<style lang="scss">
  .settings {
    margin-bottom: 6px;
    padding: 0 12px 0 14px;
    border-left: 2px solid var(--clickgui-setting-group-border-color);
    background-color: var(--clickgui-setting-group-background-color);
  }

  .more {
    display: flex;
    align-items: center;
    gap: 9px;
    width: 100%;
    min-height: 30px;
    margin-bottom: 6px;
    padding: 0 0 0 14px;
    border: none;
    border-left: 2px solid var(--clickgui-setting-group-border-color);
    background-color: var(--clickgui-setting-group-background-color);
    font-family: "Inter", sans-serif;
    font-size: 12px;
    font-weight: 600;
    color: var(--clickgui-text-color);
    text-align: left;
    cursor: pointer;
  }

  .count {
    margin-left: auto;
    font-size: 11px;
    font-weight: 500;
    color: var(--clickgui-text-muted-color);
  }

  .arrow {
    flex: none;
    width: 10px;
    height: 10px;
    margin-right: 10px;
    background: url("/img/clickgui/icon-settings-expand.svg") center no-repeat;
    opacity: .8;
    transform: rotate(-90deg);
  }
</style>
