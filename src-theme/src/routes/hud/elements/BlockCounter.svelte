<script lang="ts">
    import {listen} from "../../../integration/ws";
    import {fly} from "svelte/transition";
    import type {BlockCountChangeEvent} from "../../../integration/events";

    let count: number | undefined;

    function mapToColor(value: number): string {
        if (value <= 0) {
            return 'rgb(255, 0, 0)';
        } else if (value <= 60) {
            return `rgb(255, ${Math.floor(value * 255 / 60)}, 0)`;
        } else if (value <= 120) {
            return `rgb(${Math.floor((120 - value) * 255 / 60)}, 255, 0)`;
        } else {
            return 'rgb(0, 255, 0)';
        }
    }

    listen("blockCountChange", (data: BlockCountChangeEvent) => {
        count = data.count;
    });

</script>

{#if count !== undefined}
    <div class="counter" style="color: {mapToColor(count)}" in:fly={{ y: -5, duration: 200 }}
         out:fly={{ y: -5, duration: 200 }}>
        {count}
    </div>
{/if}

<style lang="scss">
  @import "../../../colors";

  .counter {
    background-color: rgba($blockcounter-base-color, 0.68);
    border-radius: 5px;
    white-space: nowrap;
    padding: 5px 8px;
    font-weight: 500;
    transform: translate(-100%);
  }
</style>
