<script lang="ts">
    import {fade} from 'svelte/transition';
    import {clientName} from "../../../../util/Theme/ThemeManager";
    import type {ClientInfo} from "../../../../integration/types";

    export let clientInfo: ClientInfo;
    export let gradient = false
</script>
<svg aria-hidden="true" height="0" width="0">
    <filter height="500%" id="glow" primitiveUnits="objectBoundingBox" width="200%" x="-50%" y="-200%">
        <feGaussianBlur in="SourceGraphic" result="blurred" stdDeviation=".025 .2"/>
        <feColorMatrix in="blurred" result="saturated" type="saturate" values="1.3"/>
        <feBlend in="SourceGraphic" in2="saturated" mode="normal"/>
    </filter>
</svg>
<div class="client" class:client-glow={gradient} in:fade>
    {$clientName ? $clientName : `禁漫修復 ${clientInfo?.clientVersion ?? ''}`}
</div>

<style lang="scss">
  @use "../../../../colors" as *;

  @property --k {
    syntax: '<number>';
    initial-value: 0;
    inherits: false;
  }

  @keyframes k {
    to {
      --k: 1;
    }
  }

  .client-glow {
    animation: k 4s linear infinite;
    filter: url(#glow);
    background-image: linear-gradient(
                    90deg,
                    hsl(calc(var(--k) * 1turn), 95%, 65%),
                    hsl(calc(var(--k) * 1turn + 90deg), 95%, 65%)
    );
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    color: transparent;
    font-weight: 900;
  }

  .client {
    font-size: 22px;
    font-family: 'Alibaba', sans-serif;
    font-feature-settings: "tnum";
    font-variant-numeric: tabular-nums;
    font-weight: 700;
    text-transform: uppercase;
    color: white;
  }
</style>
