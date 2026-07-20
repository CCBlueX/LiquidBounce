<script lang="ts">
    import {getComponentFileUrl} from "../../../integration/rest";

    interface Props {
        componentId: string;
        settings: { [name: string]: any };
    }

    const {componentId, settings}: Props = $props();

    const maxVisualHeight = 70;

    let scale = $derived(resolveScale(settings.scale));
    let maxHeight = $derived(`${maxVisualHeight / scale}vh`);
    let source = $derived(resolveSource());

    function resolveSource() {
        if (settings.source !== "File") {
            return settings.uRL;
        }

        return settings.file
            ? getComponentFileUrl(componentId, hash(settings.file))
            : "";
    }

    function hash(value: string) {
        let result = 0;

        for (let index = 0; index < value.length; index++) {
            result = Math.imul(31, result) + value.charCodeAt(index) | 0;
        }

        return result.toString(36);
    }

    function resolveScale(value: unknown) {
        const scale = Number(value);

        return Number.isFinite(scale) && scale > 0 ? scale : 1;
    }
</script>

{#if source}
    <img src={source} alt="" class="image" style:zoom={scale} style:max-height={maxHeight}
         style:opacity="{settings.opacity}%">
{/if}

<style lang="scss">
  .image {
    display: block;
    height: auto;
    object-fit: contain;
  }
</style>
