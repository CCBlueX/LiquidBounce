<button type="button" class="text-button" on:click>
    <slot/>
</button>

<style lang="scss">
  .text-button {
    flex: none;
    height: 20px;
    padding: 0 5px;
    border: none;
    border-radius: 3px;
    background: none;
    color: var(--clickgui-text-muted-color);
    font-family: "Inter", sans-serif;
    font-size: 11px;
    font-weight: 500;
    cursor: pointer;
    transition: ease color .15s, ease background-color .15s;

    &:hover, &:focus-visible {
      color: var(--clickgui-text-color);
      background-color: var(--clickgui-control-hover-background-color);
    }
  }
</style>
