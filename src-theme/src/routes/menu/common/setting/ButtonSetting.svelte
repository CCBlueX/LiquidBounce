<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import CircleLoader from "../CircleLoader.svelte";

    export let title: string;
    export let disabled = false;
    export let secondary = false;
    export let inset = false;
    export let listenForEnter = false;
    export let loading = false;

    const dispatch = createEventDispatcher();

    function handleKeyDown(e: KeyboardEvent) {
        if (!listenForEnter) {
            return;
        }
        if (e.key === "Enter") {
            dispatch("click");
        }
    }
</script>

<svelte:window on:keydown={handleKeyDown}/>
<button class="button-setting" class:inset type="button" on:click={() => dispatch("click")} {disabled} class:secondary>
    {#if loading}
        <CircleLoader/>
    {/if}
    {title}
</button>

<style lang="scss">

  .button-setting {
    position: relative;
    border: 1px solid var(--accent-border-color);
    background-color: var(--menu-button-background-color);
    color: var(--menu-text-color);
    font-family: "Inter", sans-serif;
    font-weight: 500;
    padding: 16px 20px;
    border-radius: var(--radius-sm);
    font-size: 18px;
    transition: background-color var(--duration-fast) var(--ease-standard),
    border-color var(--duration-fast) var(--ease-standard),
    opacity var(--duration-fast) var(--ease-standard);

    &.inset {
      margin: 0 30px;
    }

    &.secondary {
      background-color: var(--menu-button-secondary-background-color);
    }

    &:not([disabled]) {
      cursor: pointer;
    }

    &:not([disabled]):hover {
      background-color: var(--menu-button-hover-background-color);
      border-color: var(--accent-border-strong-color);

      &.secondary {
        background-color: var(--menu-button-secondary-hover-background-color);
      }
    }

    &[disabled] {
      cursor: not-allowed;
      opacity: .45;
    }
  }
</style>