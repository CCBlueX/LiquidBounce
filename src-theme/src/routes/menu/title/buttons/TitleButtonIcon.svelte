<script lang="ts">
    export let icon: string;
    export let size = "100%";

    $: iconMaskImage = `url('/img/menu/icon-${icon}.svg')`;
</script>

<span
        class="title-button-icon"
        aria-hidden="true"
        style={`--icon-mask-image: ${iconMaskImage}; --icon-size: ${size};`}
></span>

<style>
    .title-button-icon {
        display: block;
        width: var(--icon-size);
        height: var(--icon-size);
        background-color: currentColor;
        -webkit-mask-image: var(--icon-mask-image);
        mask-image: var(--icon-mask-image);
        -webkit-mask-position: center;
        mask-position: center;
        -webkit-mask-repeat: no-repeat;
        mask-repeat: no-repeat;
        -webkit-mask-size: contain;
        mask-size: contain;
    }
</style>
