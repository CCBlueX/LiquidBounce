<script lang="ts">
    import {fade} from "svelte/transition";

    type ConfettiPiece = {
        id: number;
        shapeClass: string;
        style: string;
    };

    const colors = [
        "#4677ff",
        "#fc4130",
        "#67d24a",
        "#ffd166",
        "#ffffff",
        "#8ad9ff",
        "#ff8fab"
    ];

    const pieceCount = 84;
    const columnCount = 42;
    const horizontalPadding = -8;
    const horizontalSpread = 116;
    const goldenStep = 0.61803398875;

    const pieces: ConfettiPiece[] = Array.from({length: pieceCount}, (_, index) => createPiece(index));

    function random(seed: number) {
        const value = Math.sin(seed * 12.9898) * 43758.5453;
        return value - Math.floor(value);
    }

    function fractional(value: number) {
        return value - Math.floor(value);
    }

    function createPiece(index: number): ConfettiPiece {
        const shapeValue = random(index + 11);
        const baseSize = 6 + random(index + 23) * 12;
        const isRound = shapeValue > 0.7;
        const isStreamer = shapeValue < 0.2;
        const columnWidth = horizontalSpread / columnCount;
        const column = index % columnCount;
        const layer = Math.floor(index / columnCount);

        const width = isRound ? baseSize : isStreamer ? baseSize * 0.45 : baseSize * 0.72;
        const height = isRound ? baseSize : isStreamer ? baseSize * 1.8 : baseSize * 1.24;
        const borderRadius = isRound ? "999px" : `${1 + random(index + 31) * 4}px`;
        const duration = 7 + random(index + 41) * 9;
        const delayProgress = fractional(index * goldenStep + random(index + 53) * 0.2);
        const delay = -(delayProgress * duration);
        const leftJitter = (random(index + 67) - 0.5) * columnWidth * 0.8;
        const left = horizontalPadding + (column + 0.5) * columnWidth + leftJitter;
        const driftDirection = (column + layer) % 2 === 0 ? -1 : 1;
        const driftMagnitude = 34 + random(index + 79) * 74;
        const drift = driftDirection * driftMagnitude + (random(index + 149) - 0.5) * 12;
        const opacity = 0.45 + random(index + 83) * 0.35;
        const flip = 240 + random(index + 97) * 540;
        const spinDuration = 1.8 + random(index + 101) * 2.8;
        const color = colors[Math.floor(random(index + 113) * colors.length)];
        const scale = 0.75 + random(index + 127) * 0.85;
        const blur = random(index + 137) > 0.86 ? 1 : 0;

        return {
            id: index,
            shapeClass: isRound ? "is-round" : isStreamer ? "is-streamer" : "",
            style: [
                `--left:${left}%;`,
                `--drift:${drift}px;`,
                `--fall-duration:${duration.toFixed(2)}s;`,
                `--fall-delay:${delay.toFixed(2)}s;`,
                `--spin-duration:${spinDuration.toFixed(2)}s;`,
                `--width:${width.toFixed(2)}px;`,
                `--height:${height.toFixed(2)}px;`,
                `--opacity:${opacity.toFixed(2)};`,
                `--color:${color};`,
                `--flip:${flip.toFixed(0)}deg;`,
                `--scale:${scale.toFixed(2)};`,
                `--blur:${blur}px;`,
                `--border-radius:${borderRadius};`
            ].join("")
        };
    }
</script>

<div class="confetti-layer" aria-hidden="true" transition:fade|global={{duration: 500}}>
    {#each pieces as piece (piece.id)}
        <span class={`confetti-piece ${piece.shapeClass}`} style={piece.style}>
            <span class="confetti-bit"></span>
        </span>
    {/each}
</div>

<style>
    .confetti-layer {
        position: fixed;
        inset: 0;
        overflow: hidden;
        pointer-events: none;
        z-index: -1;
        contain: layout style paint;
    }

    .confetti-piece {
        position: absolute;
        top: -18vh;
        left: var(--left);
        opacity: var(--opacity);
        will-change: transform;
        animation: confetti-fall var(--fall-duration) linear infinite;
        animation-delay: var(--fall-delay);
    }

    .confetti-bit {
        display: block;
        width: var(--width);
        height: var(--height);
        background: var(--color);
        border-radius: var(--border-radius);
        filter: blur(var(--blur));
        box-shadow: 0 0 10px rgba(255, 255, 255, 0.12);
        transform-origin: center;
        transform-style: preserve-3d;
        backface-visibility: hidden;
        animation: confetti-spin var(--spin-duration) ease-in-out infinite alternate;
    }

    .confetti-piece.is-streamer .confetti-bit {
        border-radius: 999px;
    }

    @keyframes confetti-fall {
        from {
            transform: translate3d(0, -20vh, 0);
        }

        to {
            transform: translate3d(var(--drift), 120vh, 0);
        }
    }

    @keyframes confetti-spin {
        0% {
            transform: rotate(0deg) scaleX(var(--scale)) scaleY(var(--scale));
        }

        25% {
            transform: rotate(calc(var(--flip) * 0.25)) scaleX(calc(var(--scale) * 0.68)) scaleY(calc(var(--scale) * 0.94));
        }

        50% {
            transform: rotate(calc(var(--flip) * 0.5)) scaleX(calc(var(--scale) * 0.48)) scaleY(calc(var(--scale) * 0.88));
        }

        75% {
            transform: rotate(calc(var(--flip) * 0.75)) scaleX(calc(var(--scale) * 0.72)) scaleY(calc(var(--scale) * 0.96));
        }

        100% {
            transform: rotate(var(--flip)) scaleX(var(--scale)) scaleY(var(--scale));
        }
    }
</style>
