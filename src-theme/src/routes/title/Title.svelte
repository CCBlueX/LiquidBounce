<script>
    import Account from "./elements/Account.svelte";
    import Logo from "../../elements/Logo.svelte";
    import ChildButton from "../../elements/buttons/ChildButton.svelte";
    import MainButton from "./elements/buttons/MainButton.svelte";
    import MainButtons from "./elements/buttons/MainButtons.svelte";
    import IconButton from "../../elements/buttons/IconButton.svelte";
    import IconTextButton from "../../elements/buttons/IconTextButton.svelte";
    import ButtonWrapperLeft from "../../elements/buttons/ButtonWrapperLeft.svelte";
    import ButtonWrapperRight from "../../elements/buttons/ButtonWrapperRight.svelte";

    import { getSession, getLocation, openScreen, browse, exitClient, getUpdate } from "../../client/api.svelte";
    import { push } from "svelte-spa-router";
    import {fade} from "svelte/transition";


    function openProxyManager() {
        push("/proxymanager");
    }

    function openAltManager() {
        push("/altmanager");
    }

    function openSingleplayer() {
        openScreen("singleplayer").catch(console.error);
    }

    function openMultiplayer() {
        openScreen("multiplayer").catch(console.error);
    }

    function openRealms() {
        openScreen("multiplayer_realms").catch(console.error);
    }

    function openCustomize() {
        // Redirect to
        push("/customize");
    }

    function openOptions() {
        openScreen("options").catch(console.error);
    }

    function scheduleStop() {
        exitClient();
    }

    function browseForum() {
        browse("https://forums.ccbluex.net").catch(console.error);
    }

    function browseGitHub() {
        browse("https://github.com/CCBlueX").catch(console.error);
    }

    function browseGuilded() {
        browse("https://guilded.gg/CCBlueX?r=pmbDp7K4").catch(console.error);
    }

    function browseTwitter() {
        browse("https://twitter.com/CCBlueX").catch(console.error);
    }

    function browseYouTube() {
        browse("https://youtube.com/CCBlueX").catch(console.error);
    }

    function browseWebsite() {
        browse("https://liquidbounce.net").catch(console.error);
    }

    let username = "Loading...";
    let faceUrl = "";
    let accountType = "";
    let location = "unknown";

    getSession().then(session => {
        username = session.username;
        faceUrl = session.avatar;
        accountType = session.accountType;
    }).catch(console.error);

    getLocation().then(ip => {
        const country = ip.country;

        // Lowercase country code
        location = country.toLowerCase();
    }).catch(console.error);

    let updateAvailable = false;
    getUpdate().then(update => {
        updateAvailable = update.updateAvailable;
    }).catch(console.error);
</script>

<main transition:fade>
    <div class="scale">
        <div class="wrapper">
            {#if updateAvailable}
                <div class="update-available">
                    <span on:click={browseWebsite}>Update available! Go to https://liquidbounce.net/</span>
                </div>
            {/if}

            <Logo/>
            <Account username={username} location={location} faceUrl={faceUrl} accountType={accountType}
                     on:proxyManagerClick={openProxyManager} on:altManagerClick={openAltManager}/>
            <MainButtons>
                <MainButton text="Singleplayer" icon="singleplayer" on:click={openSingleplayer}/>
                <MainButton text="Multiplayer" icon="multiplayer" on:click={openMultiplayer} let:hovered>
                    <ChildButton text="Realms" icon="realms" {hovered} on:click={openRealms}/>
                </MainButton>
                <MainButton text="Customize" icon="customize" on:click={openCustomize}/>
                <MainButton text="Options" icon="options" on:click={openOptions}/>
            </MainButtons>

            <ButtonWrapperLeft>
                <IconTextButton text="Change Background" icon="change-background"/>
                <IconTextButton text="Exit" icon="exit" on:click={scheduleStop}/>
            </ButtonWrapperLeft>

            <ButtonWrapperRight>
                <IconButton text="Forum" icon="nodebb" on:click={browseForum}/>
                <IconButton text="GitHub" icon="github" on:click={browseGitHub}/>
                <IconButton text="Guilded" icon="guilded" on:click={browseGuilded}/>
                <IconButton text="Twitter" icon="twitter" on:click={browseTwitter}/>
                <IconButton text="YouTube" icon="youtube" on:click={browseYouTube}/>
                <IconTextButton text="liquidbounce.net" icon="liquidbounce.net" on:click={browseWebsite}/>
            </ButtonWrapperRight>
        </div>
    </div>
</main>

<style>
    main {
        height: 100vh;
        width: 100vw;
        background-image: url("../img/background.png");
        background-size: cover;
        -webkit-user-select: none;
        cursor: default !important;
    }

    .update-available {
        position: absolute;
        left: 0;
        right: 0;
        margin-left: auto;
        margin-right: auto;
        width: 420px;
        border-radius: 6px;
        padding: 10px;

        cursor: pointer;
        background-color: rgba(0, 0, 0, .68);
        color: white;
    }

    .wrapper {
        position: relative;
        height: 100%;
    }

    .scale {
        position: relative;
        height: 100%;
        padding: 50px;
    }

    @media screen and (max-width: 1366px) {
        .scale {
            zoom: .7;
        }
    }

    @media screen and (max-width: 1024px) {
        .scale {
            zoom: .5;
        }
    }

    @media screen and (max-height: 1000px) {
        .scale {
            zoom: .7;
        }
    }

    @media screen and (max-height: 700px) {
        .scale {
            zoom: .5;
        }
    }

    @media screen and (max-height: 540px) {
        .scale {
            zoom: .4;
        }
    }
</style>
