<script lang="ts">
    let {
        title,
        onclick,
        primary = false,
        danger = false,
        active = false,
        disabled = false,
        mono = false
    }: {
        title: string;
        onclick: () => void;
        primary?: boolean;
        danger?: boolean;
        active?: boolean;
        disabled?: boolean;
        mono?: boolean;
    } = $props();

    function click(e: MouseEvent) {
        e.stopPropagation();
        onclick();
    }
</script>

<button type="button" class="button" class:primary class:danger class:active class:mono {disabled} onclick={click}>
    {title}
</button>

<style lang="scss">
  .button {
    font-family: "Inter", sans-serif;
    font-size: 12px;
    font-weight: 600;
    white-space: nowrap;
    color: var(--clickgui-text-color);
    background-color: transparent;
    border: solid 1px var(--clickgui-global-settings-divider-color);
    padding: 5px 14px;
    border-radius: 999px;
    cursor: pointer;
    transition: ease background-color .2s, ease border-color .2s, ease color .2s;

    &:hover:not(:disabled) {
      background-color: var(--clickgui-tab-hover-background-color);
    }

    &.active {
      background-color: var(--clickgui-tab-active-background-color);
      border-color: var(--clickgui-tab-active-border-color);
    }

    &.primary {
      background-color: var(--clickgui-button-background-color);
      border-color: var(--clickgui-button-background-color);

      &:hover:not(:disabled) {
        background-color: var(--clickgui-button-hover-background-color);
        border-color: var(--clickgui-button-hover-background-color);
      }
    }

    &.danger {
      background-color: var(--clickgui-selection-chip-remove-background-color);
      border-color: var(--clickgui-selection-chip-remove-color);
      color: var(--clickgui-selection-chip-remove-color);
    }

    &.mono {
      font-family: monospace;
      letter-spacing: 1px;
    }

    &:disabled {
      opacity: .4;
      cursor: default;
    }
  }
</style>
