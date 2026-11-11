<script lang="ts">
    import {createEventDispatcher, tick} from "svelte";
    import {cubicOut} from "svelte/easing";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";
    import {escapeLayer} from "../../../../integration/escape";

    export let name: string | null;
    export let options: string[];
    export let value: string;

    const dispatch = createEventDispatcher();

    const WINDOW_MARGIN = 8;
    const MAX_OPTIONS_HEIGHT = 300;

    let expanded = false;
    let dropdownHead: HTMLElement;
    let optionsElement: HTMLElement | undefined;
    let optionsStyle = "";

    $: label = (text: string) => $spaceSeperatedNames ? convertToSpacedString(text) : text;

    function portal(node: HTMLElement) {
        document.body.appendChild(node);

        return {
            destroy: () => node.remove()
        };
    }

    function isInside(target: EventTarget | null) {
        return dropdownHead.contains(target as Node) || (optionsElement?.contains(target as Node) ?? false);
    }

    function windowClickHide(e: MouseEvent) {
        if (!isInside(e.target)) {
            expanded = false;
        }
    }

    // The options scroll by themselves when there are many
    function windowScrollHide(e: Event) {
        if (!optionsElement?.contains(e.target as Node)) {
            expanded = false;
        }
    }

    function updateValue(v: string) {
        value = v;
        expanded = false;
        dispatch("change");
    }

    async function toggleExpanded() {
        expanded = !expanded;
        if (!expanded) {
            return;
        }

        await tick();
        updateOptionsPosition();
    }

    function updateOptionsPosition() {
        if (!expanded) {
            return;
        }

        const bounds = dropdownHead.getBoundingClientRect();
        const scale = bounds.width / dropdownHead.offsetWidth;
        const roomBelow = (window.innerHeight - bounds.bottom - WINDOW_MARGIN) / scale;
        optionsStyle = [
            `left: ${bounds.left}px`,
            `top: ${bounds.bottom}px`,
            `width: ${dropdownHead.offsetWidth}px`,
            `max-height: ${Math.max(Math.min(roomBelow, MAX_OPTIONS_HEIGHT), 0)}px`,
            `--dropdown-scale: ${scale}`
        ].join(";");
    }

    function closeDropdown() {
        expanded = false;
    }

    const layer = escapeLayer(closeDropdown);
    $: layer.setOpen(expanded);

    // The transform already carries the scale of the ClickGUI, so the way down goes through `translate`
    function drop(_: HTMLElement) {
        return {
            duration: 120,
            easing: cubicOut,
            css: (t: number) => `opacity: ${t}; translate: 0 ${(t - 1) * 4}px;`,
        };
    }
</script>

<svelte:window
        on:click={windowClickHide}
        on:resize={updateOptionsPosition}
        on:scroll|capture={windowScrollHide}
/>
<div class="dropdown" class:expanded>
    <button
            type="button"
            class="head"
            aria-haspopup="listbox"
            aria-expanded={expanded}
            bind:this={dropdownHead}
            on:click={toggleExpanded}
    >
        {#if name !== null}
            <span class="name">{label(name)}</span>
        {/if}
        <span class="value">{label(value)}</span>
        <span class="chevron"></span>
    </button>

    {#if expanded}
        <div class="options" role="listbox" style={optionsStyle} bind:this={optionsElement} use:portal in:drop>
            {#each options as o (o)}
                <button
                        type="button"
                        role="option"
                        aria-selected={o === value}
                        class="option"
                        class:active={o === value}
                        on:click={() => updateValue(o)}
                >
                    {label(o)}
                </button>
            {/each}
        </div>
    {/if}
</div>

<style lang="scss">
  .dropdown {
    position: relative;
    // Grows only where it has a line of its own
    flex: 1 1 170px;
    width: 170px;
    min-width: 0;
    max-width: 100%;
  }

  .head {
    display: flex;
    align-items: center;
    gap: 6px;
    width: 100%;
    height: 28px;
    padding: 0 8px;
    border: none;
    border-bottom: 2px solid var(--clickgui-control-border-color);
    background-color: var(--clickgui-input-background-color);
    color: var(--clickgui-text-color);
    font-family: "Inter", sans-serif;
    font-size: 12px;
    font-weight: 500;
    text-align: left;
    cursor: pointer;
    transition: ease border-color .15s;

    &:hover, &:focus-visible {
      border-bottom-color: var(--clickgui-text-muted-color);
    }
  }

  .expanded .head {
    border-bottom-color: var(--accent-color);
  }

  .name {
    flex: none;
    color: var(--clickgui-text-muted-color);
  }

  .value {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .chevron {
    flex: none;
    width: 9px;
    height: 6px;
    margin-left: auto;
    background: url("/img/clickgui/icon-settings-expand.svg") center / contain no-repeat;
    opacity: .8;
    transition: ease transform .15s;
  }

  .expanded .chevron {
    transform: rotate(180deg);
  }

  .options {
    --dropdown-scale: 1;

    position: fixed;
    z-index: 999999;
    box-sizing: border-box;
    padding: 3px 0;
    overflow-y: auto;
    background-color: var(--clickgui-dropdown-background-color);
    border: 1px solid var(--clickgui-divider-color);
    border-top: none;
    border-radius: 0 0 3px 3px;
    box-shadow: 0 6px 14px var(--clickgui-panel-shadow-color);
    transform: scale(var(--dropdown-scale));
    transform-origin: top left;

    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-thumb {
      border-radius: 3px;
      background-color: var(--clickgui-control-border-color);
    }
  }

  .option {
    display: block;
    width: 100%;
    height: 28px;
    padding: 0 8px;
    border: none;
    background: none;
    color: var(--clickgui-dropdown-option-color);
    font-family: "Inter", sans-serif;
    font-size: 12px;
    font-weight: 500;
    text-align: left;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    cursor: pointer;
    transition: ease color .15s, ease background-color .15s;

    &:hover, &:focus-visible {
      color: var(--clickgui-dropdown-option-hover-color);
      background-color: var(--clickgui-control-hover-background-color);
    }

    &.active {
      color: var(--clickgui-dropdown-option-selected-color);
    }
  }
</style>
