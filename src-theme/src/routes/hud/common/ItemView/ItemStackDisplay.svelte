<script lang="ts">
    import { getEffectiveEnchantmentStatus, type ItemStack } from "../../../../integration/types";
    import { REST_BASE } from "../../../../integration/host";
    import { mapToColor } from "../../../../util/color_utils";

    export let stack: ItemStack;

    export let showDurability = false;
    export let dyedMode = false;
    export let durabilityColorMode = false;
    export let durabilityPosition: "outside" | "inside" = "inside";

    const { count, damage, identifier, maxDamage, dyedColor } = stack;
    const showEnchantment = getEffectiveEnchantmentStatus(stack);

    const isDyed = dyedMode && dyedColor !== undefined;
    const itemIconUrl = isDyed
        ? `${REST_BASE}/api/v1/client/resource/dyedItemTexture?id=${identifier}&color=${dyedColor.toString(16)}`
        : `${REST_BASE}/api/v1/client/resource/itemTexture?id=${identifier}`;

    const countColor = count <= 0 ? "red" : "white";
    const durabilityPercent = maxDamage > 0 ? (maxDamage - damage) / maxDamage : 0;
    const durabilityColor = durabilityColorMode ? mapToColor(120 * durabilityPercent) : "inherit";
</script>

<div class="item-stack" class:dyed-mode={dyedMode}>

    <img alt={identifier} class="item-icon" src={itemIconUrl} />

    {#if showEnchantment}
        <div class="enchant-glint" style="mask-image: url({itemIconUrl})"></div>
    {/if}

    {#if showDurability && maxDamage > 0}
        <div class="durability-bar" class:outside={durabilityPosition === 'outside'}>
            <div
                    class="durability"
                    style="width: {durabilityPercent * 100}%; background-color: {durabilityColor}"
            ></div>
        </div>
    {/if}

    <div
            class="count"
            class:hidden={count <= 1}
            style="color: {countColor}"
    >
        {count}
    </div>
</div>

<style lang="scss">
  @use "../../../../colors" as *;

  .item-stack {
    position: relative;
    width: 32px;
    height: 32px;

    &.dyed-mode {
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  .item-icon {
    width: 100%;
    height: 100%;
    mask-image: none;
  }

  .durability-bar {
    position: absolute;
    left: 10%;
    width: 80%;
    height: 4px;
    background-color: rgba($text, 0.68);
    border-radius: 12px;
    box-shadow: 0 0 2px $text;

    &.outside {
      bottom: -4px;
    }

    &:not(.outside) {
      bottom: 0;
      height: 2px;
      box-shadow: none;
      border-radius: 0;
    }
  }

  .durability {
    height: 100%;
    border-radius: inherit;
    transition: width 150ms;
  }

  .count {
    position: absolute;
    bottom: 0;
    right: 0;
    font-size: 12px;
    text-shadow: 1px 1px black;

    &.hidden {
      display: none;
    }
  }
</style>
