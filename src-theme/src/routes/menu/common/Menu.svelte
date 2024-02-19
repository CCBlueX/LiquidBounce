<script lang="ts">
    import {onMount} from "svelte";
    import Header from "../header/Header.svelte";

    const transitionDuration = 800; // TODO: suboptimal

    let ready = false;

    onMount(() => {
        setTimeout(() => {
            ready = true;
        }, transitionDuration)
    })
</script>

<div class="menu">
    <Header/>

    {#if ready}
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