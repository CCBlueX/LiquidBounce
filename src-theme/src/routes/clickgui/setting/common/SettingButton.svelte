<script lang="ts">
    export let value: string;
</script>

<button class="setting-button" on:click>{value}</button>

<style lang="scss">
  .setting-button {
    font-family: "Inter", sans-serif;
    font-weight: 500;
    font-size: 12px;
    color: var(--clickgui-text-color);
    background-color: var(--clickgui-button-background-color);
    border: 1px solid var(--accent-border-color);
    padding: 7px 10px;
    border-radius: var(--radius-xs);
    width: 100%;
    cursor: pointer;
    transition: background-color var(--duration-fast) var(--ease-standard),
    border-color var(--duration-fast) var(--ease-standard);

    &:hover {
      background-color: var(--clickgui-button-hover-background-color);
      border-color: var(--accent-border-strong-color);
    }
  }
</style>
