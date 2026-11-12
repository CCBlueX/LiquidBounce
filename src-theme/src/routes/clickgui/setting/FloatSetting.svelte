<script lang="ts">
    import "nouislider/dist/nouislider.css";
    import "./nouislider.scss";
    import {createEventDispatcher, onMount} from "svelte";
    import noUiSlider, {type API} from "nouislider";
    import type {FloatSetting, ModuleSetting,} from "../../../integration/types";
    import ValueInput from "./common/ValueInput.svelte";
    import SettingRow from "./common/SettingRow.svelte";

    export let setting: ModuleSetting;

    const cSetting = setting as FloatSetting;

    const dispatch = createEventDispatcher();

    let slider: HTMLElement;
    let apiSlider: API;

    onMount(() => {
        // The value can sit outside the declared range (e.g. set through the `.value` command),
        // so widen the slider instead of letting noUiSlider clamp it away.
        const min = Math.min(cSetting.range.from, cSetting.value);
        const max = Math.max(cSetting.range.to, cSetting.value);

        // Coarsen the step by however much the track had to grow, so an outlier in
        // either direction does not get an absurdly fine step. Reduces to
        // `cSetting.range.to` whenever the value is within its declared range.
        const grownBy = (max - min) - (cSetting.range.to - cSetting.range.from);
        const stepBasis = cSetting.range.to + grownBy;

        let step = 0.01;

        if (stepBasis > 100) {
            step = 0.1;
        } else if (stepBasis <= 0.1) {
            step = 0.0001;
        } else if (stepBasis <= 1.0) {
            step = 0.001;
        }

        apiSlider = noUiSlider.create(slider, {
            start: cSetting.value,
            connect: "lower",
            range: {
                min,
                max,
            },
            step: step,
            format: {
                to: (value) => parseFloat(value.toFixed(4)), // Display up to 4 decimal places
                from: (value) => parseFloat(value), // Convert back to float
            }
        });

        apiSlider.on("update", (values) => {
            const newValue = parseFloat(values[0].toString());

            cSetting.value = newValue;
            setting = { ...cSetting };
        });

        apiSlider.on("set", () => {
            dispatch("change");
        });
    });
</script>

<SettingRow {setting}>
    <span class="slider-value">
        <ValueInput valueType="float" value={cSetting.value}
                    on:change={(e) => apiSlider.set(e.detail.value)}/>
        {#if cSetting.suffix !== ""}
            <span>{cSetting.suffix}</span>
        {/if}
    </span>
    <div slot="below" class="slider-track" bind:this={slider}></div>
</SettingRow>
