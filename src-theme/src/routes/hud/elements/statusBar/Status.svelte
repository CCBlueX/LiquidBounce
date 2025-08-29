<script lang="ts">
    import {Tween} from 'svelte/motion';
    import {cubicOut} from 'svelte/easing';
    import {readable} from 'svelte/store';
    import {derived} from 'svelte/store';

    export let max: number = 100;
    export let value: number = 0;
    export let color: string = 'white';
    export let disableAutoColor: boolean = false;
    export let height: string = '20px';
    export let borderRadius: string = '5px';
    export let label: string | null = null;
    export let icon: string | null = null;
    export let animate: boolean = true;
    export let onDone: () => void = () => {};

    $: ratio = Math.max(0, Math.min(value, max)) / max;
    $: tweenRatio = tweenStore(tween, ratio);

    const tween = new Tween(0, {
        duration: animate ? 450 : 0,
        easing: cubicOut
    });

    function tweenStore<T>(tween: Tween<T>, target: T) {
        return readable(tween.current, (set) => {
            tween.set(target).then(() => onDone());
            const interval = setInterval(() => set(tween.current), 16);
            return () => clearInterval(interval);
        });
    }


    const widthPct = derived(tweenRatio, $tr => $tr * 100);

    $: pct = ratio * 100;
    $: barColor = disableAutoColor
        ? color
        : pct < 20
            ? 'red'
            : pct < 40
                ? 'orange'
                : color;
</script>

<div class="status-bar-container" style="--h: {height}; --radius: {borderRadius};">
    <div class="status-bar">
        {#if icon}
            <img class="icon" src="img/hud/hotbar/icon-{icon}.svg" alt={icon}/>
        {/if}

        <div class="bar-bg">
            <div
                    class="bar-fg"
                    style="width: {$widthPct}%; --barColor: {barColor};"
            ></div>
        </div>

        {#if label}
            <div class="label">{label}</div>
        {/if}
    </div>
</div>

<style lang="scss">
  @use "../../../../colors" as *;

  .status-bar-container {
    position: relative;
    width: 100%;
    height: var(--h);
  }

  .status-bar {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    position: relative;

    .icon {
      margin: 0 4px;
      filter: drop-shadow(0 0 4px rgba($base, 0.5));
      height: calc(var(--h) * 0.8);
      z-index: 2;
    }

    .bar-bg {
      position: relative;
      flex: 1;
      height: 100%;
      background: #333;
      border-radius: var(--radius);
      overflow: hidden;
      box-shadow: inset 0 0 5px rgba(0, 0, 0, 0.3);

      .bar-fg {
        height: 100%;
        transition: width 0.45s ease;
        will-change: width;
        background: linear-gradient(
                        135deg,
                        var(--barColor) 10%,
                        color-mix(in srgb, var(--barColor), transparent 50%) 90%
        );
      }
    }

    .label {
      position: absolute;
      width: 100%;
      text-align: center;
      font-size: calc(var(--h) * 0.6);
      color: #fff;
      text-shadow: 0 0 2px rgba(0, 0, 0, 0.7);
      pointer-events: none;
      user-select: none;
      z-index: 2;
    }
  }
</style>
