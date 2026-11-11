<script lang="ts">
    import type {ModuleSetting} from "../../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";
    import {isModified} from "../defaults";

    /**
     * The line every setting is shown on: its name, whether it differs from its default with a way back, and its
     * control beside the name. Content of the `below` slot takes the full width under that line.
     */
    export let setting: ModuleSetting;
    export let label: string | null = null;
    /**
     * Puts the control under the name instead of beside it.
     */
    export let stacked = false;

    let element: HTMLElement;

    $: modified = isModified(setting);
    $: name = label ?? ($spaceSeperatedNames ? convertToSpacedString(setting.name) : setting.name);

    // GenericSetting resets the value and rebuilds the control, which only read its value when it was created
    function reset() {
        element.dispatchEvent(new CustomEvent("settingreset", {bubbles: true}));
    }
</script>

<div class="row" class:modified class:stacked bind:this={element}>
    <div class="label">
        {#if modified}
            <span class="changed" title="Changed from default"></span>
        {/if}
        <span class="name">{name}</span>
        {#if modified}
            <button class="reset" type="button" title="Reset to default" aria-label="Reset {name}" on:click={reset}></button>
        {/if}
    </div>
    {#if $$slots.default}
        <div class="control">
            <slot/>
        </div>
    {/if}
    {#if $$slots.below}
        <div class="below">
            <slot name="below"/>
        </div>
    {/if}
</div>

<style lang="scss">
  .row {
    position: relative;
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    column-gap: 10px;
    min-height: 32px;
    font-size: 12px;
    font-weight: 500;
    color: var(--clickgui-text-dimmed-color);
  }

  // The name takes the room that is left over. In a row too narrow for both, the control wraps onto a line of
  // its own and grows to fill it.
  .label {
    flex: 9999 1 auto;
    min-width: 0;
    display: flex;
    align-items: center;
    gap: 6px;
    min-height: 32px;
  }

  .name {
    overflow-wrap: anywhere;
    transition: ease color .15s;
  }

  .modified .name {
    color: var(--clickgui-text-color);
    font-weight: 600;
  }

  // In the gutter left of the name, so a change does not move the text
  .changed {
    position: absolute;
    left: -11px;
    top: 13px;
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

  .control {
    flex: 1 1 auto;
    max-width: 100%;
    min-width: 0;
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 6px;
    color: var(--clickgui-text-color);
  }

  .stacked .control {
    flex: 1 0 100%;
    justify-content: stretch;
    padding-bottom: 6px;
  }

  .below {
    flex: 0 0 100%;
    min-width: 0;
  }
</style>
