<script lang="ts">
    import "nouislider/dist/nouislider.css";
    import "./nouislider.scss";
    import { createEventDispatcher, onMount } from "svelte";
    import noUiSlider from "nouislider";
    import type {
        ModuleSetting,
        FloatSetting,
    } from "../../../integration/types";

    export let setting: ModuleSetting;

    const cSetting = setting as FloatSetting;

    const dispatch = createEventDispatcher();

    let slider: HTMLElement;

    onMount(() => {
        const s = noUiSlider.create(slider, {
            start: cSetting.value,
            connect: "lower",
            range: {
                min: cSetting.range.from,
                max: cSetting.range.to,
            },
            step: 0.01,
        });

        s.on("update", (values) => {
            const newValue = parseFloat(values[0].toString());

            cSetting.value = newValue;
            setting = { ...cSetting };
            dispatch("change");
        });
    });
</script>

<div class="setting" class:has-suffix={cSetting.suffix !== ""}>
    <div class="name">{cSetting.name}</div>
    <div class="value">{cSetting.value}</div>
    {#if cSetting.suffix !== ""}
        <div class="suffix">{cSetting.suffix}</div>
    {/if}
    <div bind:this={slider} class="slider"></div>
</div>

<style lang="scss">
    @import "../../../colors.scss";

    .setting {
        padding: 7px 0 2px 0;
        display: grid;
        grid-template-areas:
            "a b"
            "d d";
        grid-template-columns: 1fr max-content;
        column-gap: 5px;
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
    }

    .value {
        grid-area: b;
    }

    .suffix {
        grid-area: c;
    }

    .slider {
        grid-area: d;
        margin-right: 10px;
    }
</style>
