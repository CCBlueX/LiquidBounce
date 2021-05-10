<script>
    import { createEventDispatcher } from "svelte";

    const dispatch = createEventDispatcher();

    import ToolTip from "../elements/ToolTip.svelte";

    export let username;
    export let lastUsed;
    export let location;

    function handleAccountClick(e) {
        dispatch("altManagerClick");
    }

    function handleLocationClick(e) {
        dispatch("altManagerClick");
    }
</script>

<div class="account" on:click={handleAccountClick}>
    <div class="icon">
        <div class="location-wrapper">
            <ToolTip text="Change location" />
            <img on:click={handleLocationClick} class="location" src="img/flags/{location}.svg" alt={location}>
        </div>
        <img class="head" src="https://crafatar.com/avatars/3aa1c8378f4948e9ab4d7a7c36679748" alt="head">
    </div>
    <div class="username">{username}</div>
    <div class="last-used">{lastUsed}</div>
    <div class="change-account">
        <img src="img/icons/pen.svg" alt="pen">
    </div>
</div>

<style>
    .account {
        background-color: rgba(0, 0, 0, .68);
        position: absolute;
        top: 0;
        right: 0;
        width: 488px;
        padding: 15px 18px;
        border-radius: 6px;
        display: grid;
        grid-template-areas: 
            "a b c"
            "a d c";
        grid-template-columns: max-content auto max-content;
        column-gap: 20px;
    }

    .location-wrapper {
        position: absolute;
        top: 0;
        right: 0;
    }

    .location {
        position: relative;
        top: 0;
        right: 0;
    }

    .icon {
        grid-area: a;
        display: flex;
        align-items: center;
        position: relative;
    }

    .username {
        grid-area: b;
        font-weight: bold;
        color: white;
        font-size: 20px;
        display: flex;
        align-items: flex-end;
    }

    .change-account {
        grid-area: c;
        display: flex;
        align-items: center;
    }

    .last-used {
        grid-area: d;
        font-weight: medium;
        color: #8B8D90;
        font-size: 20px;
    }

    .last-used::before {
        content: "Last Used: "
    }

    .head {
        height: 68px;
        width: 68px;
        border-radius: 50%;
    }
</style>