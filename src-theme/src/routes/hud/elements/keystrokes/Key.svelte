<script lang="ts">
    import {listen} from "../../../../integration/ws";
    import type {KeyEvent} from "../../../../integration/events";

    export let gridArea: string;
    export let key: string;

    let active = false;

    listen("key", (e: KeyEvent) => {
        console.log(e);
        if (e.key.name.split(".").pop() !== key.toLowerCase()) {
            return;
        }

        active = e.action === 1 || e.action === 2;
    });
</script>

<div class="key" style="grid-area: {gridArea};" class:active>
    {key}
</div>

<style lang="scss">
  @import "../../../../colors.scss";

  .key {
    height: 50px;
    background-color: rgba($keystrokes-base-color, .68);
    color: $keystrokes-text-color;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 5px;
    font-size: 14px;
    font-weight: 500;
    transition: ease box-shadow .2s;
    position: relative;
    box-shadow: inset 0 0 0 0 $accent-color;

    &.active {
      box-shadow: inset 0 0 0 25px $accent-color;
    }
  }
</style>