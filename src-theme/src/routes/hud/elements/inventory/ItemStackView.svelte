<script lang="ts">
    import type {ItemStack} from "../../../../integration/types";
    import {REST_BASE} from "../../../../integration/host";

    export let stack: ItemStack;

    const {count, damage, identifier, maxDamage} = stack;

    const countColor = count <= 0 ? 'red' : 'white';

    const valueColor = (() => {
        const value = 120 * (maxDamage - damage) / maxDamage;
        if (value <= 0) {
            return 'rgb(255, 0, 0)';
        } else if (value <= 60) {
            return `rgb(255, ${Math.floor(value * 255 / 60)}, 0)`;
        } else if (value <= 120) {
            return `rgb(${Math.floor((120 - value) * 255 / 60)}, 255, 0)`;
        } else {
            return 'rgb(0, 255, 0)';
        }
    })();
</script>

<figure class="item-stack">
    <img class="icon" src="{REST_BASE}/api/v1/client/resource/itemTexture?id={identifier}" alt={identifier}/>

    <div class="durability-bar" class:hidden={damage === 0}>
        <div class="durability"
             style="width: {100 * (maxDamage - damage) / maxDamage}%; background-color: {valueColor}">
        </div>
    </div>

    {#if count > 1 || count < 0}
        <div class="count" style="color: {countColor}">
            {count}
        </div>
    {/if}
</figure>

<style lang="scss">
  @import "../../../../colors";

  .hidden {
    display: none;
  }

  .item-stack {
    position: relative;
    width: 32px;
    height: 32px;
  }

  .icon {
    width: 100%;
    height: 100%;
  }

  .durability-bar {
    position: absolute;
    bottom: 0;
    left: 10%;
    width: 80%;
    height: 2px;
    background-color: rgba($item-damage-base-color, 0.68);
  }

  .durability {
    height: 100%;
    transition: width 150ms;
  }

  .count {
    position: absolute;
    bottom: 0;
    right: 0;
    font-size: 14px;
    font-weight: bold;
    text-shadow: 1px 1px black;
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  }
</style>
