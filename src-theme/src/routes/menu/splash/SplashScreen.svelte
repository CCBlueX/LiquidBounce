<script lang="ts">
    import Vivus from "vivus";
    import {onMount} from "svelte";
    import {fade, fly} from "svelte/transition";

    let svgObjectElement: HTMLObjectElement;
    let animationFinished = false;

    onMount(() => {
        new Vivus(svgObjectElement, {duration: 200, type: "sync"}, () => {
            animationFinished = true;
        });
    });
</script>


{#if !animationFinished}
    <object class="logo" in:fly|global={{duration: 200, y: -50}} out:fade|global={{duration: 200}} bind:this={svgObjectElement} title=""
            type="image/svg+xml" data="img/lb-logo-stroke.svg"></object>
{:else}
    <object class="logo" out:fly|global={{duration: 200, y: -50}} in:fade|global={{duration: 200}} title=""
            type="image/svg+xml" data="img/lb-logo.svg"></object>
{/if}

<style lang="scss">
  .logo {
    position: fixed;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 50vw;
  }
</style>
