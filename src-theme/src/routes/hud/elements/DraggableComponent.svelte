<script lang="ts">
    import {type Alignment, HorizontalAlignment, VerticalAlignment} from "../../../integration/types.js";

    export let name: string;
    export let id: string;
    export let alignment: Alignment;
    let element: HTMLElement | undefined;

    $: styleString = generateStyleString(alignment);

    function generateStyleString(alignment: Alignment): string {
        let style = "position: fixed;";

        switch (alignment.horizontalAlignment) {
            case HorizontalAlignment.LEFT:
                style += `left: ${alignment.horizontalOffset}px;`;
                break;
            case HorizontalAlignment.RIGHT:
                style += `right: ${alignment.horizontalOffset}px;`;
                break;
            case HorizontalAlignment.CENTER:
            case HorizontalAlignment.CENTER_TRANSLATED:
                style += `left: calc(50% + ${alignment.horizontalOffset}px);`;
                break;
        }

        switch (alignment.verticalAlignment) {
            case VerticalAlignment.TOP:
                style += `top: ${alignment.verticalOffset}px;`;
                break;
            case VerticalAlignment.BOTTOM:
                style += `bottom: ${alignment.verticalOffset}px;`;
                break;
            case VerticalAlignment.CENTER:
            case VerticalAlignment.CENTER_TRANSLATED:
                style += `top: calc(50% + ${alignment.verticalOffset}px);`;
                break;
        }

        style += "transform: translate("
        if (alignment.horizontalAlignment === HorizontalAlignment.CENTER_TRANSLATED) {
            style += "-50%,";
        } else {
            style += "0,";
        }
        if (alignment.verticalAlignment === VerticalAlignment.CENTER_TRANSLATED) {
            style += "-50%);";
        } else {
            style += "0);"
        }

        return style;
    }
</script>

<div class="draggable-element" style={styleString} bind:this={element}>
    <!-- svelte-ignore a11y-no-static-element-interactions -->
    <div class="contained-element">
        <slot/>
    </div>
</div>

<style lang="scss">
  @use "../../../colors.scss" as *;

  .draggable-element {
    position: relative;
  }

  .contained-element {
    min-width: 50px;
    min-height: 50px;
  }
</style>
