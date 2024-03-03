<script lang="ts">
    import type {TextComponent as TTextComponent} from "../../../integration/types";

    export let textComponent: TTextComponent | string;
    export let allowPreformatting = false;
    export let inheritedColor = "#ffffff";

    const colors: { [name: string]: string } = {
        black: "#000000",
        dark_blue: "#0000aa",
        dark_green: "#00aa00",
        dark_aqua: "#00aaaa",
        dark_red: "#aa0000",
        dark_purple: "#aa00aa",
        gold: "#ffaa00",
        gray: "#aaaaaa",
        dark_gray: "#555555",
        blue: "#5555ff",
        green: "#55ff55",
        aqua: "#55ffff",
        red: "#ff5555",
        light_purple: "#ff55ff",
        yellow: "#ffff55",
        white: "#ffffff"
    };

    function translateColor(color: string): string {
        if (!color) {
            return colors.white;
        }
        if (color.startsWith("#")) {
            return color;
        } else {
            return colors[color];
        }
    }

    function convertLegacyCodes(text: string) {
        let obfuscated = false;
        let bold = false;
        let strikethrough = false;
        let underlined = false;
        let italic = false;
        let color = colors.black;

        function reset() {
            obfuscated = false;
            bold = false;
            strikethrough = false;
            underlined = false;
            italic = false;
            color = colors.black;
        }

        const components: TTextComponent[] = [];
        const textParts = (text.startsWith("§") ? text : `§f${text}`).split("§");

        for (const p of textParts) {
            const code = p.charAt(0);
            const t = p.slice(1);

            switch (code) {
                case "k":
                    obfuscated = true;
                    break;
                case "l":
                    bold = true;
                    break;
                case "m":
                    strikethrough = true;
                    break;
                case "n":
                    underlined = true;
                    break;
                case "o":
                    italic = true;
                    break;
                case "r":
                    reset();
                    break;
                default:
                    color = colors[Object.keys(colors)[parseInt(code, 16)]] ?? colors.black;
                    break;
            }

            components.push({
                color,
                bold,
                italic,
                underlined,
                obfuscated,
                strikethrough,
                text: t,
            });
        }

        return {
            extra: components
        };
    }
</script>

<span class="text-component">
    {#if typeof textComponent === "string"}
        <svelte:self textComponent={convertLegacyCodes(textComponent)}/>
    {:else if textComponent}
        {#if textComponent.text }
            <span class="text" class:bold={textComponent.bold} class:italic={textComponent.italic}
                  class:underlined={textComponent.underlined}
                  class:strikethrough={textComponent.strikethrough}
                  class:allow-preformatting={allowPreformatting}
                  style="color: {textComponent.color ? translateColor(textComponent.color) : inheritedColor}">{textComponent.text}</span>
        {/if}
        {#if textComponent.extra}
            {#each textComponent.extra as e}
                <svelte:self {allowPreformatting} inheritedColor={textComponent.color} textComponent={e}/>
            {/each}
        {/if}
    {/if}
</span>

<style>
    .text-component {
        font-size: 0;
    }

    .text {
        font-size: 18px;
        display: inline;

        &.allow-preformatting {
            font-family: monospace;
            white-space: pre;
        }

        &.bold {
            font-weight: 500;
        }

        &.italic {
            font-style: italic;
        }

        &.underlined {
            text-decoration: underline;
        }

        &.strikethrough {
            text-decoration: line-through;
        }
    }
</style>
