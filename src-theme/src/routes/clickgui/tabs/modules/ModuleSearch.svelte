<script lang="ts">
    import {onMount} from "svelte";
    import type {ConfigurableSetting} from "../../../../integration/types";
    import type {ClickGuiValueChangeEvent} from "../../../../integration/events";
    import {getModuleSettings} from "../../../../integration/rest";
    import {listen} from "../../../../integration/ws";
    import {escapeLayer} from "../../../../integration/escape";
    import {isTextEntry} from "../../../../util/utils";
    import {modules} from "./modules.svelte";

    let input: HTMLInputElement;
    let autoFocus = true;

    const layer = escapeLayer(() => modules.query = "");

    $effect(() => {
        layer.setOpen(modules.query !== "");
    });

    function applyValues(configurable: ConfigurableSetting) {
        autoFocus = configurable.value.find(v => v.name === "SearchBarAutoFocus")?.value as boolean ?? true;
    }

    // Typing anywhere in the ClickGUI goes to the search
    function handleWindowKeyDown(event: KeyboardEvent) {
        if (!autoFocus || isTextEntry(document.activeElement) || document.querySelector("dialog[open]")) {
            return;
        }

        if (event.key.length === 1 && event.key !== " " && !event.ctrlKey && !event.metaKey && !event.altKey) {
            input.focus();
        }
    }

    onMount(async () => {
        applyValues(await getModuleSettings("ClickGUI"));
    });

    listen("clickGuiValueChange", (event: ClickGuiValueChangeEvent) => applyValues(event.configurable));
</script>

<svelte:window onkeydown={handleWindowKeyDown}/>

<label class="search">
    <img src="img/menu/icon-search.svg" alt="">
    <input type="text" placeholder="Search modules and settings" aria-label="Search modules and settings"
           spellcheck="false" bind:value={modules.query} bind:this={input}>
    {#if modules.query}
        <button type="button" class="clear" aria-label="Clear the search"
                onclick={() => { modules.query = ""; input.focus(); }}></button>
    {/if}
</label>

<style lang="scss">
  .search {
    width: 320px;
    min-width: 0;
    height: 32px;
    display: flex;
    align-items: center;
    gap: 9px;
    padding: 0 6px 0 10px;
    background-color: var(--clickgui-input-background-color);
    border-bottom: 2px solid var(--clickgui-control-border-color);
    transition: ease border-color .15s;
    cursor: text;

    &:focus-within {
      border-bottom-color: var(--clickgui-input-border-color);
    }

    img {
      flex: none;
      width: 13px;
      height: 13px;
      opacity: .8;
    }
  }

  input {
    flex: 1;
    min-width: 0;
    background: none;
    border: none;
    font-family: "Inter", sans-serif;
    font-size: 13px;
    font-weight: 500;
    color: var(--clickgui-text-color);

    &::placeholder {
      color: var(--clickgui-text-muted-color);
    }
  }

  .clear {
    flex: none;
    width: 20px;
    height: 20px;
    border: none;
    border-radius: 3px;
    background: url("/img/clickgui/icon-cross.svg") center / 9px no-repeat;
    opacity: .7;
    cursor: pointer;
    transition: ease opacity .15s, ease background-color .15s;

    &:hover, &:focus-visible {
      opacity: 1;
      background-color: var(--clickgui-control-hover-background-color);
    }
  }
</style>
