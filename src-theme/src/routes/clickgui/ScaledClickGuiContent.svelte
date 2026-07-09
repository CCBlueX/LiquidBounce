<script lang="ts">
    import type {Snippet} from "svelte";
    import {scaleFactor} from "./clickgui_store";

    let {children}: {
        children: Snippet;
    } = $props();
</script>

<div
        class="scaled-clickgui-content"
        style="
            transform: scale({$scaleFactor * 50}%);
            width: {2 / $scaleFactor * 100}vw;
            height: {2 / $scaleFactor * 100}vh;
        "
>
    {@render children()}
</div>

<style lang="scss">
  .scaled-clickgui-content {
    position: absolute;
    left: 0;
    top: 0;
    overflow: hidden;
    transform-origin: top left;
  }
</style>
