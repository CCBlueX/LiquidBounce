<script>
    import {onMount} from "svelte";

    export let instance;

    let name = instance.getName();
    let value = instance.get().toHex();

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
                preview: true,
                opacity: true,
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
            const rgba = v.toRGBA().map(val => val | 0)
            value = v.toHEXA();
            kotlin.log(String(rgba))
            instance.set(kotlin.color(rgba[0], rgba[1], rgba[2], rgba[3]))
        });
    });

    // TODO: use hex colors
    function handleValueChange(e) {
        /*         const v = e.target.value;
                if (v.length === 6) {
                    pickr.setColor(`#${v}`);
                } */
    }

    let hidden = true

    function togglePickr() {
        hidden = !hidden
    }
</script>

<div class="setting">
    <div class="name">{name}</div>
    <div class="value-spot">
        <input class="value" {value} on:input={handleValueChange}>
        <button class="color-pickr-button" on:click={togglePickr} style="background-color: {value};"></button>
    </div>
    <div class="animation-fix color-picker" class:hidden={hidden}>;
        <button bind:this={colorPicker}/>
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

    .hidden {
        height: 0px;
        display: none;
    }

    .value {
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
        margin-right: 15px;
        margin-left: auto;
        width: 50px;
    }

    .value-spot {
        grid-area: b;
        display: flex;
    
        align-items: stretch;
    }

    .color-picker {
        grid-area: c;
    }

    .color-pickr-button {
        margin-top: -4px;
        margin-bottom: -4px;
        width: 30px;
        border-radius: 3px;
        background-color: blue;
        border-style: none;

    }
    .color-pickr-button:focus {
        outline: 3px solid  #ffffff;
    }
</style>
