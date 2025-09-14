<script lang="ts">
    import ToolTip from "../ToolTip.svelte";
    import {createEventDispatcher} from "svelte";

    export let title: string;
    export let icon: string;

    const dispatch = createEventDispatcher();
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="icon-button" on:click={() => dispatch("click")}>

    <ToolTip text={title}/>

    <div class="icon">
        <img alt={icon} draggable="false"
             src="img/menu/icon-{icon}.svg">
    </div>
</div>

<style lang="scss">
  .icon-button {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-direction: column;
  }

  .icon {
    height: 58px;
    width: 58px;
    display: flex;
    align-items: center;
    justify-content: center;
    transform-origin: center center;
    cursor: pointer;
    transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1), filter 0.4s ease;

    img {
      transition: transform, opacity 0.2s ease, filter 0.4s ease;
      transform-origin: center center;
      opacity: 0.75;
    }


    &:hover {
      cursor: pointer;
      transform: translateY(-4px);

      img {
        filter: drop-shadow(0 0 12px rgba(255, 255, 255, 0.8));
        opacity: 1;
      }
    }
  }
</style>
