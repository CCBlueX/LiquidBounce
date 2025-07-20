<script lang="ts">
    import type {TextComponent as TTextComponent} from "../../../integration/types";

    export let textComponent: TTextComponent | string;
    export let allowPreformatting = false;
    export let preFormattingMonospace = true;
    export let inheritedColor = "#ffffff";
    export let inheritedStrikethrough = false;
    export let inheritedItalic = false;
    export let inheritedUnderlined = false;
    export let inheritedBold = false;
    export let fontSize: number;

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
        <svelte:self {fontSize} {allowPreformatting} {preFormattingMonospace} textComponent={convertLegacyCodes(textComponent)}/>
    {:else if textComponent}
        {#if textComponent.text}
            {#if !textComponent.text.includes("§")}
                <span class="text" class:bold={textComponent.bold !== undefined ? textComponent.bold : inheritedBold}
                      class:italic={textComponent.italic !== undefined ? textComponent.italic : inheritedItalic}
                      class:underlined={textComponent.underlined !== undefined ? textComponent.underlined : inheritedUnderlined}
                      class:strikethrough={textComponent.strikethrough !== undefined ? textComponent.strikethrough : inheritedStrikethrough}
                      class:allow-preformatting={allowPreformatting}
                      class:monospace={preFormattingMonospace && allowPreformatting}
                      style="color: {textComponent.color !== undefined ? translateColor(textComponent.color) : translateColor(inheritedColor)}; font-size: {fontSize}px;">{textComponent.text}</span>
            {:else}
                <svelte:self {allowPreformatting} {preFormattingMonospace} {fontSize}
                             inheritedColor={textComponent.color !== undefined ? textComponent.color : inheritedColor}
                             inheritedBold={textComponent.bold !== undefined ? textComponent.bold : inheritedBold}
                             inheritedItalic={textComponent.italic !== undefined ? textComponent.italic : inheritedItalic}
                             inheritedUnderlined={textComponent.underlined !== undefined ? textComponent.underlined : inheritedUnderlined}
                             inheritedStrikethrough={textComponent.strikethrough !== undefined ? textComponent.strikethrough : inheritedStrikethrough}
                             textComponent={convertLegacyCodes(textComponent.text)}/>
            {/if}
        {/if}
        {#if textComponent.extra}
            {#each textComponent.extra as e}
                <svelte:self {allowPreformatting} {preFormattingMonospace} {fontSize}
                             inheritedColor={textComponent.color !== undefined ? textComponent.color : inheritedColor}
                             inheritedBold={textComponent.bold !== undefined ? textComponent.bold : inheritedBold}
                             inheritedItalic={textComponent.italic !== undefined ? textComponent.italic : inheritedItalic}
                             inheritedUnderlined={textComponent.underlined !== undefined ? textComponent.underlined : inheritedUnderlined}
                             inheritedStrikethrough={textComponent.strikethrough !== undefined ? textComponent.strikethrough : inheritedStrikethrough}
                             textComponent={e}/>
            {/each}
        {/if}
    {/if}
</span>

<style>
    .text-component {
        font-size: 0;
    }

    .text {
        display: inline;

        &.allow-preformatting {
            white-space: pre;
        }

        &.monospace {
            font-family: monospace;
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
