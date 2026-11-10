<script lang="ts">
    let {works, fails}: {
        works: number;
        fails: number;
    } = $props();
</script>

<span class="reports">
    {#if works + fails === 0}
        No reports yet
    {:else}
        <span class="works">{works} working</span>
        <span class="broken">{fails} broken</span>
    {/if}
</span>

<style lang="scss">
  .reports {
    display: inline-flex;
    justify-content: flex-end;
    column-gap: 8px;
    min-width: 130px;
  }

  .works {
    color: var(--clickgui-marketplace-works-color);
  }

  .broken {
    color: var(--clickgui-marketplace-broken-color);
  }
</style>
