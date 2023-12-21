<script>
    export let instance;
    export let write;

    import {onMount} from "svelte";

    let type = instance.valueType;

    let name = instance.name;
    let min = instance.range.from;
    let max = instance.range.to;
    let step = type.includes("INT") ? 1 : 0.01;
    let multi = type.includes("_RANGE");

    let value;
    if (multi) {
        value = [instance.value.from, instance.value.to];
    } else {
        value = [instance.value];
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
    let valueField1 = null;
    let valueField2 = null;

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
                instance.value = {
                    from: value[0],
                    to: value[1]
                };
            } else {
                instance.value = value[0];
            }
            write();

            updateValueString();
        });

        slider.noUiSlider.on("start", () => {
            // Prevents the input field from being focused when the slider is clicked
            if (valueField1 != null) {
                valueField1.blur();
            }
            if (valueField2 != null) {
                valueField2.blur();
            }
        });
    });

    function unfocusOnEnter(event) {
        if (event.key === "Enter") {
            event.target.blur();
        }
    }
</script>

<div class="setting animation-fix">
    <div class="name">{name}</div>
    {#if multi}
        <!-- <div class="value grid-area-b ">{valueString}</div> -->
        <div class="grid-area-b multiValues">
            <input size="" type="number" on:change={slider.noUiSlider.set([this.value, null])} bind:this={valueField1} on:keydown={unfocusOnEnter} class="value multi text-align-center" value={value[0]}>
            <div class="value">-</div>
            <input size="" type="number" on:change={slider.noUiSlider.set([null, this.value])} bind:this={valueField2} on:keydown={unfocusOnEnter} class="value multi text-align-center" value={value[1]}>
        </div>
    {:else}
            <input size="" type="number" on:change={slider.noUiSlider.set([this.value])} bind:this={valueField1} on:keydown={unfocusOnEnter} class="value grid-area-b single" id="inputElem" value={valueString}>
    {/if}
    <div bind:this={slider} class="slider"/>
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
        color: var(--text);;
        font-size: 12px;
    }

    .slider {
        grid-area: c;
    }

    .value {
        grid-area: b;
        font-weight: 500;
        color: var(--text);;
        text-align: right;
        font-size: 12px;
        background-color: transparent;
        outline: none;
        border: none;
        
    }

    .text-align-center {
        text-align: center;
    }

    .single {
        width: 100%;
    }
    
    .multi {
        width: 30px;
    }
    .value:focus {
            outline: none;
    }

    .grid-area-b {
        grid-area: b;
    }

    .multiValues {
        display: flex;
    }
</style>
