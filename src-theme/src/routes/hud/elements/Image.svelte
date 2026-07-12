<script lang="ts">
    interface Props {
        settings: { [name: string]: any };
    }

    const {settings}: Props = $props();

    const maxVisualHeight = 70;

    let scale = $derived(resolveScale(settings.scale));
    let maxHeight = $derived(`${maxVisualHeight / scale}vh`);

    function resolveScale(value: unknown) {
        const scale = Number(value);

        return Number.isFinite(scale) && scale > 0 ? scale : 1;
    }
</script>

<img src={settings.uRL} alt="" class="image" style:zoom={scale} style:max-height={maxHeight}
     style:opacity="{settings.opacity}%">

<style lang="scss">
  .image {
    display: block;
    height: auto;
    object-fit: contain;
  }
</style>
