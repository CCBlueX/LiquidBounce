<script lang="ts">
    import Button from "../../common/Button.svelte";

    /**
     * How many reported that a config works and that it is broken. With [onreport] the counts are buttons to
     * report it, and [own] is the report the account made.
     */
    let {works, fails, own, onreport}: {
        works: number;
        fails: number;
        own?: boolean;
        onreport?: (works: boolean) => void;
    } = $props();
</script>

{#if onreport}
    <Button variant="secondary" label="Works" pressed={own === true} onclick={() => onreport(true)}>
        <span class="count works">{works}</span>
    </Button>
    <Button variant="secondary" label="Broken" pressed={own === false} onclick={() => onreport(false)}>
        <span class="count broken">{fails}</span>
    </Button>
{:else if works + fails > 0}
    <span class="reports" aria-label="{works} reported that it works, {fails} that it is broken">
        <span class="count works">{works}</span>
        <span class="count broken">{fails}</span>
    </span>
{/if}

<style lang="scss">
  .reports {
    display: inline-flex;
    column-gap: 10px;
  }

  .count {
    display: inline-flex;
    align-items: center;
    column-gap: 5px;

    &::before {
      content: "";
      width: 10px;
      height: 10px;
      background-color: currentColor;
      mask: center / contain no-repeat;
    }

    &.works {
      color: var(--clickgui-marketplace-works-color);

      &::before {
        mask-image: url("/img/clickgui/icon-tick-checked.svg");
      }
    }

    &.broken {
      color: var(--clickgui-marketplace-broken-color);

      &::before {
        mask-image: url("/img/clickgui/icon-cross.svg");
      }
    }
  }
</style>
