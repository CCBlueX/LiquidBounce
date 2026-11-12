<script lang="ts">
    import "@simonwep/pickr/dist/themes/classic.min.css";
    import "./pickr.scss";
    import {createEventDispatcher, onMount, tick} from "svelte";
    import type {ColorSetting, ModuleSetting,} from "../../../integration/types.js";
    import Pickr from "@simonwep/pickr";
    import SettingRow from "./common/SettingRow.svelte";
    import {intToRgba, rgbaToHex, rgbaToInt} from "../../../integration/util";
    import {escapeLayer} from "../../../integration/escape";

    export let setting: ModuleSetting;

    const cSetting = setting as ColorSetting;

    const dispatch = createEventDispatcher();

    let colorPicker: HTMLElement;
    let pickr: Pickr;
    let hidden = true;

    let hex = rgbaToHex(intToRgba(cSetting.value));

    const layer = escapeLayer(() => hidden = true);
    $: layer.setOpen(!hidden);

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

    async function togglePicker() {
        hidden = !hidden;
        if (hidden) {
            return;
        }

        // Pickr places its handles by measuring, which gives nothing while the picker is not displayed
        await tick();
        pickr.setColor(hex, true);
    }

    function handleValueInput() {
        pickr.setColor(hex);
    }
</script>

<SettingRow {setting}>
    <input
            class="hex"
            spellcheck="false"
            aria-label="Hex colour"
            bind:value={hex}
            on:input={handleValueInput}
    />
    <button
            type="button"
            class="swatch"
            class:open={!hidden}
            aria-label="Edit colour"
            aria-expanded={!hidden}
            on:click={togglePicker}
            style="background-color: {hex};"
    ></button>
    <div slot="below" class="color-picker" class:hidden>
        <!-- svelte-ignore a11y_consider_explicit_label -->
        <button bind:this={colorPicker}></button>
    </div>
</SettingRow>

<style lang="scss">
  .hidden {
    display: none;
  }

  .hex {
    width: 9ch;
    min-width: 0;
    padding: 2px 0;
    border: none;
    border-bottom: 1px solid transparent;
    background: none;
    color: var(--clickgui-text-color);
    font-family: "Inter", sans-serif;
    font-size: 11px;
    font-weight: 500;
    font-variant-numeric: tabular-nums;
    text-align: right;
    text-transform: uppercase;
    transition: ease border-color .15s;

    &:hover {
      border-bottom-color: var(--clickgui-control-border-color);
    }

    &:focus {
      border-bottom-color: var(--accent-color);
    }
  }

  .swatch {
    flex: none;
    width: 26px;
    height: 16px;
    border: 1px solid var(--clickgui-control-border-color);
    border-radius: 3px;
    cursor: pointer;
    transition: ease border-color .15s;

    &:hover, &:focus-visible, &.open {
      border-color: var(--clickgui-text-color);
    }
  }
</style>
