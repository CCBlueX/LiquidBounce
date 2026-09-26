<script>
    import LiquidBounceLogo from "../../components/LiquidBounceLogo.svelte";
    import {fade} from 'svelte/transition';
</script>

<div class="watermark" in:fade={{ duration: 100 }} out:fade={{ duration: 100 }}>
    <LiquidBounceLogo
        width="165px"
        height="61.8917px"
        badgeFill="var(--accent-color)"
    />
</div>

<style>
    .watermark {
        position: absolute;
        bottom: 15px;
        right: 15px;
        opacity: 0.6;
    }
</style>
