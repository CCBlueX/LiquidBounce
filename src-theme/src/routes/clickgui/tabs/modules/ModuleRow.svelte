<script lang="ts">
    import type {Module} from "../../../../integration/types";
    import {setModuleEnabled} from "../../../../integration/rest";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";
    import BindDisplay from "../../setting/bind/BindDisplay.svelte";
    import {isBound, modules, selectModule} from "./modules.svelte";

    /**
     * A left click turns the module on or off, a right click or the arrow opens its settings.
     */
    let {module, onhover}: {
        module: Module;
        onhover?: (module: Module | null) => void;
    } = $props();

    const selected = $derived(modules.selected === module.name);
    const label = $derived($spaceSeperatedNames ? convertToSpacedString(module.name) : module.name);

    let element: HTMLElement;

    // A module picked from the search or kept from last time can be anywhere in its list
    $effect(() => {
        if (selected) {
            element.scrollIntoView({block: "nearest"});
        }
    });

    function open(event: Event) {
        event.preventDefault();
        selectModule(selected ? null : module.name);
    }
</script>

<div class="row" class:enabled={module.enabled} class:selected data-module={module.name} role="listitem"
     bind:this={element}
     onmouseenter={() => onhover?.(module)} onmouseleave={() => onhover?.(null)}>
    <button type="button" class="toggle" aria-pressed={module.enabled} oncontextmenu={open}
            onclick={() => setModuleEnabled(module.name, !module.enabled)}>
        <span class="name">{label}</span>
        {#if modules.modified[module.name]}
            <span class="changed" title="Changed from default"></span>
        {/if}
    </button>
    {#if isBound(module)}
        <span class="keycap">
            <BindDisplay boundKey={module.keyBind.boundKey} modifiers={module.keyBind.modifiers}/>
        </span>
    {/if}
    <button type="button" class="open" aria-label="Settings of {label}" aria-expanded={selected} onclick={open}
            oncontextmenu={open}></button>
</div>

<style lang="scss">
  .row {
    position: relative;
    display: flex;
    align-items: center;
    height: 34px;
    margin-bottom: 2px;
    border-radius: 3px;
    break-inside: avoid;
    color: var(--clickgui-text-dimmed-color);
    transition: ease background-color .12s, ease color .12s;

    &::before {
      content: "";
      position: absolute;
      left: 3px;
      top: 9px;
      bottom: 9px;
      width: 3px;
      border-radius: 2px;
      background-color: var(--accent-color);
      opacity: 0;
      transition: ease opacity .12s, ease background-color .12s;
      pointer-events: none;
    }

    &:hover, &:focus-within {
      background-color: var(--clickgui-row-hover-background-color);
      color: var(--clickgui-text-color);
    }

    &.enabled {
      color: var(--clickgui-module-enabled-color);
      font-weight: 600;

      &::before {
        opacity: 1;
      }
    }

    &.selected {
      background-color: var(--clickgui-selected-background-color);
      color: var(--clickgui-text-color);
      font-weight: 600;

      &::before {
        background-color: var(--clickgui-text-color);
      }
    }
  }

  .toggle {
    flex: 1;
    min-width: 0;
    height: 100%;
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 0 6px 0 14px;
    border: none;
    background: none;
    color: inherit;
    font: inherit;
    font-family: "Inter", sans-serif;
    font-size: 13px;
    text-align: left;
    cursor: pointer;
  }

  .name {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .changed {
    flex: none;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background-color: var(--clickgui-changed-color);
  }

  .keycap {
    flex: none;
    min-width: 20px;
    height: 20px;
    padding: 0 5px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 3px;
    border: 1px solid var(--clickgui-keycap-border-color);
    background-color: var(--clickgui-keycap-background-color);
    font-size: 10px;
    font-weight: 600;
    color: var(--clickgui-text-color);
    pointer-events: none;
  }

  .open {
    flex: none;
    width: 28px;
    height: 28px;
    border: none;
    background: url("/img/clickgui/icon-settings-expand.svg") center no-repeat;
    // The arrow points down in the file
    transform: rotate(-90deg);
    opacity: 0;
    cursor: pointer;
    transition: ease opacity .12s;

    &:focus-visible {
      opacity: 1;
    }
  }

  .row:hover .open {
    opacity: .55;

    &:hover {
      opacity: 1;
    }
  }

  .selected .open {
    opacity: 1;
  }
</style>
