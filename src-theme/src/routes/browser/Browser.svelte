<script lang="ts">
    import type {Browser} from "../../integration/types.js";
    import {onMount} from "svelte";
    import {
        browserForceReload,
        browserGoBack,
        browserGoForward,
        browserNavigate,
        browserReload,
        getBrowser
    } from "../../integration/rest.js";
    import {listen} from "../../integration/ws";
    import type {BrowserUrlChangeEvent} from "../../integration/events";
    import {delay} from "../../integration/util";

    let browser: Browser;

    async function loadBrowser() {
        browser = await getBrowser();
    }

    onMount(async () => {
        await delay(250);
        await loadBrowser();
    });

    async function onKeyPress(event: KeyboardEvent) {
        if (event.key === "Enter") {
            await browserNavigate(browser.url);
        }
    }

    async function handleGo() {
        await browserNavigate(browser.url);
    }

    async function handleBack() {
        await browserGoBack();
    }

    async function handleForward() {
        await browserGoForward();
    }

    async function handleReload() {
        await browserReload();
    }

    async function handleForceReload() {
        await browserForceReload();
    }

    listen("browserUrlChange", (e: BrowserUrlChangeEvent) => {
        browser.url = e.url;
    });
</script>

<style lang="scss">
    .browser-controls {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 6px;
        position: fixed;
        bottom: 16px;
        left: 16px;
        right: 16px;
        background-color: var(--menu-base-90-color);
        backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturation));
        padding: 8px;
        border: 1px solid var(--accent-border-color);
        border-radius: var(--radius-lg);
        box-shadow: var(--elevation-md) var(--menu-base-50-color);
    }

    .address-bar {
        flex-grow: 1;
        margin: 0 4px;
    }

    input {
        width: 100%;
        padding: 10px 12px;
        background-color: var(--menu-base-50-color);
        color: var(--menu-text-color);
        border: 1px solid var(--surface-border-color);
        border-radius: var(--radius-sm);
        font-size: 14px;
        transition: border-color var(--duration-fast) var(--ease-standard),
        box-shadow var(--duration-fast) var(--ease-standard);

        &:focus-visible {
            border-color: var(--accent-color);
            box-shadow: 0 0 0 3px var(--accent-subtle-background-color);
        }
    }

    button {
        background-color: var(--accent-subtle-background-color);
        color: var(--menu-text-color);
        border: 1px solid var(--accent-border-color);
        padding: 10px 14px;
        border-radius: var(--radius-sm);
        cursor: pointer;
        font-size: 14px;
        font-weight: 500;
        white-space: nowrap;
        transition: background-color var(--duration-fast) var(--ease-standard),
        border-color var(--duration-fast) var(--ease-standard);

        &:hover:not(:disabled) {
            background-color: var(--menu-button-background-color);
            border-color: var(--accent-border-strong-color);
        }
    }

    button:disabled {
        cursor: not-allowed;
        opacity: 0.45;
    }
</style>

{#if browser}
    <div class="browser-controls">
        <button type="button" aria-label="Go back" on:click={handleBack}>&larr;</button>
        <button type="button" aria-label="Go forward" on:click={handleForward}>&rarr;</button>
        <button type="button" aria-label="Reload" on:click={handleReload}>&#x21bb;</button>
        <div class="address-bar">
            <input id="url" aria-label="URL" bind:value={browser.url} on:keypress={onKeyPress} placeholder="Enter URL" />
        </div>
        <button type="button" on:click={handleGo}>Go</button>
        <button type="button" on:click={handleForceReload}>Force Reload</button>
    </div>
{/if}
