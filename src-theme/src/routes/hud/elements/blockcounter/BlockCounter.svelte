<script lang="ts">
    import {listen} from "../../../../integration/ws";
    import {fade} from "svelte/transition";
    import type {BlockCountChangeEvent} from "../../../../integration/events";

    let count: number | null = null;

    let color = 'transparent';

    listen("blockCountChange", (data: BlockCountChangeEvent) => {
        count = data.count;

        if (count == null) color = color;
        else if (count <= 0) color = 'rgb(255, 0, 0)';
        else if (count <= 60) color = `rgb(255, ${~~(count * 255 / 60)}, 0)`;
        else if (count <= 120) color = `rgb(${~~(120 - count) * 255 / 60}, 255, 0)`;
        else color = 'rgb(0, 255, 0)';
    });

</script>

{#if count != null}
    <div class="counter" style="color: {color}" transition:fade={{duration: 300}}>
        {count}
    </div>
{/if}

<style lang="scss">
  @import "../../../../colors.scss";

  .counter {
    background-color: rgba($targethud-base-color, 0.38);
    border-radius: 5px;
    display: inline-block;
    white-space: nowrap;
    padding: 0.25rem 0.5rem;
    transform: translateX(-50%);
  }
</style>
