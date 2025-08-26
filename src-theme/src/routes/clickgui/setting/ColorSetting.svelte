<script lang="ts">
    import "@simonwep/pickr/dist/themes/classic.min.css";
    import "./pickr.scss";
    import {createEventDispatcher, onMount} from "svelte";
    import type {ColorSetting, ModuleSetting,} from "../../../integration/types.js";
    import Pickr from "@simonwep/pickr";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import {intToRgba, rgbaToHex, rgbaToInt} from "../../../integration/util";

    export let setting: ModuleSetting;

    const cSetting = setting as ColorSetting;

    const dispatch = createEventDispatcher();

    let colorPicker: HTMLElement;
    let pickr: Pickr;
    let hidden = true;

    let hex = rgbaToHex(intToRgba(cSetting.value));

    onMount(() => {
        pickr = Pickr.create({
            el: colorPicker,
            theme: "classic",
            showAlways: true,
            inline: true,
            default: rgbaToHex(intToRgba(cSetting.value)),

            components: {
                preview: false,
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
                    save: false,
                },
            },
        });

        pickr.on("change", (v: any) => {
            hex = v.toHEXA().toString();

            const [r, g, b, a] = v.toRGBA();
            const rgba = [r, g, b, a * 255];

            cSetting.value = rgbaToInt(rgba);
            setting = { ...cSetting };
            dispatch("change");
        });
    });

    function handleValueInput() {
        pickr.setColor(hex);
    }
</script>

<div class="setting">
    <div class="name">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>
    <div class="value-spot">
        <input
            class="value"
            bind:value={hex}
            on:input={handleValueInput}
        />
        <!-- svelte-ignore a11y_consider_explicit_label -->
        <button
            class="color-pickr-button"
            on:click={() => (hidden = !hidden)}
            style="background-color: {hex};"
        ></button>
    </div>
    <!-- svelte-ignore a11y_consider_explicit_label -->
    <div class="color-picker" class:hidden>
        <!-- svelte-ignore element_invalid_self_closing_tag -->
        <button bind:this={colorPicker} />
    </div>
</div>

<style lang="scss">
    @use "../../../colors.scss" as *;

    .setting {
        display: grid;
        grid-template-areas:
            "a b"
            "c c";
        padding: 7px 0px;
    }

    .name {
        grid-area: a;
        font-weight: 500;
        color: $clickgui-text-color;
        font-size: 12px;
    }

    .hidden {
        height: 0px;
        display: none;
    }

    .value {
        font-weight: 500;
        color: $clickgui-text-color;
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
        width: 70px;
        font-family: monospace;
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
        margin-top: -2px;
        margin-bottom: -2px;
        width: 30px;
        border-radius: 3px;
        background-color: blue;
        border-style: none;
    }
    .color-pickr-button:focus {
        outline: 3px solid #ffffff;
    }
</style>
