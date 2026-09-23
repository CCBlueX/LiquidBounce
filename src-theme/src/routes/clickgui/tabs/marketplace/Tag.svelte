<script lang="ts">
    let {text}: { text: string } = $props();
</script>

<span class="tag">{text}</span>

<style lang="scss">
  .tag {
    font-size: 11px;
    font-weight: 500;
    color: var(--clickgui-text-dimmed-color);
    background-color: var(--clickgui-global-settings-divider-color);
    padding: 2px 8px;
    border-radius: 999px;
    white-space: nowrap;
  }
</style>
