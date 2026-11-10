<script lang="ts">
    import type {Snippet} from "svelte";

    /**
     * [pressed] makes it a toggle that is on.
     */
    let {onclick, variant = "primary", pressed, disabled = false, autofocus = false, children}: {
        onclick: () => void;
        variant?: "primary" | "secondary" | "danger";
        pressed?: boolean;
        disabled?: boolean;
        autofocus?: boolean;
        children: Snippet;
    } = $props();
</script>

<!-- svelte-ignore a11y_autofocus -->
<button type="button" class="button {variant}" class:pressed aria-pressed={pressed} {disabled} {autofocus} {onclick}>
    {@render children()}
</button>

<style lang="scss">
  .button {
    font-family: "Inter", sans-serif;
    font-weight: 500;
    font-size: 12px;
    color: var(--clickgui-text-color);
    background-color: var(--clickgui-button-background-color);
    border: none;
    padding: 6px 12px;
    border-radius: 3px;
    white-space: nowrap;
    cursor: pointer;
    transition: ease background-color .2s, ease opacity .2s;

    &:hover:not(:disabled) {
      background-color: var(--clickgui-button-hover-background-color);
    }

    &:focus-visible {
      outline: solid 1px var(--clickgui-text-color);
      outline-offset: 1px;
    }

    &:disabled {
      opacity: 0.5;
      cursor: default;
    }

    &.secondary {
      background-color: var(--clickgui-button-secondary-background-color);

      &:hover:not(:disabled) {
        background-color: var(--clickgui-button-secondary-hover-background-color);
      }
    }

    &.danger {
      color: var(--clickgui-button-danger-color);
      background-color: var(--clickgui-button-danger-background-color);

      &:hover:not(:disabled) {
        background-color: var(--clickgui-button-danger-hover-background-color);
      }
    }

    &.pressed {
      color: var(--clickgui-chip-active-color);
      background-color: var(--clickgui-chip-active-background-color);
    }
  }
</style>
