<script lang="ts">
    import Header from "./header/Header.svelte";
    import {fly, fade} from "svelte/transition";
    import {onMount} from "svelte";
    import {location} from "svelte-spa-router";
    import Background from "./Background.svelte";


    const transitionDuration = 700;
    let ready = false;
    let showShadow = false;
    const noAccountPaths = ["/altmanager", "/disconnected"];
    const noHeaderPaths = ["/lockscreen", "/disconnected", "connecting"];
    const BackgroundPaths = ["lockscreen",];


    onMount(() => {
        setTimeout(() => {
            ready = true;
            showShadow = true;
        }, transitionDuration);
    });
    $: showAccount = !noAccountPaths.includes($location);
    $: showHeader = !noHeaderPaths.includes($location);
    $: showBackground = BackgroundPaths.includes($location);


</script>

<div class="menu-container">

    {#if showShadow}
        <div class="edge-shadows" transition:fade|global={{ duration: 500 }}>
            <!-- svelte-ignore element_invalid_self_closing_tag -->
            <div class="shadow top"/>
            <!-- svelte-ignore element_invalid_self_closing_tag -->
            <div class="shadow right"/>
            <!-- svelte-ignore element_invalid_self_closing_tag -->
            <!-- svelte-ignore element_invalid_self_closing_tag -->
            <div class="shadow bottom"/>
            <!-- svelte-ignore element_invalid_self_closing_tag -->
            <div class="shadow left"/>
        </div>
    {/if}

    <div class="menu">
        {#if ready}
            <div transition:fly|global={{ duration: 300, y: -100 }}>

                <Header showAccount={showAccount}
                        showHeader={ showHeader }
                />
            </div>

            <div class="menu-wrapper">
                <slot/>
            </div>
        {/if}
    </div>
</div>
<Background showBackground={showBackground}/>
<style lang="scss">

  .menu-container {
    position: relative;
    height: 100%;
  }

  .edge-shadows {
    position: fixed;
    inset: 0;
    pointer-events: none;
    z-index: 10;

    .shadow {
      position: absolute;
      background: linear-gradient(
                      to var(--direction),
                      rgba(0, 0, 0, 0.15) 0%,
                      transparent 100%
      );
      opacity: 0;


      &.top {
        --direction: bottom;
        top: 0;
        left: 0;
        right: 0;
        height: 20px;

      }

      &.right {
        --direction: left;
        top: 0;
        right: 0;
        bottom: 0;
        width: 20px;

      }

      &.bottom {
        --direction: top;
        left: 0;
        right: 0;
        bottom: 0;
        height: 20px;

      }

      &.left {
        --direction: right;
        top: 0;
        left: 0;
        bottom: 0;
        width: 20px;

      }
    }
  }

  .menu {
    padding: 50px;
    display: flex;
    flex-direction: column;
    height: 100vh;
    position: relative;
  }

  .menu-wrapper {
    flex: 1;
    display: flex;
    flex-direction: column;
    will-change: transform;
  }

  @media screen and (max-width: 1600px) {
    .menu {
      zoom: 0.9;
      height: 100vh;
    }
  }

  @media screen and (max-width: 1366px) {
    .menu {
      zoom: 0.8;
      height: 125vh;
    }
  }

  @media screen and (max-width: 1200px) {
    .menu {
      zoom: 0.5;
      height: 200vh;
    }
  }

  @media screen and (max-height: 1100px) {
    .menu {
      zoom: 0.8;
      height: 125vh;
    }
  }

  @media screen and (max-height: 700px) {
    .menu {
      zoom: 0.5;
      height: 200vh;
    }
  }

  @media screen and (max-height: 540px) {
    .menu {
      zoom: 0.4;
      height: 250vh;
    }
  }
</style>
