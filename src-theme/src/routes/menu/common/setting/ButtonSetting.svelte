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
    border: none;
    background-color: var(--menu-button-background-color);
    color: var(--menu-text-color);
    font-family: "Inter", sans-serif;
    padding: 20px;
    border-radius: 5px;
    font-size: 20px;
    transition: ease background-color .2s, ease opacity .2s;

    &.inset {
      margin: 0 30px;
    }

    &.secondary {
      background-color: var(--menu-button-secondary-background-color);
    }

    &:not([disabled]):hover {
      background-color: var(--menu-button-hover-background-color);
      cursor: pointer;

      &.secondary {
        background-color: var(--menu-button-secondary-hover-background-color);
      }
    }

    &[disabled] {
      opacity: .6;
    }
  }
</style>