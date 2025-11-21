<script lang="ts">
    import {listen} from "../../../integration/ws";
    import type {ClientPlayerDataEvent} from "../../../integration/events";
    import type {PlayerData} from "../../../integration/types";
    import {rgbaToHex} from "../../../integration/util";
    import {intToRgba} from "../../../integration/util.js";

    let playerData: PlayerData | null = null;
    let processedText: string = '';

    export let settings: { [name: string]: any };

    const cSettings = settings as HudTextSettings;

    listen("clientPlayerData", (event: ClientPlayerDataEvent) => {
        playerData = event.playerData;
        processText();
    });

    function processText() {
        if (!cSettings.text || !playerData) {
            processedText = cSettings.text || '';
            return;
        }

        processedText = cSettings.text.replace(/{(\w+(\.\w+)*)}/g, (match: string, p1: string) => {
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
    font-family: {cSettings.font};
    font-size: {cSettings.size}px;
    color: {rgbaToHex(intToRgba(cSettings.color))};
    font-weight: {cSettings.decorations.bold ? 'bold' : 'normal'};
    font-style: {cSettings.decorations.italic ? 'italic' : 'normal'};
    text-decoration:
      {cSettings.decorations.underline ? 'underline ' : ''}
      {cSettings.decorations.strikethrough ? 'line-through' : ''};
    text-shadow:
      {cSettings.shadow.enabled
        ? `${cSettings.shadow.offsetX}px ${cSettings.shadow.offsetY}px ${cSettings.shadow.blurRadius}px ${rgbaToHex(intToRgba(cSettings.shadow.color))}`
        : 'none'};
    filter: {cSettings.glow.enabled ? `drop-shadow(0px 0px ${cSettings.glow.radius}px ${rgbaToHex(intToRgba(cSettings.glow.color))}` : 'none'};
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
