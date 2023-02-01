<script>
    import { onMount } from "svelte";

    export let instance;

    let type = instance.getValueType().toString();

    let name = instance.getName();
    let min = instance.getRange().getStart()
    let max = instance.getRange().getEndInclusive()
    let step = type.includes("INT") ? 1 : 0.1;
    let multi = type.includes("RANGE");

    let value;
    if (multi) {
        value = [instance.get().getStart(), instance.get().getEndInclusive()];
    } else {
        value = [instance.get()];
    }

    let valueString;
    function updateValueString() {
        if (multi) {
            valueString = `${value[0]} - ${value[1]}`
        } else {
            valueString = value[0].toString();
        }
    }
    updateValueString();

    let slider = null;
    onMount(() => {
        const start = value;
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
            value = values.map(v => parseFloat(v));

            if (type.includes("INT")) {
                value[0] |= 0;
                value[1] |= 0;
            }

            if (multi) {
                if (type.includes("FLOAT")) {
                    instance.set(kotlin.floatRange(value[0], value[1]));
                } else {
                    instance.set(kotlin.intRange(value[0], value[1]));
                }
            } else {
                instance.set(value[0]);
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
