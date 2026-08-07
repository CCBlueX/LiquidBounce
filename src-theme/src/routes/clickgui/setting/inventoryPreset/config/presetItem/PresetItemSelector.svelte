<script lang="ts">
    import type {PresetItem} from "../../../../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../../../theme/theme_config";
    import ItemImage from "../../ItemImage.svelte";
    import {onMount} from "svelte";
    import {getRegistryItems, setTyping} from "../../../../../../integration/rest";
    import VirtualList from "../../../list/VirtualList.svelte";
    import {REST_BASE} from "../../../../../../integration/host";
    import PresetTooltip from "../PresetTooltip.svelte";

    export let setItem: (item: PresetItem) => void
    export let filter: ((item: string) => boolean) | null = null

    const commonItems: PresetItem[] = [
        {
            type: "SINGLE",
            item: "minecraft:shield"
        },
        {
            type: "SINGLE",
            item: "minecraft:ender_pearl"
        },
        {
            type: "SINGLE",
            item: "minecraft:arrow"
        },
        {
            type: "SINGLE",
            item: "minecraft:bow"
        },
        {
            type: "SINGLE",
            item: "minecraft:crossbow"
        },
        {
            type: "SINGLE",
            item: "minecraft:fishing_rod"
        },
        {
            type: "SINGLE",
            item: "minecraft:golden_apple"
        },
        {
            type: "SINGLE",
            item: "minecraft:enchanted_golden_apple"
        },
        {
            type: "SINGLE",
            item: "minecraft:water_bucket"
        },
        {
            type: "SINGLE",
            item: "minecraft:lava_bucket"
        },
        {
            type: "SINGLE",
            item: "minecraft:flint_and_steel"
        },
        {
            type: "SINGLE",
            item: "minecraft:shears"
        },
    ];

    interface GenericPresetItemList {
        item: PresetItem
        name: string,
        tooltip?: string | null
    }

    const genericItems: GenericPresetItemList[] = [
        {
            item: { type: "GROUP", group: "WEAPON" },
            name: "Weapon"
        },
        {
            item: { type: "GROUP", group: "SPEAR" },
            name: "Spear"
        },
        {
            item: { type: "GROUP", group: "MACE" },
            name: "Mace"
        },
        {
            item: { type: "GROUP", group: "SHIELD" },
            name: "Shield"
        },
        {
            item: { type: "GROUP", group: "ROD" },
            name: "Fishing Rod"
        },
        {
            item: { type: "GROUP", group: "FOOD" },
            name: "Food"
        },
        {
            item: { type: "GROUP", group: "BLOCK" },
            name: "Blocks"
        },
        {
            item: { type: "GROUP", group: "AXE" },
            name: "Axe"
        },
        {
            item: { type: "GROUP", group: "PICKAXE" },
            name: "Pickaxe"
        },
        {
            item: { type: "GROUP", group: "HOE" },
            name: "Hoe"
        },
        {
            item: { type: "GROUP", group: "POTION" },
            name: "Potions"
        },
        {
            item: { type: "GROUP", group: "THROWABLE" },
            name: "Throwables"
        },
        {
            item: { type: "GROUP", group: "ARROWS" },
            name: "Arrows"
        }
    ]

    interface TItem {
        name: string;
        identifier: string;
    }

    let items: TItem[] = [];
    let renderedItems: TItem[] = items;
    let searchQuery = "";

    $: {
        let filteredItems = items;

        if (searchQuery) {
            filteredItems = filteredItems.filter(b =>
                b.name.toLowerCase().includes(searchQuery.toLowerCase())
                || b.identifier.toLowerCase().includes(searchQuery.toLowerCase())
            );
        }

        filteredItems = filteredItems.filter(item =>
            item.identifier !== "minecraft:air"
            && filter ? filter(item.identifier) : true
        )

        renderedItems = filteredItems;
    }

    function setItemProxy(item: PresetItem) {
        setItem(item)

        if (filter && item.type == "SINGLE") {
            renderedItems = renderedItems.filter(it => it.identifier != item.item)
        }
    }

    onMount(async () => {
        const registryItems = await getRegistryItems("item");

        items = Object.entries(registryItems)
            .map(([identifier, item]) => ({
                name: item.name,
                identifier
            }))
            .sort((a, b) => a.identifier.localeCompare(b.identifier));
    });
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="select-selector">
    <div class="select-title">
        <span>{searchQuery === "" ? "Select Items" : "Search"}</span>
    </div>

    {#if searchQuery === ""}
        <div>
            <span class="items-group-title">Quick Select</span>
            <div class="common-wrapper">
                {#each commonItems as commonItem}
                    <div class="item-background common-item-wrapper" on:click={() => setItemProxy(commonItem)}>
                        <div class="common-item">
                            <ItemImage bind:item={commonItem} />
                        </div>
                    </div>
                {/each}
            </div>
        </div>
    {/if}
    <div class="search-wrapper">
        <span class="items-group-title">Specific Items</span>
        <div class="search margin">
            <input
                    type="text"
                    placeholder="Search items..."
                    class="search-input"
                    bind:value={searchQuery}
                    on:focusin={async () => await setTyping(true)}
                    on:focusout={async () => await setTyping(false)}
                    spellcheck="false">
            <div class="search-icon">
                <img src="img/menu/icon-pen.svg" alt="Search" />
            </div>
        </div>

    </div>

    {#if searchQuery === ""}
        <div>
            <span class="items-group-title">Item Groups <PresetTooltip text="These are common items that may be of different types but perform the same function. The sorting goes from best to worst."></PresetTooltip></span>
            <div class="generic-wrapper">
                <VirtualList items={genericItems} let:item>
                    <div class="generic-item" on:click={() => setItemProxy(item.item)}>
                        <div class="img-wrapper">
                            <div class="img">
                                <ItemImage bind:item={item.item} />
                            </div>
                        </div>
                        <span>{$spaceSeperatedNames ? convertToSpacedString(item.name) : item.name}</span>
                        {#if item.tooltip != null}
                            <PresetTooltip text={item.tooltip} />
                        {/if}
                    </div>
                </VirtualList>
            </div>
        </div>
    {:else}
        {#if renderedItems.length > 0}
            <div class="results">
                <VirtualList items={renderedItems} let:item>
                    <div class="result-item" on:click={() => setItemProxy({type: "SINGLE", item: item.identifier})}>
                        <div class="icon-wrapper">
                            <img class="icon" src="{REST_BASE}/api/v1/client/resource/itemTexture?id={item.identifier}" alt={item.identifier}/>
                        </div>

                        <span class="name">
                            {item.name}
                        </span>
                    </div>
                </VirtualList>
            </div>
        {:else}
            <span class="items-group-title">No Results</span>
        {/if}
    {/if}
</div>

<style lang="scss">
  @use "sass:color";
  @use "../../../../../../colors" as *;
  @use "../select" as *;
  @use "../item" as *;

  .margin {
    margin-top: 5px;
  }

  .common-wrapper {
    margin-top: 8px;
    display: flex;
    gap: 8px;
    justify-content: space-between;
    flex-wrap: wrap;
  }

  .common-item-wrapper {
    border-radius: 3px;
    width: 28px;
    height: 28px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
  }

  .common-item {
    width: 20px;
    height: 20px;
  }

  .generic-wrapper {
    height: 170px;
    overflow-y: auto;
    overflow-x: hidden;
  }

  .generic-item {
    display: flex;
    width: 100%;
    height: 40px;
    align-items: center;
    gap: 10px;
    cursor: pointer;

    & > .img-wrapper {
      transition: opacity 0.3s ease;
      width: 25px;
      height: 25px;
      opacity: 0.6;
    }

    & > span {
      color: $clickgui-text-dimmed-color;
      display: flex;
      font-size: 15px;
      transition: color 0.3s ease;
    }

    &:hover {
      & > .img-wrapper {
        opacity: 1;
      }

      & > span {
        color: $clickgui-text-color;
      }
    }
  }

</style>
