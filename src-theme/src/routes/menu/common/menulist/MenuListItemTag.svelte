<script lang="ts">
    export let text: string;
</script>

<div class="tag">
    {text}
</div>

<style lang="scss">

  .tag {
    background-color: var(--accent-subtle-background-color);
    border: 1px solid var(--accent-border-color);
    color: var(--menu-text-color);
    font-size: 11px;
    font-weight: 500;
    letter-spacing: 0.03em;
    padding: 3px 9px;
    border-radius: var(--radius-pill);
    margin-left: 10px;
    transition: color var(--duration-fast) var(--ease-standard),
    background-color var(--duration-fast) var(--ease-standard);
    transform: translateY(-2px);
  }
</style>
