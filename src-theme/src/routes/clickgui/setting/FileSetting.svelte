<script lang="ts">
    import type {FileSetting, ModuleSetting} from "../../../integration/types";
    import {createEventDispatcher} from "svelte";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";

    export let setting: ModuleSetting;

    const cSetting = setting as FileSetting;

    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = { ...cSetting };
        dispatch("change");
    }
</script>

<div class="setting">
    <div class="name">{spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>
</div>

<style lang="scss">
  @use "../../../colors.scss" as *;

  .setting {
    padding: 7px 0 2px 0;
    display: grid;
    grid-template-areas:
            "a b"
            "d d";
    grid-template-columns: 1fr max-content;
    column-gap: 5px;

    /* animation fix */
    min-height: 46px;
  }

  .setting,
  .value {
    color: $clickgui-text-color;
    font-weight: 500;
    font-size: 12px;
  }

  .name {
    grid-area: a;
    font-weight: 500;
  }
</style>
