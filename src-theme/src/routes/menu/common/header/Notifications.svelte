<script lang="ts">
    import {listen} from "../../../../integration/ws";
    import {fly} from "svelte/transition";

    let visible = false;
    let title = "";
    let message = "";
    let error = false;

    let hideTimeout: number | null = null;

    function show(t: string, m: string) {
        title = t;
        message = m;
        visible = true;
        setTimeout(() => {
            visible = false;
        }, 3000)
    }

    listen("accountManagerAddition", (e: any) => {
        error = !!e.error;
        if (!e.error) {
            show("AltManager", `Successfully added account ${e.username}`);
        } else {
            show("AltManager", e.error);
        }
    });

    listen("accountManagerMessage", (e: any) => {
        show("AltManager", e.message);
    });

    listen("accountManagerLogin", (e: any) => {
        error = !!e.error;
        if (!e.error) {
            show("AltManager", `Successfully logged in to account ${e.username}`);
        } else {
            show("AltManager", e.error);
        }
    });
</script>

<div class="notifications">
    {#if visible}
        <div class="notification" transition:fly|global={{duration: 500, y: -100}}>
            <div class="icon" class:error>
                <img src="img/hud/notification/icon-info.svg" alt="info">
            </div>
            <div class="title">{title}</div>
            <div class="message">{message}</div>
        </div>
    {/if}
</div>

<style lang="scss">
  @import "../../../../colors.scss";

  .notification {
    background-color: rgba($menu-base-color, 0.68);
    border-radius: 5px;
    display: grid;
    grid-template-areas:
        "a b"
        "a c";
    overflow: hidden;
    padding-right: 10px;

    .title {
      color: $menu-text-color;
      font-weight: 600;
      font-size: 18px;
      grid-area: b;
      align-self: flex-end;
    }

    .message {
      color: $menu-text-dimmed-color;
      font-weight: 500;
      grid-area: c;
    }

    .icon {
      grid-area: a;
      height: 65px;
      width: 65px;
      background-color: $accent-color;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 10px;

      &.error {
        background-color: $menu-error-color;
      }
    }
  }
</style>