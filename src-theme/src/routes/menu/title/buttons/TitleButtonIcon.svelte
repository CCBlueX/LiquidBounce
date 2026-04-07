<script lang="ts">
    export let icon: string;

    $: iconPath = `img/menu/icon-${icon}.svg`;
</script>

<span
        class="title-button-icon"
        aria-hidden="true"
        style={`mask-image: url('${location.origin}${location.pathname}/${iconPath}');`}
>
    <img class="title-button-icon-size" src={iconPath} alt="" />
</span>

<style>
    .title-button-icon {
        display: inline-block;
        background-color: currentColor;
        mask-position: center;
        mask-repeat: no-repeat;
        mask-size: contain;
    }

    .title-button-icon-size {
        display: block;
        visibility: hidden;
    }
</style>
