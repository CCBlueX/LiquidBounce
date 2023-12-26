<script>
    import {createEventDispatcher} from "svelte";
    import ToolTip from "../../../elements/ToolTip.svelte";

    const dispatch = createEventDispatcher();

    export let username;
    export let accountType;
    export let location;
    export let faceUrl;

    function handleAccountClick(e) {
        dispatch("altManagerClick");
    }

    function handleLocationClick(e) {
        dispatch("proxyManagerClick");
    }
</script>

<div class="account">
    <div class="icon">
        <div class="location-wrapper">
            <ToolTip text="Change location"/>
            <img on:click={handleLocationClick} class="location" src="img/flags/{location}.svg"
                 onerror="this.src='img/flags/none.svg'" alt={location}>
        </div>
        <object data={faceUrl} type="image/png" class="head">
            <img src="img/steve.png" alt="head" class="head"/>
        </object>
    </div>
    <div class="username">{username}</div>
    <div class="last-used">{accountType}</div>
    <div class="change-account" on:click={handleAccountClick}>
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
        width: 21px;
        height: 21px;
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

    .head {
        height: 68px;
        width: 68px;
        border-radius: 50%;
    }
</style>
