<script lang="ts">
    import {toggleBackgroundShaderEnabled} from "../../../../integration/rest";
    import {onDestroy,} from "svelte";
    import {currentLogo, logoVariants} from "./logoStorage";
    import {lock} from "../../login/locked_store";
    import ClientLogo from "../../../hud/common/ClientLogo.svelte";

    export let showLogo: boolean;
    let glitchActive = false;
    let intervalId: ReturnType<typeof setInterval>;
    let timeoutId: ReturnType<typeof setTimeout>;
    let redLayer: HTMLElement;
    let blueLayer: HTMLElement;
    let pressTimer: NodeJS.Timeout;

    const LONG_PRESS_DURATION = 1000;

    function handleLogoMouseDown() {
        pressTimer = setTimeout(() => {
            lock();
        }, LONG_PRESS_DURATION);
    }

    function handleLogoMouseUp() {
        clearTimeout(pressTimer);
    }

    function startGlitch() {
        clearInterval(intervalId);
        clearTimeout(timeoutId);
        glitchActive = true;
        const layers = [redLayer, blueLayer].filter(Boolean) as HTMLImageElement[];

        intervalId = setInterval(() => {
            layers.forEach((layer) => {
                const tx = Math.random() * 20 - 10;
                const ty = Math.random() * 20 - 10;
                layer.style.transform = `translate(${tx}px, ${ty}px)`;

                const x = Math.random() * 100;
                const y = Math.random() * 100;
                const w = Math.random() * 20 + 20;
                const h = Math.random() * 20 + 20;
                layer.style.clipPath = `polygon(${x}% ${y}%, ${x + w}% ${y}%, ${x + w}% ${y + h}%, ${x}% ${y + h}%)`;
            });
        }, 30);

        timeoutId = setTimeout(() => {
            clearInterval(intervalId);
            layers.forEach((layer) => {
                layer.style.transform = "";
                layer.style.clipPath = "";
            });
            glitchActive = false;
        }, 1000);
    }

    function switchLogo() {
        startGlitch();
        toggleBackgroundShaderEnabled();
        currentLogo.update(n => (n % logoVariants) + 1);
    }

    function handleClick() {
        toggleBackgroundShaderEnabled();
        currentLogo.update(n => (n % logoVariants) + 1);
    }

    onDestroy(() => {
        clearInterval(intervalId);
        clearTimeout(timeoutId);
    });
</script>


{#if showLogo}
    <button
            class="logo-container reset-button"
            on:contextmenu|preventDefault={handleClick}
            on:click|preventDefault={switchLogo}
            on:mousedown={handleLogoMouseDown}
            on:mouseup={handleLogoMouseUp}
            on:mouseleave={handleLogoMouseUp}
            on:touchstart={handleLogoMouseDown}
            on:touchend={handleLogoMouseUp}
    >
        <div class="logo {glitchActive ? 'transparent' : ''}">
            <ClientLogo/>
        </div>
        <div bind:this={redLayer}
             class="logo glitch-layer red {glitchActive ? 'visible' : ''}"
        >
            <ClientLogo/>
        </div>

        <div bind:this={blueLayer}
             class="logo glitch-layer blue {glitchActive ? 'visible' : ''}"
        >
            <ClientLogo/>
        </div>
    </button>
{/if}

<style lang="scss">
  @import "../../../../colors";


  .logo-container {
    position: relative;
    cursor: pointer;
  }

  .logo {
    height: 100%;
    width: 100%;
    user-select: none;
    display: block;
    transition: opacity 0.2s ease;

  }

  .logo.transparent {
    opacity: 0;

  }

  .glitch-layer {
    position: absolute;
    top: 0;
    left: 0;
    height: 125px;
    pointer-events: none;
    mix-blend-mode: lighten;
    opacity: 0;
    transition: opacity 0.1s ease;
  }

  .glitch-layer.visible {
    opacity: 1;
  }

  .glitch-layer.red {
    filter: drop-shadow(-2px 0 0 var(--primary-color));
  }

  .glitch-layer.blue {
    filter: drop-shadow(2px 0 0 var(--secondary-color));
  }

</style>
