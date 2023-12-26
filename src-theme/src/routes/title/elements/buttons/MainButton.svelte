<script>
    import {fade} from "svelte/transition";
    import {createEventDispatcher} from "svelte";

    const dispatch = createEventDispatcher();

    export let text;
    export let icon;

    let hovered = false;

    function handleMouseEnter(e) {
        hovered = true;
    }

    function handleMouseLeave(e) {
        hovered = false;
    }

    function handleClick(e) {
        dispatch("click", e);
    }
</script>

<div class="button" on:mouseenter={handleMouseEnter} on:mouseleave={handleMouseLeave} on:click={handleClick}>
    <div class="icon">
        {#if hovered}
            <img transition:fade="{{ duration: 200 }}" src="img/icons/{icon}-hover.svg" alt="icon">
        {:else}
            <img transition:fade="{{ duration: 200 }}" src="img/icons/{icon}.svg" alt="icon">
        {/if}
    </div>
    <div class="text">{text}</div>
    <slot {hovered}/>
</div>

<style lang="scss">
  .button {
    width: 590px;
    display: grid;
    grid-template-columns: max-content auto max-content;
    padding: 25px 35px;
    border-radius: 6px;
    align-items: center;
    column-gap: 30px;
    background: linear-gradient(to left, rgba(0, 0, 0, .68) 50%, #4677ff 50%);
    background-size: 200% 100%;
    background-position: right bottom;
    will-change: background-position;
    transition: background-position .2s ease-out;

    &:hover {
      background-position: left bottom;
    }
  }

  .icon {
    background-color: #4677ff;
    width: 90px;
    height: 90px;
    border-radius: 50%;
    position: relative;
    transition: ease background-color .2s;

    > img {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
    }
  }

  .button:hover .icon {
    background-color: white;
  }

  .text {
    font-size: 26px;
    color: white;
    font-weight: bold;
  }
</style>
