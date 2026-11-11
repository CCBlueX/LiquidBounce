<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {ModuleSetting, TextSetting,} from "../../../integration/types";
    import SettingRow from "./common/SettingRow.svelte";

    export let setting: ModuleSetting;

    const cSetting = setting as TextSetting;

    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = {...cSetting};
        dispatch("change");
    }
</script>

<SettingRow {setting}>
    <input type="text" class="value" spellcheck="false" aria-label={cSetting.name}
           bind:value={cSetting.value}
           on:input={handleChange}
    >
</SettingRow>

<style lang="scss">
  @use "./common/underlined-input" as *;

  .value {
    @include underlined-input;
    flex: 1 1 170px;
    width: 170px;
    min-width: 0;
    max-width: 100%;
  }
</style>
