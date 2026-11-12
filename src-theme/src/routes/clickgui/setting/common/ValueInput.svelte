<script lang="ts">
    import {createEventDispatcher} from "svelte";

    export let value: number;
    export let valueType: "int" | "float";

    let inputElement: HTMLElement;
    let inputValue = "";

    $: {
        if (document.activeElement !== inputElement) {
            inputValue = value.toString();
        }
    }

    const dispatch = createEventDispatcher<{
        change: { value: number }
    }>();

    function handleInput() {
        let parsed: number;
        if (valueType === "float") {
            parsed = parseFloat(inputValue);
        } else {
            parsed = parseInt(inputValue);
        }

        if (!isNaN(parsed)) {
            dispatch("change", {value: parsed});
        }
    }

    function handleKeyDown(e: KeyboardEvent) {
        if (e.key === "Enter") {
            e.preventDefault();
        }
    }
</script>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<span contenteditable="true" class="value" bind:innerText={inputValue} on:input={handleInput} on:keydown={handleKeyDown} bind:this={inputElement}></span>

<style lang="scss">
  .value {
    display: inline-block;
    min-width: 5px;
    border-bottom: 1px solid transparent;
    color: var(--clickgui-text-color);
    font-weight: 500;
    font-size: 12px;
    font-variant-numeric: tabular-nums;
    cursor: text;
    transition: ease border-color .15s;

    &:hover {
      border-bottom-color: var(--clickgui-control-border-color);
    }

    &:focus {
      border-bottom-color: var(--accent-color);
    }
  }
</style>
