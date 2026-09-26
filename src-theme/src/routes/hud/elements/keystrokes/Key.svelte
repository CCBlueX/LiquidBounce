<script lang="ts">
    import {listen} from "../../../../integration/ws";
    import type {KeyEvent} from "../../../../integration/events";
    import type {MinecraftKeybind} from "../../../../integration/types";

    export let gridArea: string;
    export let key: MinecraftKeybind | undefined;

    let active = false;

    listen("key", (e: KeyEvent) => {
        if (e.key !== key?.key.translationKey) {
            return;
        }

        active = e.action === 1 || e.action === 2;
    });
</script>

<div class="key" style="grid-area: {gridArea};" class:active>
    {key?.key.localized ?? "???"}
</div>

<style lang="scss">

  .key {
    height: 50px;
    background-color: var(--keystrokes-background-color);
    color: var(--keystrokes-text-color);
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 5px;
    font-size: 14px;
    font-weight: 500;
    transition: ease box-shadow .2s;
    position: relative;
    box-shadow: inset 0 0 0 0 var(--keystrokes-active-color);
    text-align: center;

    &.active {
      box-shadow: inset 0 0 0 25px var(--keystrokes-active-color);
    }
  }
</style>
