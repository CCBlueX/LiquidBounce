<script>
    import { onMount } from "svelte";

    export let name;
    export let min;
    export let max;
    export let step;
    export let value1;
    export let value2;
    export let setValue1;
    export let setValue2;

    let multi = value2 !== null;

    let valueString;
    function updateValueString() {
        if (multi) {
            valueString = `${value1} - ${value2}`
        } else {
            valueString = value1.toString();
        }
    }

    let slider = null;
    onMount(() => {
        const start = [value1];
        if (multi) {
            start.push(value2);
        }
        let connect = "lower";
        if (multi) {
            connect = true;
        }

        noUiSlider.create(slider, {
            start: start,
            connect: connect,
            padding: [0, 0],
            range: {
                min: min,
                max: max,
            },
            step: step
        });

        slider.noUiSlider.on("update", values => {
            value1 = parseFloat(values[0]);
            setValue1(value1);
            if (multi) {
                value2 = parseFloat(values[1]);
                setValue2(value2);
            }

            updateValueString();
        });
    });
</script>

<div class="setting animation-fix">
    <div class="name">{name}</div>
    <div class="value">{valueString}</div>
    <div bind:this={slider} class="slider" />
</div>

<style>
    .setting {
        display: grid;
        grid-template-areas:
            "a b"
            "c c";
        grid-template-columns: 1fr;
        padding: 7px 10px;
    }

    /* Fix glitching of settings expand animation */
    .animation-fix {
        min-height: 51px;
    }

    .name {
        grid-area: a;
        font-weight: 500;
        color: white;
        font-size: 12px;
    }

    .slider {
        grid-area: c;
    }

    .value {
        grid-area: b;
        font-weight: 500;
        color: white;
        text-align: right;
        font-size: 12px;
    }
</style>
