<script lang="ts">
    import type {PresetItemGroup} from "../../../../integration/types";
    import ItemImage from "./ItemImage.svelte";

    export let group: PresetItemGroup
    let currentIndex = 1

    $: rendered = group.filter(item => item.type != "ANY")

    setInterval(() => {
        currentIndex++
    }, 1000)
</script>

<div class="preset-item">
    {#if rendered.length > 0}
        <div class="img-wrapper">
            <ItemImage bind:item={rendered[currentIndex % rendered.length]}/>
        </div>
    {/if}
</div>

<style lang="scss">
  @use "sass:color";
  @use "../../../../colors.scss" as *;

  .preset-item {
    width: 20px;
    height: 20px;
    outline: 1px solid color.adjust($clickgui-text-color, $lightness: -90%);
    border-radius: 3px;
    display: flex;
    justify-content: center;
    align-content: center;
    align-items: center;
  }

  .img-wrapper {
    width: 16px;
    height: 16px;
  }
</style>
