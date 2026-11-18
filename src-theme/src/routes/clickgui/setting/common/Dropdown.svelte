<script lang="ts">
    import {createEventDispatcher, tick} from "svelte";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";
    import {escapeLayer} from "../../../../integration/escape";

    export let name: string | null;
    export let options: string[];
    export let value: string;

    const dispatch = createEventDispatcher();

    let expanded = false;
    let dropdownHead: HTMLElement;
    let optionsStyle = "";

    function portal(node: HTMLElement) {
        document.body.appendChild(node);

        return {
            destroy: () => node.remove()
        };
    }

    function windowClickHide(e: MouseEvent) {
        if (!dropdownHead.contains(e.target as Node)) {
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
        optionsStyle = [
            `left: ${bounds.left}px`,
            `top: ${bounds.bottom}px`,
            `width: ${dropdownHead.offsetWidth}px`,
            `--dropdown-scale: ${scale}`
        ].join(";");
    }

    function closeDropdown() {
        expanded = false;
    }

    const layer = escapeLayer(closeDropdown);
    $: layer.setOpen(expanded);
</script>

<svelte:window
        on:click={windowClickHide}
        on:resize={updateOptionsPosition}
        on:scroll|capture={closeDropdown}
/>
<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="dropdown" class:expanded>
    <div class="head" bind:this={dropdownHead} on:click={toggleExpanded}>
        {#if name !== null}
            <span class="text">{$spaceSeperatedNames ? convertToSpacedString(name) : name}
                &bull; {$spaceSeperatedNames ? convertToSpacedString(value) : value}</span>
        {:else}
            <span class="text">{$spaceSeperatedNames ? convertToSpacedString(value) : value}</span>
        {/if}
    </div>

    {#if expanded}
        <div class="options" style={optionsStyle} use:portal>
            {#each options as o (o)}
                <div
                        class="option"
                        class:active={o === value}
                        on:click={() => updateValue(o)}
                >
                    {$spaceSeperatedNames ? convertToSpacedString(o) : o}
                </div>
            {/each}
        </div>
    {/if}
</div>

<style lang="scss">
  @use "../../icon-settings-expand" as *;

  .dropdown {
    position: relative;

    &.expanded {
      .text::after {
        transform: translateY(-50%) rotate(0);
        opacity: 1;
      }

      .head {
        border-radius: var(--radius-xs) var(--radius-xs) 0 0;
        border-color: var(--accent-border-strong-color);
      }
    }
  }

  .head {
    background-color: var(--clickgui-dropdown-trigger-background-color);
    padding: 7px 10px;
    cursor: pointer;
    display: flex;
    align-items: center;
    position: relative;
    border: 1px solid var(--accent-border-color);
    border-radius: var(--radius-xs);
    transition: border-radius var(--duration-fast) var(--ease-standard),
    border-color var(--duration-fast) var(--ease-standard),
    background-color var(--duration-fast) var(--ease-standard);

    &:hover {
      background-color: var(--clickgui-button-hover-background-color);
    }

    .text {
      font-weight: 500;
      color: var(--clickgui-text-color);
      font-size: 12px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      margin-right: 20px;
    }

    .text::after {
      @include icon-settings-expand();
    }
  }

  .options {
    --dropdown-scale: 1;

    padding: 5px;
    background-color: var(--clickgui-dropdown-background-color);
    border: solid 1px var(--clickgui-dropdown-border-color);
    border-radius: 0 0 var(--radius-xs) var(--radius-xs);
    box-shadow: var(--elevation-md) var(--clickgui-panel-shadow-color);
    z-index: 999999;
    position: fixed;
    box-sizing: border-box;
    transform: scale(var(--dropdown-scale));
    transform-origin: top left;

    .option {
      color: var(--clickgui-dropdown-option-color);
      font-weight: 500;
      font-size: 12px;
      padding: 6px;
      border-radius: var(--radius-xs);
      cursor: pointer;
      text-align: center;
      transition: color var(--duration-fast) var(--ease-standard),
      background-color var(--duration-fast) var(--ease-standard);

      &:hover {
        color: var(--clickgui-dropdown-option-hover-color);
        background-color: var(--accent-subtle-background-color);
      }

      &.active {
        color: var(--clickgui-dropdown-option-selected-color);
        background-color: var(--accent-subtle-background-color);
      }
    }
  }
</style>
