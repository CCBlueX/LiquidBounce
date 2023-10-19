<script>
    import {fade} from "svelte/transition";
    import {createEventDispatcher} from "svelte";
    import ToolTip from "../ToolTip.svelte";

    const dispatch = createEventDispatcher();

    export let icon;
    export let text;
    export let hovered;

    function handleClick(e) {
        dispatch("click", e);
    }
</script>

<div class="button" on:click|stopPropagation={handleClick}>
    <ToolTip {text}/>

    <div class="icon" class:hovered>
        {#if hovered}
            <img transition:fade="{{ duration: 200 }}" src="img/icons/{icon}-hover.svg" alt="icon">
        {:else}
            <img transition:fade="{{ duration: 200 }}" src="img/icons/{icon}.svg" alt="icon">
        {/if}
    </div>
</div>

<style lang="scss">
  .button {
    position: relative;
  }

  .icon {
    border-radius: 6px;
    background-color: #4677FF;
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

    &.hovered {
      background-color: white;
    }
  }
</style>
