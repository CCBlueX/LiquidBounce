<script lang="ts">
    import Header from "../header/Header.svelte";
    import {fly} from "svelte/transition";
    import {onMount} from "svelte";

    const transitionDuration = 800; // TODO: suboptimal

    let ready = false;

    onMount(() => {
        setTimeout(() => {
            ready = true;
        }, transitionDuration);
    });
</script>

<div class="menu">
    {#if ready}
        <div transition:fly|global={{duration: 700, y: -100}}>
            <Header/>
        </div>

        <div class="menu-wrapper">
            <slot/>
        </div>
    {/if}
</div>

<style lang="scss">
  .menu {
    width: 100vw;
    height: 100vh;
    padding: 50px;
    display: flex;
    flex-direction: column;
  }

  .menu-wrapper {
    flex: 1;
    display: flex;
    flex-direction: column;
    will-change: transform;
  }
</style>