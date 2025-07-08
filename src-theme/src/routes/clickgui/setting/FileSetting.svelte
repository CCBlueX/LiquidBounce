<script lang="ts">
    import type {FileSetting, ModuleSetting} from "../../../integration/types";
    import {createEventDispatcher, onMount, tick} from "svelte";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import {openFileDialog} from "../../../integration/rest";

    export let setting: ModuleSetting;

    const cSetting = setting as FileSetting;
    let selecting = false

    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = { ...cSetting };
        dispatch("change");
    }

    function removeSelected() {
        cSetting.value = undefined;
        handleChange()
    }

    async function selectFile() {
        if (selecting) {
            return
        }

        selecting = true;

        let file = await openFileDialog({
            mode: cSetting.dialogMode,
            supportedExtensions: cSetting.supportedExtensions
        })

        selecting = false;
        if (!file.cancelled) {
            cSetting.value = file.file;
            handleChange()
        }
    }

    async function browseFile() {

    }

    let pathEl: HTMLSpanElement;
    let fullText = '';
    let isTruncated = false;

    $: fullText = cSetting.value ?? 'Not set';

    function trimStartDynamic(element: HTMLElement, text: string) {
        const style = window.getComputedStyle(element);
        const font = `${style.fontWeight} ${style.fontSize} ${style.fontFamily}`;
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d')!!;
        ctx.font = font;

        const containerWidth = element.clientWidth;
        if (ctx.measureText(text).width <= containerWidth) {
            isTruncated = false;
            return text;
        }

        let left = 0;
        let right = text.length;
        let trimmed = text;

        while (left < right) {
            const mid = Math.floor((left + right) / 2);
            const testText = '...' + text.slice(mid);
            if (ctx.measureText(testText).width > containerWidth) {
                left = mid + 1;
            } else {
                trimmed = testText;
                right = mid;
            }
        }

        isTruncated = true;
        return trimmed;
    }

    async function updateTrimmedText() {
        await tick();
        if (!pathEl) return;

        pathEl.textContent = trimStartDynamic(pathEl, fullText);
    }

    onMount(() => {
        updateTrimmedText();

        window.addEventListener('resize', updateTrimmedText);
        return () => window.removeEventListener('resize', updateTrimmedText);
    });

    $: if (fullText) {
        updateTrimmedText();
    }
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="setting">
    <div class="name">{spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>

    <div class="value">
        <span class="path muted" onclick={selectFile} bind:this={pathEl} class:truncated={isTruncated}>
          {fullText}
        </span>
        <div class="buttons">
            <div class="button" onclick={removeSelected} class:disabled={cSetting.value === undefined}>Remove</div>
            <div class="button" onclick={browseFile} class:disabled={cSetting.value === undefined}>Browse</div>
        </div>
    </div>
</div>

<style lang="scss">
  @use "../../../colors.scss" as *;

  .setting {
    padding: 7px 0 2px 0;
    display: grid;
    grid-template-areas:
    "name buttons"
    "path path";
    grid-template-columns: 1fr max-content;
    column-gap: 5px;

    min-height: 46px;
  }

  .name {
    grid-area: name;
  }

  .value {
    display: contents;
  }

  .path {
    grid-area: path;
    justify-self: start;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    width: 100%;
    cursor: pointer;

    font-family: monospace;
  }

  .path.truncated {
    text-align: right;
  }

  .buttons {
    grid-area: buttons;
    display: flex;
    place-self: start;
    flex-direction: row;
    gap: 5px;
  }

  .button {
    background-color: $accent-color;
    cursor: pointer;
    display: flex;
    align-items: center;
    align-content: center;
    justify-content: center;
  }

  .disabled {
    opacity: 0.7;
    pointer-events: none;
  }

  .value,
  .setting {
    color: $clickgui-text-color;
    font-weight: 500;
    font-size: 12px;
  }

  .name {
    grid-area: name;
    font-weight: 500;
  }

  .muted {
    color: $clickgui-text-dimmed-color;
  }
</style>
