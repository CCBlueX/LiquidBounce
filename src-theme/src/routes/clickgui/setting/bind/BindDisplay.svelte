<script lang="ts">
    import type {BindModifier} from "../../../../integration/types";
    import {os} from "../../clickgui_store";
    import {keyName} from "./key_names.svelte";
    import {UNKNOWN_KEY} from "../../../../util/utils";

    export let boundKey: string | undefined;
    export let modifiers: Iterable<BindModifier> = [];
    export let literal: boolean = false;

    const getRenderString = (modifier: BindModifier) => {
        switch ($os) {
            case "windows":
                switch (modifier) {
                    case "Control":
                        return "Ctrl";
                    case "Super":
                        return "\u229e";
                    default:
                        return modifier;
                }
            case "mac":
                switch (modifier) {
                    case "Shift":
                        return "\u21e7";
                    case "Control":
                        return "^";
                    case "Alt":
                        return "\u2325";
                    case "Super":
                        return "\u2318";
                    default:
                        return modifier;
                }
            default:
                return modifier;
        }
    };
</script>

<span class="wrapper">
    {#if boundKey !== undefined && boundKey !== UNKNOWN_KEY}
        {#each modifiers as modifier (modifier)}
            <span class="modifier">{getRenderString(modifier)}</span>
        {/each}
        <span class="boundKey">{literal ? boundKey : keyName(boundKey)}</span>
    {:else}
        <span class="dimmed">None</span>
    {/if}
</span>

<style lang="scss">

  .wrapper {
    column-gap: 2px;
    display: flex;
    align-items: center;
  }

  .dimmed {
    color: var(--clickgui-text-dimmed-color);
  }

  .modifier:after {
    content: "+";
    color: var(--clickgui-text-dimmed-color);
    opacity: 0.8;
    line-height: 1;
    font-family: monospace;
    margin-left: 2px;
  }

  .boundKey {
    font-weight: bold;
  }
</style>
