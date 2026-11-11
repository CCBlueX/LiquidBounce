<script lang="ts">
    export let value: string;
</script>

<button type="button" class="setting-button" on:click>{value}</button>

<style lang="scss">
  .setting-button {
    width: 100%;
    height: 28px;
    padding: 0 10px;
    border: none;
    border-radius: 3px;
    background-color: var(--clickgui-button-secondary-background-color);
    color: var(--clickgui-text-color);
    font-family: "Inter", sans-serif;
    font-weight: 500;
    font-size: 12px;
    cursor: pointer;
    transition: ease background-color .15s;

    &:hover, &:focus-visible {
      background-color: var(--clickgui-button-secondary-hover-background-color);
    }
  }
</style>
