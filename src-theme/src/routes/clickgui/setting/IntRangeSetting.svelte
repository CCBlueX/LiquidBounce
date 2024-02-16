<script lang="ts">
    import "nouislider/dist/nouislider.css";
    import "./nouislider.scss";
    import { createEventDispatcher, onMount } from "svelte";
    import noUiSlider from "nouislider";
    import type { ModuleSetting, IntRangeSetting } from "../../../integration/types";

    export let setting: ModuleSetting;

    const dispatch = createEventDispatcher();

    const cSetting = setting as IntRangeSetting;

    let slider: HTMLElement;

    onMount(() => {
        const s = noUiSlider.create(slider, {
            start: [cSetting.value.from, cSetting.value.to],
            connect: true,
            range: {
                min: cSetting.range.from,
                max: cSetting.range.to,
            },
            step: 1,
        });

        s.on("update", values => {
            const newValue = values.map(v => v.toString()).map(v => parseInt(v));

            cSetting.value = {
                from: newValue[0],
                to: newValue[1]
            };
            setting = { ...cSetting };
            dispatch("change");
        });
    });
</script>

<div class="setting" class:has-suffix={cSetting.suffix !== ""}>
    <div class="name">{cSetting.name}</div>
    <div class="value">{cSetting.value.from}-{cSetting.value.to}</div>
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
