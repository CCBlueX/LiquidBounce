<script lang="ts">
    import ToolTip from "../ToolTip.svelte";
    import {openScreen} from "../../../../integration/rest";

    export let username: string;
    export let avatar: string;
    export let premium: boolean;
</script>

<div class="account">
    <object data={avatar} type="image/png" class="avatar" aria-label="avatar">
        <img src="img/steve.png" alt=avatar class="avatar">
    </object>
    <div class="username">{username}</div>
    <div class="account-type">{premium ? "Premium" : "Offline"}</div>
    <button class="button-change-account" type="button" on:click={() => openScreen("altmanager")}>
        <ToolTip text="Change account" />

        <img class="icon" src="img/menu/icon-pen.svg" alt="change account">
    </button>
</div>

<style lang="scss">
  @import "../../../../colors";

  .account {
    background-color: rgba($hotbar-base-color, 0.68);
    width: 488px;
    padding: 15px 18px;
    border-radius: 5px;
    align-items: center;
    display: grid;
    grid-template-areas:
        "a b c"
        "a d c";
    grid-template-columns: max-content 1fr max-content;
    column-gap: 15px;
  }

  .avatar {
    height: 68px;
    width: 68px;
    border-radius: 50%;
    grid-area: a;
  }

  .username {
    font-weight: 600;
    color: $menu-text-color;
    font-size: 20px;
    grid-area: b;
    align-self: flex-end;
  }

  .account-type {
    font-weight: 500;
    color: $menu-text-dimmed-color;
    font-size: 20px;
    grid-area: d;
    align-self: flex-start;
  }

  .button-change-account {
    background-color: transparent;
    border: none;
    grid-area: c;
    position: relative;
    height: max-content;
    cursor: pointer;
  }
</style>