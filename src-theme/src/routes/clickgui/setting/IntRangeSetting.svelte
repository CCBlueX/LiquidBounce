<script lang="ts">
    import "nouislider/dist/nouislider.css";
    import "./nouislider.scss";
    import {createEventDispatcher, onMount} from "svelte";
    import noUiSlider, {type API} from "nouislider";
    import type {IntRangeSetting, ModuleSetting} from "../../../integration/types";
    import ValueInput from "./common/ValueInput.svelte";
    import SettingRow from "./common/SettingRow.svelte";

    export let setting: ModuleSetting;

    const dispatch = createEventDispatcher();

    const cSetting = setting as IntRangeSetting;

    let slider: HTMLElement;
    let apiSlider: API;

    onMount(() => {
        // The value can sit outside the declared range (e.g. set through the `.value` command),
        // so widen the slider instead of letting noUiSlider clamp it away.
        const min = Math.min(cSetting.range.from, cSetting.value.from, cSetting.value.to);
        const max = Math.max(cSetting.range.to, cSetting.value.from, cSetting.value.to);

        apiSlider = noUiSlider.create(slider, {
            start: [cSetting.value.from, cSetting.value.to],
            connect: true,
            range: {
                min,
                max,
            },
            step: 1,
        });

        apiSlider.on("update", values => {
            const newValue = values.map(v => v.toString()).map(v => parseInt(v));

            cSetting.value = {
                from: newValue[0],
                to: newValue[1]
            };
            setting = { ...cSetting };
        });


        apiSlider.on("set", () => {
            dispatch("change");
        });
    });
</script>

<SettingRow {setting}>
    <span class="slider-value">
        <ValueInput valueType="int" value={cSetting.value.from}
                    on:change={(e) => apiSlider.set([e.detail.value, cSetting.value.to])}/>
        -
        <ValueInput valueType="int" value={cSetting.value.to}
                    on:change={(e) => apiSlider.set([cSetting.value.from, e.detail.value])}/>
        {#if cSetting.suffix !== ""}
            <span>{cSetting.suffix}</span>
        {/if}
    </span>
    <div slot="below" class="slider-track" bind:this={slider}></div>
</SettingRow>
