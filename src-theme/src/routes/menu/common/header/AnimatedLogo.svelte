<script lang="ts">
    import LiquidBounceLogo from "../../../../components/LiquidBounceLogo.svelte";

    const idBase = `lb-logo-${Math.random().toString(36).slice(2, 9)}`;
    const bannerGradientId = `${idBase}-banner`;
    const bannerTextGradientId = `${idBase}-banner-text`;

    const anniversary = (new Date()).getFullYear() - 2016;
</script>

<div class="animated-logo">
    <LiquidBounceLogo
        width="261.263px"
        height="98px"
        badgeFill={`url(#${bannerGradientId})`}
        badgeTextFill={`url(#${bannerTextGradientId})`}
        badgeGroupClass="bounce-part"
    >
        <svelte:fragment slot="defs">
            <linearGradient id={bannerGradientId} gradientUnits="userSpaceOnUse" spreadMethod="repeat"
                            x1="48" y1="16" x2="372" y2="164">
                <stop offset="0%" stop-color="var(--animated-logo-banner-start-color)"/>
                <stop offset="50%" stop-color="var(--animated-logo-banner-middle-color)"/>
                <stop offset="100%" stop-color="var(--animated-logo-banner-end-color)"/>
                <animateTransform
                        attributeName="gradientTransform"
                        type="translate"
                        dur="9.1s"
                        repeatCount="indefinite"
                        calcMode="linear"
                        values="0 0; 324 148"
                />
            </linearGradient>

            <linearGradient id={bannerTextGradientId} gradientUnits="userSpaceOnUse" spreadMethod="repeat"
                            x1="72" y1="34" x2="324" y2="149">
                <stop offset="0%" stop-color="var(--animated-logo-text-start-color)"/>
                <stop offset="50%" stop-color="var(--animated-logo-text-middle-color)"/>
                <stop offset="100%" stop-color="var(--animated-logo-text-end-color)"/>
                <animateTransform
                        attributeName="gradientTransform"
                        type="translate"
                        dur="7s"
                        repeatCount="indefinite"
                        calcMode="linear"
                        values="0 0; 252 115"
                />
            </linearGradient>
        </svelte:fragment>
    </LiquidBounceLogo>

    <div class="divider"></div>

    <div class="message">
        <div>Thank you for <span>{anniversary} Years</span></div>
        <div>of LiquidBounce!</div>
    </div>
</div>

<style>
    .animated-logo {
        display: flex;
        column-gap: 30px;
    }

    .divider {
        background-color: var(--animated-logo-divider-color);
        width: 2px;
        flex: 1;
    }

    .message {
        color: var(--animated-logo-foreground-color);
        font-size: 25px;
        font-weight: 500;
        line-height: 40px;
        justify-content: center;
        display: flex;
        flex-direction: column;

        span {
            color: var(--animated-logo-anniversary-highlight-color);
            font-weight: 600;
        }
    }

    :global(.bounce-part) {
        filter: drop-shadow(0 0 16px var(--animated-logo-glow-primary-color))
            drop-shadow(0 0 30px var(--animated-logo-glow-secondary-color));
    }
</style>
