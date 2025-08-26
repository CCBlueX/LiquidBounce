<script lang="ts">
    import {Tween} from "svelte/motion";
    import {onMount, tick} from "svelte";
    import {expoInOut} from "svelte/easing";
    import {fade} from "svelte/transition";
    import ConnectionDetail from "./ConnectionDetail.svelte";
    import SplashTip from "./SplashTip.svelte";

    let fps = 60;
    let totalDuration = 2333;


    function makeCustomEasing(fpsValue: number) {
        const clamped = Math.max(20, Math.min(fpsValue, 60));
        const factor = 60 / clamped;

        return (t: number) => {
            if (t <= 0.15) {
                const u = t / 0.15;
                return Math.pow(u, factor) * 0.15;
            } else if (t >= 0.75) {
                const u = (t - 0.75) / 0.25;
                return 0.75 + (1 - Math.pow(1 - u, factor)) * 0.25;
            } else {
                return 0.15 + ((t - 0.15) / 0.60) * 0.60;
            }
        };
    }

    const progress = new Tween(0, {
        duration: totalDuration,
        easing: makeCustomEasing(fps),
    });


    async function restartProgress() {

        await progress.set(0, {duration: 0});

        await tick();

        await progress.set(100, {
            duration: totalDuration,
            easing: makeCustomEasing(fps),
        });
    }

    onMount(() => {
        restartProgress();
    });
</script>

<div class="wrapper" out:fade={{ duration: 400 }}>
    <div class="bg-pattern"
         transition:fade={{ duration:1200, easing: expoInOut }}>
    </div>
    <ConnectionDetail/>
    <SplashTip/>
    <div class="line-decoration" transition:fade={{ duration: 600, easing: expoInOut }}></div>
    <div class="loader-wrapper"
         transition:fade={{ duration: 600, easing: expoInOut }}>
        <div aria-hidden="true" class="loading-bar" role="presentation">
            <div class="loading-bar-bg"></div>
            <div
                    class="loading-bar-progress"
                    style="width: {progress.current}%"
            ></div>
        </div>
    </div>
</div>

<style lang="scss">
  .wrapper {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background-color: #1c1b22;
    display: flex;
    justify-content: center;
    align-items: flex-start;
    z-index: 0;
  }

  .line-decoration {
    position: absolute;
    top: calc(87vh + 31.25px);
    height: 2px;
    width: 100vw;
    z-index: 1;
    pointer-events: none;

    &::before,
    &::after {
      content: "";
      position: absolute;
      top: 0;
      height: 2px;
      background: rgba(204, 204, 204, 0.5);
      opacity: 0.6;
      width: calc((100vw - 500px * 0.6) / 2 - 20px);
    }

    &::before {
      left: 0;
    }

    &::after {
      right: 0;
    }
  }

  .loader-wrapper {
    position: relative;
    display: flex;
    margin-top: 87vh;
    flex-direction: column;
    align-items: center;
    gap: 2rem;
    z-index: 2;
    transform: scale(0.6);
  }

  .bg-pattern {
    position: absolute;
    width: 100%;
    height: 100%;
    background: #1C1B22;
  }

  .loading-bar {
    position: relative;
    width: 500px;
    height: 62.5px;
    user-select: none;
    overflow: hidden;

    &-bg {
      position: absolute;
      width: 100%;
      height: 100%;
      background-color: #434343;
      mask: url("/img/menu/connection/loading-bar.webp") no-repeat left 100%;
      mask-size: 500px 62.5px;
    }

    &-progress {
      position: absolute;
      top: 0;
      left: 0;
      height: 100%;
      background-color: #ece5d8;
      mask: url("/img/menu/connection/loading-bar.webp") no-repeat left 100%;
      filter: drop-shadow(0 0 12px rgba(255, 255, 255, 0.8));
      mask-size: 500px 62.5px;
      width: 0;
      transition: width linear;
      will-change: width;
    }
  }
</style>


