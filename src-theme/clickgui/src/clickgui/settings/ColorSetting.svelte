<script>
    import { onMount } from "svelte";

    export let name;
    export let value;

    let colorPicker = null;
    let pickr = null;

    onMount(() => {
        pickr = Pickr.create({
            el: colorPicker,
            theme: "classic", 
            showAlways: true,
            inline: true,
            default: value,

            components: {
                preview: false,
                opacity: false,
                hue: true,

                interaction: {
                    hex: false,
                    rgba: false,
                    hsla: false,
                    hsva: false,
                    cmyk: false,
                    input: false,
                    clear: false,
                    save: false
                }
            }
        });

        pickr.on("change", v => {
            value = `RGBA(${v.toRGBA().map(val => val | 0).join(", ")})`;
        });
    });

    // TODO: use hex colors
    function handleValueChange(e) {
/*         const v = e.target.value;
        if (v.length === 6) {
            pickr.setColor(`#${v}`);
        } */
    }
</script>

<div class="setting">
    <div class="name">{name}</div>
    <input class="value" {value} on:input={handleValueChange}>
    <div class="animation-fix color-picker">
        <div bind:this={colorPicker} />
    </div>
</div>

<style>
    .setting {
        display: grid;
        grid-template-areas:
            "a b"
            "c c";
        padding: 7px 10px;
    }

    .name {
        grid-area: a;
        font-weight: 500;
        color: white;
        font-size: 12px;
    }

    /* Fix glitching of settings expand animation */
    .animation-fix {
        height: 138px;
    }

    .value {
        grid-area: b;
        font-weight: 500;
        color: white;
        text-align: right;
        font-size: 12px;
        cursor: text;
        text-transform: uppercase;
        background-color: transparent;
        border: none;
        padding: 0;
        margin: 0;
    }

    .color-picker {
        grid-area: c;
    }
</style>
