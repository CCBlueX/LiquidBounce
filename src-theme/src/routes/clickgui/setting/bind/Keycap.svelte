<script lang="ts">
    /**
     * The key a setting is bound to, drawn as a key. Clicking it starts or cancels binding.
     */
    export let muted = false;
    export let binding = false;
</script>

<button type="button" class="keycap" class:muted class:binding on:click on:mouseenter on:mouseleave>
    <slot/>
</button>

<style lang="scss">
  .keycap {
    flex: none;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 26px;
    max-width: 100%;
    height: 24px;
    padding: 0 7px;
    border: 1px solid var(--clickgui-keycap-border-color);
    border-radius: 3px;
    background-color: var(--clickgui-keycap-background-color);
    color: var(--clickgui-text-color);
    font-family: "Inter", sans-serif;
    font-size: 11px;
    font-weight: 600;
    white-space: nowrap;
    cursor: pointer;
    transition: ease border-color .15s, ease background-color .15s, ease color .15s;

    &:hover, &:focus-visible {
      background-color: var(--clickgui-button-secondary-hover-background-color);
    }

    &.muted {
      color: var(--clickgui-text-muted-color);
    }

    &.binding {
      border-color: var(--accent-color);
      color: var(--clickgui-text-color);
    }
  }
</style>
