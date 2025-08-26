<script lang="ts">
    import {listen} from "../../../integration/ws";
    import {fly} from "svelte/transition";
    import type {BlockCountChangeEvent} from "../../../integration/events";
    import {mapToColor} from "../../../util/color_utils";

    let count: number | undefined;

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
  @use "../../../colors.scss" as *;

  .counter {
    background-color: rgba($blockcounter-base-color, 0.68);
    border-radius: 5px;
    white-space: nowrap;
    padding: 5px 8px;
    font-weight: 500;
    transform: translate(-100%);
  }
</style>
