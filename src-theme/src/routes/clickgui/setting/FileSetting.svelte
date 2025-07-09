<script lang="ts">
    import type {FileSetting, ModuleSetting} from "../../../integration/types";
    import {createEventDispatcher, onMount, tick} from "svelte";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import {openFileDialog, openInExplorer} from "../../../integration/rest";

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
        console.log("lol")
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
        if (cSetting.value) {
            await openInExplorer(cSetting.value)
        }
    }

    let pathEl: HTMLSpanElement;
    let fullText = '';

    $: fullText = cSetting.value ?? 'Nothing';

    let inactivityTimeout: ReturnType<typeof setTimeout> | null = null;

    let isDragging = false;
    let dragStartX = 0;
    let dragStartY = 0;
    let scrollStartX = 0;
    let dragDetected = false;

    let canScroll = false;
    let leftHidden = false;
    let rightHidden = false;

    function handlePointerDown(e: PointerEvent) {
        if (!pathEl || !canScroll) return;

        isDragging = true;
        dragDetected = false;
        dragStartX = e.clientX;
        dragStartY = e.clientY;
        scrollStartX = pathEl.scrollLeft;
        pathEl.setPointerCapture(e.pointerId);

        if (inactivityTimeout) clearTimeout(inactivityTimeout);
        e.preventDefault();
    }

    function handlePointerMove(e: PointerEvent) {
        if (!isDragging || !pathEl || !canScroll) return;

        const dx = e.clientX - dragStartX;
        const dy = e.clientY - dragStartY;

        if (!dragDetected && (Math.abs(dx) > 5 || Math.abs(dy) > 5)) {
            dragDetected = true;
        }

        pathEl.scrollLeft = scrollStartX - dx;
        e.preventDefault();
    }

    function handlePointerUp(e: PointerEvent) {
        if (!isDragging || !pathEl || !canScroll) return;

        isDragging = false;
        pathEl.releasePointerCapture(e.pointerId);
        handleScrollActivity();

        if (!dragDetected) {
            browseFile()
        }
    }

    function updateScrollShadows() {
        if (!pathEl) return;

        const scrollLeft = pathEl.scrollLeft;
        const maxScrollLeft = pathEl.scrollWidth - pathEl.clientWidth;

        leftHidden = scrollLeft > 0;
        rightHidden = scrollLeft < maxScrollLeft;
    }

    function handleScrollActivity() {
        if (inactivityTimeout) clearTimeout(inactivityTimeout);

        inactivityTimeout = setTimeout(() => {
            adjustScrollAlignment();
        }, 2000);
    }

    function handleScroll() {
        handleScrollActivity();
        updateScrollShadows();
    }

    async function adjustScrollAlignment() {
        await tick();
        if (!pathEl) return;

        const el = pathEl;
        const fits = el.scrollWidth <= el.clientWidth;
        canScroll = el.scrollWidth > el.clientWidth;

        el.scrollTo({
            left: fits ? 0 : el.scrollWidth,
            behavior: 'smooth'
        });

        updateScrollShadows();
    }

    onMount(() => {
        adjustScrollAlignment();

        pathEl?.addEventListener('pointerdown', handlePointerDown);
        pathEl?.addEventListener('pointermove', handlePointerMove);
        pathEl?.addEventListener('pointerup', handlePointerUp);
        pathEl?.addEventListener('scroll', handleScroll);
        window.addEventListener('resize', adjustScrollAlignment);

        return () => {
            pathEl?.removeEventListener('pointerdown', handlePointerDown);
            pathEl?.removeEventListener('pointermove', handlePointerMove);
            pathEl?.removeEventListener('pointerup', handlePointerUp);
            pathEl?.removeEventListener('scroll', handleScroll);
            window.removeEventListener('resize', adjustScrollAlignment);
        };
    });

    $: if (fullText) {
        adjustScrollAlignment();
    }
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="setting">
    <div class="name">{spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>

    <div class="value">
        <div class="path-wrapper"
             class:left-shadow="{leftHidden}"
             class:right-shadow="{rightHidden}"
        >
            <span class="path muted"
                  bind:this={pathEl}
                  class:scrolling="{isDragging}"
            >
              {fullText}
            </span>
        </div>
        <div class="buttons">
            <div class="button" onclick={removeSelected} class:disabled={cSetting.value === undefined}>Remove</div>
            <div class="button" onclick={selectFile}>Select</div>
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

  .path-wrapper {
    position: relative;
    grid-area: path;

    white-space: nowrap;
    -webkit-mask-image: none;
    mask-image: none;

    &.left-shadow {
      mask-image: linear-gradient(to right, transparent 0%, black 20%, black 100%);
    }

    &.right-shadow {
      mask-image: linear-gradient(to left, transparent 0%, black 20%, black 100%);
    }

    &.left-shadow.right-shadow {
      mask-image: linear-gradient(to right, transparent 0%, black 20%, black 80%, transparent 100%);
    }
  }

  .path {
    display: inline-block;
    overflow-x: auto;
    overflow-y: hidden;
    white-space: nowrap;
    width: 100%;
    font-family: monospace;
    user-select: none;
    scroll-behavior: smooth;

    &::-webkit-scrollbar {
      display: none;
    }

    scrollbar-width: none;

    cursor: pointer;

    &.scrolling {
      cursor: grabbing;
    }
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
