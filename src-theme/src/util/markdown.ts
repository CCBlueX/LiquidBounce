import {Lexer, type MarkedToken, type Token, type Tokens} from "marked";

export interface MarkdownImage {
    src: string;
    alt: string;
}

/**
 * The blocks of a description with its images taken out for a gallery.
 */
export interface MarkdownDocument {
    blocks: MarkedToken[];
    images: MarkdownImage[];
}

/**
 * The link as the browser would load it. Relative, `javascript:` and `data:` links give `undefined`.
 */
export function toWebUrl(url: string): string | undefined {
    try {
        const parsed = new URL(url);
        return parsed.protocol === "https:" || parsed.protocol === "http:" ? parsed.href : undefined;
    } catch {
        return undefined;
    }
}

let entityDecoder: HTMLTextAreaElement | undefined;

/**
 * marked leaves character references such as `&amp;` in text. A textarea decodes them without parsing tags.
 */
export function decodeEntities(text: string): string {
    if (!text.includes("&")) {
        return text;
    }

    entityDecoder ??= document.createElement("textarea");
    entityDecoder.innerHTML = text;
    return entityDecoder.value;
}

function lex(source: string): MarkedToken[] {
    // Without extensions the lexer only makes the tokens marked knows
    return Lexer.lex(source, {gfm: true}) as MarkedToken[];
}

function walk(tokens: Token[], visit: (token: MarkedToken, inLink: boolean) => void, inLink = false) {
    for (const token of tokens as MarkedToken[]) {
        visit(token, inLink);
        if (token.type === "list") {
            walk(token.items, visit, inLink);
        } else if (token.type === "table") {
            for (const cell of [...token.header, ...token.rows.flat()]) {
                walk(cell.tokens, visit, inLink);
            }
        } else if ("tokens" in token && token.tokens) {
            walk(token.tokens, visit, inLink || token.type === "link");
        }
    }
}

function toPlainText(tokens: Token[]): string {
    let text = "";
    walk(tokens, token => {
        if ((token.type === "text" && !token.tokens) || token.type === "codespan" || token.type === "escape") {
            text += token.text;
        }
    });
    return decodeEntities(text);
}

/**
 * Whether the paragraph holds nothing but images, which the gallery takes.
 */
export function isImageParagraph(paragraph: Tokens.Paragraph): boolean {
    return paragraph.tokens.every(token =>
        token.type === "image" || token.type === "br" || (token.type === "text" && !token.text.trim())
    );
}

function isSameText(text: string, other: string | undefined): boolean {
    return !!other && text.trim().toLowerCase() === other.trim().toLowerCase();
}

/**
 * Parses a description. Images inside a link stay with the link, every other one goes to the gallery. Headings
 * whose section held only images are dropped, and so is a first heading that repeats [title].
 */
export function parseMarkdown(source: string, title?: string): MarkdownDocument {
    const tokens = lex(source);
    const images: MarkdownImage[] = [];
    walk(tokens, (token, inLink) => {
        const src = token.type === "image" && !inLink ? toWebUrl(token.href) : undefined;
        if (src && token.type === "image") {
            images.push({src, alt: decodeEntities(token.text)});
        }
    });

    const kept = tokens.filter(token =>
        token.type !== "space"
        && token.type !== "def"
        && !(token.type === "html" && token.block)
        && !(token.type === "paragraph" && isImageParagraph(token))
    );

    // Backwards, so a heading whose subheadings were all dropped counts as empty too
    const blocks: MarkedToken[] = [];
    for (let index = kept.length - 1; index >= 0; index--) {
        const token = kept[index];
        const next = blocks[0];
        if (token.type === "heading" && (!next || (next.type === "heading" && next.depth <= token.depth))) {
            continue;
        }
        blocks.unshift(token);
    }

    const first = blocks[0];
    if (first?.type === "heading" && isSameText(toPlainText(first.tokens), title)) {
        blocks.shift();
    }
    return {blocks, images};
}

/**
 * The inline tokens of a one-line summary without its images. A line of only images, or one that repeats
 * [title], gives nothing.
 */
export function parseMarkdownSummary(source: string, title?: string): Token[] {
    let token: MarkedToken | undefined = lex(source).find(block => block.type !== "space");
    while (token?.type === "list" || token?.type === "list_item" || token?.type === "blockquote") {
        const children: MarkedToken[] = token.type === "list" ? token.items : (token.tokens as MarkedToken[]);
        token = children.find(child => child.type !== "checkbox");
    }

    const inline = token && "tokens" in token && token.tokens
        ? token.tokens.filter(child => child.type !== "image")
        : [];
    const text = toPlainText(inline);
    return !text.trim() || isSameText(text, title) ? [] : inline;
}
