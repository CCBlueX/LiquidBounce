<script lang="ts">
    import {listen} from "../../../integration/ws";
    import type {ClientPlayerDataEvent} from "../../../integration/events";
    import type {PlayerData} from "../../../integration/types";
    import {rgbaToHex} from "../../../integration/util";
    import {intToRgba} from "../../../integration/util.js";

    let playerData: PlayerData | null = null;
    let processedText: string = '';

    export let settings: { [name: string]: any };

    listen("clientPlayerData", (event: ClientPlayerDataEvent) => {
        playerData = event.playerData;
        processText();
    });

    function processText() {
        if (!settings.text || !playerData) {
            processedText = settings.text || '';
            return;
        }

        processedText = settings.text.replace(/{(\w+(\.\w+)*)}/g, (match: string, p1: string) => {
            const keys = p1.split(".");
            let value: any = playerData;

            for (const key of keys) {
                value = value ? value[key] : null;
            }

            if (value !== null && value !== undefined) {
                switch (typeof value) {
                    case 'number':
                        if (value % 1 === 0) {
                            return value.toString();
                        }

                        return value.toFixed(2);
                    case 'object':
                        return JSON.stringify(value);
                    default:
                        return value.toString();
                }
            }

            return match;
        });
    }

    // Process text on mount
    $: processText();
</script>

<div class="text" style="
    font-family: {settings.font};
    font-size: {settings.size}px;
    color: {rgbaToHex(intToRgba(settings.color))};
    font-weight: {settings.decorations.bold ? 'bold' : 'normal'};
    font-style: {settings.decorations.italic ? 'italic' : 'normal'};
    text-decoration:
      {settings.decorations.underline ? 'underline ' : ''}
      {settings.decorations.strikethrough ? 'line-through' : ''};
    text-shadow:
      {settings.shadow.enabled
        ? `${settings.shadow.offsetX}px ${settings.shadow.offsetY}px ${settings.shadow.blurRadius}px ${rgbaToHex(intToRgba(settings.shadow.color))}`
        : 'none'};
    filter: {settings.glow.enabled ? `drop-shadow(0px 0px ${settings.glow.radius}px ${rgbaToHex(intToRgba(settings.glow.color))}` : 'none'};
">
    {processedText}
</div>

<style lang="scss">
    @use "../../../colors.scss" as *;

    .text {
        position: absolute;
        white-space: nowrap;
        user-select: none;
        pointer-events: none;
        z-index: 1000;
    }
</style>
