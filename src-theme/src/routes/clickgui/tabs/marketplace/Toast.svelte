<script lang="ts">
    import {fly} from "svelte/transition";
    import {toast} from "./marketplace";

    const SHOWN = 4000;

    let timeout: ReturnType<typeof setTimeout> | undefined;

    $effect(() => {
        if ($toast) {
            clearTimeout(timeout);
            timeout = setTimeout(() => toast.set(null), SHOWN);
        }
    });
</script>

{#if $toast}
    {#key $toast.id}
        <div class="toast" class:error={$toast.error} transition:fly={{y: -10, duration: 200}}>
            {$toast.message}
        </div>
    {/key}
{/if}

<style lang="scss">
  .toast {
    position: absolute;
    top: 18px;
    right: 24px;
    max-width: 380px;
    padding: 10px 14px;
    font-size: 12px;
    font-weight: 500;
    line-height: 1.4;
    color: var(--clickgui-text-color);
    background-color: var(--clickgui-description-background-color);
    border-left: solid 4px var(--clickgui-module-settings-border-color);
    border-radius: 0 5px 5px 0;
    box-shadow: 0 0 10px var(--clickgui-description-shadow-color);
    z-index: 2000;

    &.error {
      border-left-color: var(--clickgui-selection-chip-remove-color);
    }
  }
</style>
