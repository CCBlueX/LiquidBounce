<script lang="ts">
    import type {FileSetting, ModuleSetting} from "../../../integration/types";
    import {createEventDispatcher, onMount, tick} from "svelte";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import {browsePath, getClientInfo, openFileDialog} from "../../../integration/rest";

    export let setting: ModuleSetting;
    const cSetting = setting as FileSetting;
    let selecting = false;
    let clientDir: string;
    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = {...cSetting};
        dispatch("change");
    }

    function removeSelected() {
        cSetting.value = '';
        handleChange();
    }

    async function selectFile() {
        if (selecting) {
            return;
        }
        selecting = true;
        let file = await openFileDialog({
            mode: cSetting.dialogMode,
            supportedExtensions: cSetting.supportedExtensions
        })
        selecting = false;
        if (file.file !== undefined) {
            cSetting.value = file.file;
            handleChange();
        }
    }

    let pathEl: HTMLSpanElement;
    let fullPathText = '';
    const removePrefix = (str: string, prefix: string) => {
        if (str.startsWith(prefix)) {
            return str.substring(prefix.length);
        }
        return str;
    }
    $: fullPathText = removePrefix(cSetting.value, clientDir) || '<ClientFolder>';
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
        pathEl.scrollLeft = scrollStartX - dx * 2.5;
        e.preventDefault();
    }

    function handlePointerUp(e: PointerEvent) {
        if (!canScroll) { // Short name
            browsePath(cSetting.value);
            return;
        }
        if (!isDragging || !pathEl || !canScroll) return;
        isDragging = false;
        pathEl.releasePointerCapture(e.pointerId);
        handleScrollActivity();
        if (!dragDetected) {
            browsePath(cSetting.value);
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
        getClientInfo().then(clientInfo => {
            clientDir = clientInfo.clientDir;
            if (!clientDir.endsWith('/')) {
                clientDir += '/';
            }
        });
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
    $: if (fullPathText) {
        adjustScrollAlignment();
    }
    $: pathParts = splitPath(fullPathText);

    function splitPath(path: string): { text: string, muted: boolean }[] {
        const parts: { text: string, muted: boolean }[] = [];
        for (let i = 0; i < path.length; i++) {
            const char = path[i];
            if (char === '/' || char === '\\') {
                parts.push({text: char, muted: true});
            } else {
                let j = i;
                while (j < path.length && path[j] !== '/' && path[j] !== '\\') {
                    j++;
                }
                parts.push({text: path.slice(i, j), muted: false});
                i = j - 1;
            }
        }
        return parts;
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
                {#each pathParts as part}
                    <span class:muted-part={part.muted}>{part.text}</span>
                {/each}
            </span>
        </div>
        <div class="buttons">
            <button class="button" onclick={removeSelected} disabled={cSetting.value === ''}>
                <img src="img/menu/icon-remove-file.svg" alt="remove-file"/>
            </button>
            <button class="button" onclick={selectFile}>
                <img src="img/menu/icon-link-file.svg" alt="link-file"/>
            </button>
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
    overflow: hidden;
    white-space: nowrap;
    width: 100%;
    font-family: monospace;
    user-select: none;

    &::-webkit-scrollbar {
      display: none;
    }

    scrollbar-width: none;
    cursor: pointer;

    &.scrolling {
      scroll-behavior: auto;
      cursor: grabbing;
    }

    & > .muted-part {
      opacity: .7;
    }
  }

  .buttons {
    grid-area: buttons;
    display: flex;
    place-self: start;
    flex-direction: row;
  }

  .button {
    border: none;
    background-color: transparent;
    color: $text-color;
    cursor: pointer;
    display: flex;
    align-items: center;
    align-content: center;
    justify-content: center;

    &:disabled {
      opacity: 0.7;
      pointer-events: none;
    }

    & > img {
      width: 16px;
      height: 16px;
    }
  }

  .value,
  .setting {
    color: $text-color;
    font-weight: 500;
    font-size: 12px;
  }

  .name {
    grid-area: name;
    font-weight: 500;
  }

  .muted {
    color: $text-color;
  }
</style>
