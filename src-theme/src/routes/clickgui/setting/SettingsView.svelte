<script lang="ts">
    import type {Snippet} from "svelte";
    import {tick} from "svelte";
    import {fly} from "svelte/transition";
    import {cubicOut} from "svelte/easing";
    import type {BooleanSetting, ModuleSetting} from "../../../integration/types";
    import {escapeLayer} from "../../../integration/escape";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import Switch from "./common/Switch.svelte";
    import SettingsPage from "./SettingsPage.svelte";
    import {isModified, resetToDefault} from "./defaults";
    import {followTrail, hostPages, settingsOf} from "./pages";

    /**
     * Shows the settings below [root] one page at a time. A page is a group; groups deep inside it open as the
     * next page, with the way back in the header.
     */
    let {root = $bindable(), home = [], trail = $bindable([]), highlight = null, path, exclude = [], header, onchange}: {
        root: ModuleSetting;
        /**
         * The group the first page shows, for a root that holds more than this view is about.
         */
        home?: string[];
        /**
         * The groups opened as pages below [home].
         */
        trail?: string[];
        /**
         * A setting on the open page to scroll to and point out.
         */
        highlight?: string | null;
        /**
         * Where groups remember whether they are open.
         */
        path: string;
        /**
         * Settings of the first page that the host shows itself.
         */
        exclude?: string[];
        /**
         * The header of the first page. [reset] puts everything on it back to its defaults.
         */
        header: Snippet<[{ modified: boolean; reset: () => void }]>;
        onchange: () => void;
    } = $props();

    let body = $state<HTMLElement>();
    let forward = $state(true);
    let resets = $state(0);

    hostPages(next => open(next.slice(home.length)));

    // A mode switch or a config load can take a group away that a page was open on
    const groups = $derived(followTrail(root, [...home, ...trail]));
    const opened = $derived(groups.slice(home.length + 1));
    const page = $derived(groups[groups.length - 1]);
    const pageTrail = $derived(groups.slice(1).map(group => group.name));
    const settings = $derived(settingsOf(page).filter(setting => opened.length > 0 || !exclude.includes(setting.name)));
    const changed = $derived(settings.filter(isModified).length);
    const pageSwitch = $derived(opened.length > 0 && page.valueType === "TOGGLEABLE"
        ? (page.value as ModuleSetting[])[0] as BooleanSetting
        : null);

    const layer = escapeLayer(back);

    $effect(() => {
        layer.setOpen(opened.length > 0);
    });

    $effect(() => {
        const name = highlight;
        pageTrail;
        if (!name) {
            return;
        }

        tick().then(() => {
            const row = body?.querySelector<HTMLElement>(`[data-setting="${CSS.escape(name)}"]`);
            row?.scrollIntoView({block: "center"});
            row?.animate([
                {backgroundColor: "var(--accent-subtle-background-color)"},
                {backgroundColor: "var(--accent-subtle-background-color)", offset: 0.6},
                {backgroundColor: "transparent"},
            ], {duration: 1600, easing: "ease-out"});
        });
    });

    function spaced(name: string) {
        return $spaceSeperatedNames ? convertToSpacedString(name) : name;
    }

    // The pages that led here and where each was scrolled to, to go back to exactly that
    const visited: { trail: string[]; scroll: number }[] = [];

    async function show(next: string[], scroll: number) {
        forward = next.length > trail.length;
        trail = next;

        await tick();
        if (body) {
            body.scrollTop = scroll;
        }
    }

    function open(next: string[]) {
        visited.push({trail, scroll: body?.scrollTop ?? 0});
        show(next, 0);
    }

    function back() {
        const before = visited.pop() ?? {trail: trail.slice(0, -1), scroll: 0};
        show(before.trail, before.scroll);
    }

    function reset() {
        // Of a choice the page shows one mode, the others keep what they have
        if (page.valueType === "CHOICE") {
            settings.forEach(resetToDefault);
        } else if (opened.length > 0) {
            resetToDefault(page);
        } else {
            settings.forEach(resetToDefault);
        }
        resets++;
        onchange();
    }
</script>

