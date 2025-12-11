<script lang="ts">
    import {listen} from "../../../integration/ws";
    import {fly} from "svelte/transition";
    import {mapToColor} from "../../../util/color_utils";
    import {itemTextureUrl} from "../../../integration/rest";

    let nextBlock = $state<string | undefined>(undefined);
    let count = $state<number | undefined>(undefined);

    listen("blockCountChange", (data) => {
        nextBlock = data.nextBlock;
        count = data.count;
    });
</script>

{#if count !== undefined}
    <div class="counter" style="color: {mapToColor(count)}" in:fly={{ y: -5, duration: 200 }}
         out:fly={{ y: -5, duration: 200 }}>
        {#if nextBlock}
            <img class="icon" src={itemTextureUrl(nextBlock)} alt={nextBlock}/>
        {/if}
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
    text-align: center;
    width: fit-content;
    display: inline-flex;
    align-items: center;
    gap: 5px;
    transform: translate(-100%);
  }

  .icon {
    width: 24px;
    height: 24px;
  }
</style>
