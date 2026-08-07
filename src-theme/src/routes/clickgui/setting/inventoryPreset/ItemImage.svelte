<script lang="ts">
    import type {GroupItemPreference, PresetItem} from "../../../../integration/types";
    import {REST_BASE} from "../../../../integration/host";

    function getIconNameFor(item: GroupItemPreference): string {
        switch (item.group) {
            case "ARROWS":
                return "arrow.png";
            case "SWORD":
            case "WEAPON":
                return "sword.png";
            case "AXE":
                return "axe.png";
            case "HOE":
                return "hoe.png";
            case "SHOVEL":
                return "shovel.png";
            case "PICKAXE":
                return "pickaxe.png";
            case "FOOD":
                return "food.png";
            case "POTION":
                return "potion.png";
            case "BLOCK":
                return "blocks.png";
            case "THROWABLE":
                return "egg.png";
            default:
                throw new Error("Illegal value.");
        }
    }

    export let item: PresetItem;
</script>

{#if item.type === "SINGLE"}
    <img src="{REST_BASE}/api/v1/client/resource/itemTexture?id={item.item}" alt={item.item}/>
{:else if item.type === "GROUP"}
    <img src="img/clickgui/icon-{getIconNameFor(item)}" alt="{item.group}"/>
{:else if item.type === "IGNORE"}
    <img src="img/clickgui/icon-ignore.svg" alt="Tools"/>
{:else if item.type === "ANY"}
    <img src="img/clickgui/icon-any.svg" alt="Any"/>
{:else}
    <img class="muted" src="img/clickgui/icon-value-none.svg" alt="Any"/>
{/if}

<style lang="scss">
  .muted {
    fill: red !important;
  }

  img {
    width: 100%;
    height: 100%;
  }
</style>
