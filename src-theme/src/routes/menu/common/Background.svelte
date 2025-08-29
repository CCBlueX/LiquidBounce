<script lang="ts">
    import {shouldZoom} from "../login/locked_store";
    import {currentLogo} from './header/logoStorage';
    import {fade} from "svelte/transition";

    export let showBackground: boolean;
</script>

{#if showBackground && $currentLogo === 1}
    <div class="background zoom-in-{$shouldZoom}">
        <div class="vignette"></div>
    </div>
{:else}
    <div class="vignette "
         transition:fade|global={{duration: 300}}></div>
{/if}


<style lang="scss">
  .background {
    position: absolute;
    inset: 0;
    background: url('/background.png') center / 100% 100%;
    filter: blur(4px);
    transform: scale(1);
    transition: transform 0.5s ease, filter 0.5s ease;
    z-index: -2;

    &.zoom-in-true {
      filter: blur(8px);
      transform: scale(1.2);
    }
  }

  .vignette {
    position: absolute;
    inset: 0;
    background: url('/img/Vignette.png') center / 100% 100%;
    z-index: -1;
    pointer-events: none;
  }
</style>
