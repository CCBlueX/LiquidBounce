<script lang="ts">
    import {fade} from "svelte/transition";
    import ToolTip from "../../common/ToolTip.svelte";
    import {createEventDispatcher} from "svelte";

    export let title: string;
    export let icon: string;
    export let parentHovered: boolean;

    const dispatch = createEventDispatcher();
</script>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="child-button" on:click|stopPropagation={() => dispatch("click")}>
    <ToolTip text={title} />

    <div class="icon" class:parent-hovered={parentHovered}>
        {#if parentHovered}
            <img transition:fade="{{ duration: 200 }}" src="img/menu/icon-{icon}-hover.svg" alt={title}>
        {:else}
            <img transition:fade="{{ duration: 200 }}" src="img/menu/icon-{icon}.svg" alt={title}>
        {/if}
    </div>
</div>

<style lang="scss">
    @import "../../../../colors.scss";

    .child-button {
      position: relative;
    }

    .icon {
      border-radius: 5px;
      background-color: $accent-color;
      width: 66px;
      height: 66px;
      transition: ease background-color .2s;
      position: relative;

      > img {
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
      }

      &.parent-hovered {
        background-color: $menu-text-color;
      }
    }
</style>