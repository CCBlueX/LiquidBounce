<script lang="ts">
  const { size = 80 } = $props();
</script>

<div class="lds-ripple" style="--size: {size}px; --position: calc(var(--size) * 9 / 20); --small-size: calc(var(--size) / 10);"><div></div><div></div></div>

<style lang="scss">
  /*
  Taken from https://loading.io/css/
 */

  @use "../../../colors.scss" as *;
 
  .lds-ripple {
    color: $accent-color;
  }

  .lds-ripple,
  .lds-ripple div {
    box-sizing: border-box;
  }

  .lds-ripple {
    display: inline-block;
    position: relative;
    width: var(--size);
    height: var(--size);
  }

  .lds-ripple div {
    position: absolute;
    border: 4px solid currentColor;
    opacity: 1;
    border-radius: 50%;
    animation: lds-ripple 1s cubic-bezier(0, 0.2, 0.8, 1) infinite;
  }

  .lds-ripple div:nth-child(2) {
    animation-delay: -0.5s;
  }

  @keyframes lds-ripple {
    0% {
      top: var(--position);
      left: var(--position);
      width: var(--small-size);
      height: var(--small-size);
      opacity: 0;
    }
    4.9% {
      top: var(--position);
      left: var(--position);
      width: var(--small-size);
      height: var(--small-size);
      opacity: 0;
    }
    5% {
      top: var(--position);
      left: var(--position);
      width: var(--small-size);
      height: var(--small-size);
      opacity: 1;
    }
    100% {
      top: 0;
      left: 0;
      width: var(--size);
      height: var(--size);
      opacity: 0;
    }
  }
</style>
