<script lang="ts">
    import {listen} from "../../../../integration/ws";
    import type {KeyBindingCPSEvent, KeyBindingEvent} from "../../../../integration/events";
    import type {MinecraftKeybind} from "../../../../integration/types";

    // export let gridArea: string;
    export let flexBasis: string = '50px';
    export let key: MinecraftKeybind | undefined;
    export let showCPS: boolean = false;

    let active = false;

    let cps = 0;

    listen("keybinding", (e: KeyBindingEvent) => {
        if (e.key.name !== key?.key.translationKey) {
            return;
        }

        active = e.action === 1 || e.action === 2;
    });

    if (showCPS) {
        listen("keybindingCPS", (e: KeyBindingCPSEvent) => {
            if (e.key.name !== key?.key.translationKey) {
                return;
            }

            cps = e.cps;
        });
    }
</script>

<div class="key" style="flex-basis: {flexBasis};" class:active>
    <span>{key?.key.localized ?? "???"}</span>
    {#if showCPS}
        <span>CPS: {cps}</span>
    {/if}
</div>

<style lang="scss">
  @import "../../../../colors.scss";

  .key {
    height: 50px;
    background-color: rgba($keystrokes-base-color, .68);
    color: $keystrokes-text-color;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    border-radius: 5px;
    font-size: 14px;
    font-weight: 500;
    transition: ease box-shadow .2s;
    position: relative;
    box-shadow: inset 0 0 0 0 $accent-color;
    text-align: center;

    &.active {
      box-shadow: inset 0 0 0 25px $accent-color;
    }
  }
</style>
