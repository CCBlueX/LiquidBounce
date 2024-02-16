<script lang="ts">
    import { createEventDispatcher } from "svelte";
    import type {
        ModuleSetting,
        TextSetting,
    } from "../../../integration/types";

    export let setting: ModuleSetting;

    const cSetting = setting as TextSetting;

    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = {...cSetting};
        dispatch("change");
    }
</script>

<div class="setting">
    <div class="name">{cSetting.name}</div>
    <textarea class="value" bind:value={cSetting.value} on:change={handleChange} spellcheck="false"></textarea>
</div>

<style lang="scss">
    @import "../../../colors.scss";

    .setting {
        padding: 7px 0px;
    }

    .name {
        font-weight: 500;
        color: $clickgui-text-color;
        font-size: 12px;
        margin-bottom: 5px;
    }

    .value {
        width: auto;
        max-width: 100%;
        min-width: 100%;
        max-height: 200px;
        height: 50px;
        min-height: 30px;
        background-color: transparent;
        font-family: monospace;
        font-size: 12px;
        color: $clickgui-text-color;
        border: none;
        border-bottom: solid 2px $accent-color;
        padding: 5px;
        border-radius: 3px;

        &::-webkit-scrollbar {
            background-color: transparent;
        }
    }
</style>
