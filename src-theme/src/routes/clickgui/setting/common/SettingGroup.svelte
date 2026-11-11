<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import {slide} from "svelte/transition";
    import {cubicOut} from "svelte/easing";
    import type {ModuleSetting} from "../../../../integration/types";
    import {setItem} from "../../../../integration/persistent_storage";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";
    import {isModified} from "../defaults";
    import {enterGroup} from "../pages";
    import GenericSetting from "./GenericSetting.svelte";

    /**
     * A named group of settings below its header. It opens in place, or as a page of its own when it sits deep
     * inside a page already. The `end` slot follows the header.
     */
    export let setting: ModuleSetting;
    export let path: string;
    /**
     * How many of the group's values the header covers itself: the switch of a toggleable group.
     */
    export let skip = 0;
    /**
     * Shows the settings as out of use.
     */
    export let off = false;
    /**
     * Whether the `title` slot takes the place of the name.
     */
    export let customTitle = false;

    const thisPath = `${path}.${setting.name}`;
    const {asPage, open} = enterGroup(setting.name);
    const dispatch = createEventDispatcher();

    let element: HTMLElement;
    let expanded = localStorage.getItem(thisPath) === "true";

    $: label = $spaceSeperatedNames ? convertToSpacedString(setting.name) : setting.name;
    $: settings = setting.value as ModuleSetting[];
    // A host with a title of its own has no way to reset the group either
    $: modified = !customTitle && settings.some(isModified);
    $: openable = settings.length > skip;

    function toggle() {
        if (!openable) {
            return;
        }

        if (asPage) {
            open();
            return;
        }

        expanded = !expanded;
        setItem(thisPath, expanded.toString());
    }

    function handleChange() {
        setting = setting;
        dispatch("change");
    }

    // GenericSetting resets the group and builds it again
    function reset() {
        element.dispatchEvent(new CustomEvent("settingreset", {bubbles: true}));
    }
</script>

<div class="group" bind:this={element}>
    <!-- svelte-ignore a11y-no-static-element-interactions -->
    <div class="head" on:contextmenu|preventDefault={toggle}>
        {#if modified}
            <span class="changed" title="Changed from default"></span>
        {/if}
        {#if customTitle}
            <slot name="title" {label} {toggle} {expanded}/>
        {:else}
            <button type="button" class="title" disabled={!openable} aria-expanded={asPage ? undefined : expanded}
                    on:click={toggle}>
                <span class="arrow" class:open={expanded && !asPage} class:hidden={!openable}></span>
                <span class="name">{label}</span>
                {#if openable}
                    <span class="count">{settings.length - skip}</span>
                {/if}
            </button>
        {/if}
        {#if modified}
            <button type="button" class="reset" title="Reset to defaults" aria-label="Reset {label}"
                    on:click={reset}></button>
        {/if}
        <slot name="end"/>
    </div>

    {#if expanded && openable && !asPage}
        <div class="settings" class:off transition:slide={{duration: 180, easing: cubicOut}}>
            {#each settings as child, index (child.name)}
                {#if index >= skip}
                    <GenericSetting path={thisPath} bind:setting={child} on:change={handleChange}/>
                {/if}
            {/each}
        </div>
    {/if}
</div>

<style lang="scss">
  .head {
    position: relative;
    display: flex;
    align-items: center;
    gap: 8px;
    min-height: 34px;
  }

  .title {
    flex: 1;
    min-width: 0;
    align-self: stretch;
    display: flex;
    align-items: center;
    gap: 9px;
    padding: 0;
    border: none;
    background: none;
    font-family: "Inter", sans-serif;
    font-size: 12px;
    font-weight: 600;
    color: var(--clickgui-text-color);
    text-align: left;
    cursor: pointer;

    &:disabled {
      cursor: default;
    }
  }

  .arrow {
    flex: none;
    width: 10px;
    height: 10px;
    background: url("/img/clickgui/icon-settings-expand.svg") center no-repeat;
    opacity: .8;
    // Points down in the file
    transform: rotate(-90deg);
    transition: ease transform .18s;

    &.open {
      transform: none;
    }

    &.hidden {
      visibility: hidden;
    }
  }

  .name {
    min-width: 0;
    overflow-wrap: anywhere;
  }

  .count {
    margin-left: auto;
    padding-left: 8px;
    font-size: 11px;
    font-weight: 500;
    color: var(--clickgui-text-muted-color);
  }

  .changed {
    position: absolute;
    left: -11px;
    top: 14px;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background-color: var(--clickgui-changed-color);
  }

  .reset {
    flex: none;
    width: 20px;
    height: 20px;
    border: none;
    border-radius: 3px;
    background: url("/img/clickgui/icon-reset.svg") center / 11px no-repeat;
    opacity: .6;
    cursor: pointer;
    transition: ease opacity .15s, ease background-color .15s;

    &:hover, &:focus-visible {
      opacity: 1;
      background-color: var(--clickgui-control-hover-background-color);
    }
  }

  .settings {
    margin-bottom: 6px;
    padding: 0 12px 0 14px;
    border-left: 2px solid var(--clickgui-setting-group-border-color);
    background-color: var(--clickgui-setting-group-background-color);

    &.off {
      :global(.row), :global(.head) {
        opacity: .5;
      }
    }
  }
</style>
