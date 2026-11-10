<script lang="ts">
    import type {MarkedToken, Token} from "marked";
    import {browseUrl} from "../../../integration/rest";
    import {decodeEntities, isImageParagraph, toWebUrl} from "../../../util/markdown";

    /**
     * Renders lexed Markdown as markup, so nothing of it reaches the page as HTML. Images belong to a gallery;
     * only one inside a link shows, as its alt text. Without [links], as inside a button, links are plain text.
     */
    let {tokens, inline = false, links = true}: {
        tokens: Token[];
        inline?: boolean;
        links?: boolean;
    } = $props();

    function openLink(event: MouseEvent, href: string) {
        event.preventDefault();
        browseUrl(href);
    }

    function isLineBreak(html: string): boolean {
        return /^<br\s*\/?>$/i.test(html.trim());
    }
</script>

{#snippet spans(list: Token[], inLink: boolean)}
    {#each list as raw, index (index)}
        {@const token = raw as MarkedToken}
        {#if token.type === "strong"}<strong>{@render spans(token.tokens, inLink)}</strong>
        {:else if token.type === "em"}<em>{@render spans(token.tokens, inLink)}</em>
        {:else if token.type === "del"}<del>{@render spans(token.tokens, inLink)}</del>
        {:else if token.type === "codespan"}<code>{token.text}</code>
        {:else if token.type === "br"}<br/>
        {:else if token.type === "link"}
            {@const href = links && !inLink ? toWebUrl(decodeEntities(token.href)) : undefined}
            {#if href}
                <a {href} onclick={event => openLink(event, href)}
                   onauxclick={event => event.preventDefault()}>{@render spans(token.tokens, true)}</a>
            {:else}{@render spans(token.tokens, true)}{/if}
        {:else if token.type === "image"}{#if inLink}{decodeEntities(token.text)}{/if}
        {:else if token.type === "html"}{#if isLineBreak(token.text)}<br/>{:else}{token.text}{/if}
        {:else if token.type === "text" && token.tokens}{@render spans(token.tokens, inLink)}
        {:else if token.type === "text" || token.type === "escape"}{decodeEntities(token.text)}
        {/if}
    {/each}
{/snippet}

{#snippet blocks(list: Token[])}
    {#each list as raw, index (index)}
        {@const token = raw as MarkedToken}
        {#if token.type === "heading"}
            <svelte:element this={`h${Math.min(6, token.depth + 3)}`} class="heading" class:minor={token.depth > 2}>
                {@render spans(token.tokens, false)}
            </svelte:element>
        {:else if token.type === "paragraph"}
            {#if !isImageParagraph(token)}<p>{@render spans(token.tokens, false)}</p>{/if}
        {:else if token.type === "text"}
            {@render spans(token.tokens ?? [token], false)}
        {:else if token.type === "list"}
            <svelte:element this={token.ordered ? "ol" : "ul"}
                            start={token.ordered && token.start !== "" ? token.start : undefined}>
                {#each token.items as item, itemIndex (itemIndex)}
                    <li>{@render blocks(item.tokens)}</li>
                {/each}
            </svelte:element>
        {:else if token.type === "blockquote"}
            <blockquote>{@render blocks(token.tokens)}</blockquote>
        {:else if token.type === "code"}
            <pre><code>{token.text}</code></pre>
        {:else if token.type === "hr"}
            <hr/>
        {:else if token.type === "table"}
            <div class="table">
                <table>
                    <thead>
                    <tr>
                        {#each token.header as cell, column (column)}
                            <th style:text-align={cell.align}>{@render spans(cell.tokens, false)}</th>
                        {/each}
                    </tr>
                    </thead>
                    <tbody>
                    {#each token.rows as row, rowIndex (rowIndex)}
                        <tr>
                            {#each row as cell, column (column)}
                                <td style:text-align={cell.align}>{@render spans(cell.tokens, false)}</td>
                            {/each}
                        </tr>
                    {/each}
                    </tbody>
                </table>
            </div>
        {/if}
    {/each}
{/snippet}

{#if inline}
    {@render spans(tokens, false)}
{:else}
    <div class="markdown">{@render blocks(tokens)}</div>
{/if}

<style lang="scss">
  .markdown {
    color: var(--clickgui-text-dimmed-color);
    font-size: 13px;
    line-height: 1.5;
    overflow-wrap: anywhere;
    user-select: text;

    > :global(:first-child) {
      margin-top: 0;
    }

    > :global(:last-child) {
      margin-bottom: 0;
    }
  }

  p, ul, ol, blockquote, pre, .table {
    margin: 0 0 8px;
  }

  .heading {
    margin: 14px 0 4px;
    color: var(--clickgui-text-color);
    font-size: 14px;
    font-weight: 600;

    &.minor {
      font-size: 13px;
    }
  }

  strong {
    color: var(--clickgui-text-color);
    font-weight: 600;
  }

  del {
    opacity: 0.7;
  }

  a {
    color: var(--clickgui-marketplace-link-color);
    text-underline-offset: 2px;
    cursor: pointer;
  }

  code {
    padding: 1px 4px;
    border-radius: 3px;
    background-color: var(--clickgui-marketplace-code-background-color);
    font-family: monospace;
    font-size: 0.92em;
  }

  pre {
    padding: 8px 10px;
    border-radius: 3px;
    background-color: var(--clickgui-marketplace-code-background-color);
    overflow-x: auto;

    code {
      padding: 0;
      background: none;
      font-size: 12px;
    }
  }

  ul, ol {
    padding-left: 18px;
  }

  li + li {
    margin-top: 2px;
  }

  blockquote {
    padding-left: 10px;
    border-left: solid 2px var(--clickgui-setting-group-border-color);
  }

  hr {
    height: 1px;
    margin: 12px 0;
    border: 0;
    background-color: var(--clickgui-marketplace-divider-color);
  }

  .table {
    overflow-x: auto;
  }

  table {
    border-collapse: collapse;
    font-size: 12px;
  }

  th, td {
    padding: 5px 8px;
    text-align: left;
    vertical-align: top;
    border-bottom: solid 1px var(--clickgui-marketplace-divider-color);
  }

  th {
    color: var(--clickgui-text-color);
    font-weight: 600;
  }
</style>
