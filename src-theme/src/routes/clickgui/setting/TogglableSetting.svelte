<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {BooleanSetting as TBooleanSetting, ModuleSetting, TogglableSetting,} from "../../../integration/types";
    import ExpandArrow from "./common/ExpandArrow.svelte";
    import GenericSetting from "./common/GenericSetting.svelte";
    import Switch from "./common/Switch.svelte";
    import {setItem} from "../../../integration/persistent_storage";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";

    export let setting: ModuleSetting;
    export let path: string;

    const cSetting = setting as TogglableSetting;
    const thisPath = `${path}.${cSetting.name}`;

    const dispatch = createEventDispatcher();

    const enabledSetting = cSetting.value[0] as TBooleanSetting;

    let nestedSettings = cSetting.value.slice(1);

    let expanded = localStorage.getItem(thisPath) === "true";

    $: setItem(thisPath, expanded.toString());

    function handleChange() {
        setting = {...cSetting};
        dispatch("change");
    }

    function disable() {
        enabledSetting.value = false;
        handleChange();
    }

    function toggleExpanded() {
        if (nestedSettings.length === 0) return;
        expanded = !expanded;
    }
</script>

<div class="setting">
    <!-- svelte-ignore a11y-no-static-element-interactions -->
    <div
            class="head"
            class:expand={nestedSettings.length > 0}
            class:expanded={expanded && nestedSettings.length > 0}
            on:contextmenu|preventDefault={toggleExpanded}
    >
        <slot
                name="control"
                {disable}
                label={$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}
        >
            <Switch
                    name={$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}
                    bind:value={enabledSetting.value}
                    on:change={handleChange}
            />
        </slot>

        {#if nestedSettings.length > 0}
            <ExpandArrow bind:expanded/>
        {/if}
    </div>

    {#if expanded}
        <div class="nested-settings">
            {#each nestedSettings as setting (setting.name)}
                <GenericSetting path={thisPath} bind:setting on:change={handleChange}/>
            {/each}
        </div>
    {/if}
</div>

<style lang="scss">

  .setting {
    padding: 7px 0px;
  }

    .head {
      min-height: 14px;
      transition: ease margin-bottom .2s;

      &.expand {
        display: grid;
        grid-template-columns: 1fr max-content;
        align-items: center;
      }

    &.expanded {
      margin-bottom: 10px;
    }
  }

  .nested-settings {
    border-left: solid 2px var(--clickgui-setting-group-border-color);
    padding-left: 7px;
  }
</style>
