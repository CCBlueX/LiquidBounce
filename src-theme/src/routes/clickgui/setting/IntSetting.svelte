<script lang="ts">
    import "nouislider/dist/nouislider.css";
    import "./nouislider.scss";
    import {createEventDispatcher, onMount} from "svelte";
    import noUiSlider, {type API} from "nouislider";
    import type {IntSetting, ModuleSetting} from "../../../integration/types";
    import ValueInput from "./common/ValueInput.svelte";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";

    export let setting: ModuleSetting;

    const cSetting = setting as IntSetting;

    const dispatch = createEventDispatcher();

    let slider: HTMLElement;
    let apiSlider: API;

    onMount(() => {
        apiSlider = noUiSlider.create(slider, {
            start: cSetting.value,
            connect: "lower",
            range: {
                min: cSetting.range.from,
                max: cSetting.range.to,
            },
            step: 1,
        });

        apiSlider.on("update", (values) => {
            const newValue = parseInt(values[0].toString());

            cSetting.value = newValue;
            setting = { ...cSetting };
        });

        apiSlider.on("set", () => {
            dispatch("change");
        });
    });
</script>

<div class="setting" class:has-suffix={cSetting.suffix !== ""}>
    <div class="name">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>
    <div class="value">
        <ValueInput valueType="int" value={cSetting.value}
                    on:change={(e) => apiSlider.set(e.detail.value)}/>
    </div>
    {#if cSetting.suffix !== ""}
        <div class="suffix">{cSetting.suffix}</div>
    {/if}
    <div bind:this={slider} class="slider"></div>
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

    .setting.has-suffix {
        grid-template-areas:
            "a b c"
            "d d d";
        grid-template-columns: 1fr max-content max-content;
    }

    .suffix,
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

    .value {
        grid-area: b;
    }

    .suffix {
        grid-area: c;
    }

    .slider {
        grid-area: d;
        padding-right: 10px;
    }
</style>