{#if opened.length === 0}
    {@render header({modified: changed > 0, reset})}
{:else}
    <header class="crumbs">
        <div class="line">
            <button type="button" class="back" aria-label="Back" onclick={back}></button>
            <nav aria-label="Breadcrumb">
                <button type="button" onclick={() => open([])}>{spaced(groups[home.length].name)}</button>
                {#each opened as group, index (index)}
                    <span class="separator">/</span>
                    {#if index === opened.length - 1}
                        <span class="current">{spaced(group.name)}</span>
                    {:else}
                        <button type="button" onclick={() => open(trail.slice(0, index + 1))}>
                            {spaced(group.name)}
                        </button>
                    {/if}
                {/each}
            </nav>
            {#if changed > 0}
                <button type="button" class="reset" title="Reset to defaults" aria-label="Reset {spaced(page.name)}"
                        onclick={reset}></button>
            {/if}
            {#if pageSwitch}
                <Switch bind:value={pageSwitch.value} on:change={onchange}/>
            {/if}
        </div>
        <div class="meta">
            {settings.length} {settings.length === 1 ? "setting" : "settings"}{changed > 0 ? `, ${changed} changed` : ""}
        </div>
    </header>
{/if}

<div class="body" bind:this={body}>
    {#if settings.length === 0}
        <p class="empty">Nothing to set here.</p>
    {/if}
    {#key pageTrail.join("/")}
        <!-- The page that is left goes at once: the two are scrolled differently and cannot be shown together -->
        <div class="page" in:fly={{x: forward ? 16 : -16, opacity: 0.3, duration: 160, easing: cubicOut}}>
            <!-- Controls only read their value when they are created, so a reset builds the page again -->
            {#key resets}
                <SettingsPage
                        bind:root
                        trail={pageTrail}
                        path="{path}{pageTrail.map(name => `.${name}`).join('')}"
                        exclude={opened.length > 0 ? [] : exclude}
                        {onchange}
                />
            {/key}
        </div>
    {/key}
</div>

<style lang="scss">
  .crumbs {
    flex: none;
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 14px 18px 12px 12px;
    border-bottom: 1px solid var(--clickgui-divider-color);
  }

  .line {
    display: flex;
    align-items: center;
    gap: 8px;
    min-height: 26px;
  }

  nav {
    flex: 1;
    min-width: 0;
    display: flex;
    align-items: center;
    gap: 6px;
    overflow: hidden;
    font-size: 16px;
    font-weight: 600;
    white-space: nowrap;

    button {
      flex: 0 1 auto;
      min-width: 24px;
      overflow: hidden;
      text-overflow: ellipsis;
      border: none;
      background: none;
      padding: 0;
      font: inherit;
      font-weight: 500;
      color: var(--clickgui-text-dimmed-color);
      cursor: pointer;
      transition: ease color .15s;

      &:hover, &:focus-visible {
        color: var(--clickgui-text-color);
      }
    }
  }

  .separator {
    flex: none;
    font-weight: 400;
    color: var(--clickgui-text-muted-color);
  }

  .current {
    flex: none;
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .back, .reset {
    flex: none;
    width: 26px;
    height: 26px;
    border: none;
    border-radius: 3px;
    background: center no-repeat;
    opacity: .8;
    cursor: pointer;
    transition: ease opacity .15s, ease background-color .15s;

    &:hover, &:focus-visible {
      opacity: 1;
      background-color: var(--clickgui-control-hover-background-color);
    }
  }

  .back {
    background-image: url("/img/menu/icon-back.svg");
    background-size: 15px;
  }

  .reset {
    background-image: url("/img/clickgui/icon-reset.svg");
    background-size: 12px;
  }

  .meta {
    padding-left: 34px;
    font-size: 12px;
    color: var(--clickgui-text-muted-color);
  }

  .body {
    flex: 1;
    min-height: 0;
    display: grid;
    grid-template-columns: minmax(0, 1fr);
    align-content: start;
    overflow-x: hidden;
    overflow-y: auto;
    scrollbar-gutter: stable;
  }

  .empty {
    padding: 18px;
    font-size: 12px;
    color: var(--clickgui-text-muted-color);
  }

  .page {
    min-width: 0;
    padding: 6px 12px 14px 18px;
  }
</style>
