<script lang="ts">
    export let gradient = false;
</script>

<div class="line">
    <svg height="2" preserveAspectRatio="none" viewBox="0 0 100 2" width="100%">
        {#if gradient}
            <defs>
                <linearGradient id="flow-gradient" x1="0%" x2="100%" y1="0%" y2="0%">
                    <stop offset="0%" stop-color="var(--primary-color)" stop-opacity="0.5"/>
                    <stop offset="50%" stop-color="var(--secondary-color)" stop-opacity="0.5"/>
                    <stop offset="100%" stop-color="var(--primary-color)" stop-opacity="0.5"/>
                    <animateTransform
                            attributeName="gradientTransform"
                            dur="4s"
                            from="1 0"
                            repeatCount="indefinite"
                            to="-1 0"
                            type="translate"
                    />
                </linearGradient>
            </defs>
            <rect fill="url(#flow-gradient)" height="2" width="100%" x="0" y="0"/>
        {:else}
            <rect fill="white" height="2" width="100%" x="0" y="0"/>
        {/if}
    </svg>
</div>

<style lang="scss">
  .line {
    margin: 0.5em 0;

    svg {
      display: block;
    }
  }
</style>
