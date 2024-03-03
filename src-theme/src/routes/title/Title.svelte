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

    import {browse, exitClient, getLocation, getSession, getUpdate, openScreen} from "../../client/api.svelte";
    import {fade} from "svelte/transition";


    function openProxyManager() {
        openScreen("proxymanager");
    }

    function openAltManager() {
        openScreen("altmanager");
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

    function openClickGui() {
        // Redirect to
        openScreen("clickgui");
    }

    function openOptions() {
        openScreen("options").catch(console.error);
    }

    function scheduleStop() {
        exitClient();
    }

    function browseForum() {
        browse("MAINTAINER_FORUM").catch(console.error);
    }

    function browseGitHub() {
        browse("MAINTAINER_GITHUB").catch(console.error);
    }

    function browseGuilded() {
        browse("MAINTAINER_GUILDED").catch(console.error);
    }

    function browseTwitter() {
        browse("MAINTAINER_TWITTER").catch(console.error);
    }

    function browseYouTube() {
        browse("MAINTAINER_YOUTUBE").catch(console.error);
    }

    function browseWebsite() {
        browse("CLIENT_WEBSITE").catch(console.error);
    }

    let username = "Loading...";
    let faceUrl = "";
    let accountType = "";
    let location = "unknown";

    getSession().then(session => {
        username = session.username;
        faceUrl = session.avatar;

        if (session.premium) {
            accountType = "Premium";
        } else {
            accountType = "Cracked";
        }
    }).catch(console.error);

    getLocation().then(ip => {
        const country = ip.country;

        // Lowercase country code
        location = country.toLowerCase();
    }).catch(console.error);

    let updateAvailable = false;
    let newestVersion = {
        "clientVersion": "0.1.0",
        "minecraftVersion": "1.20.4",
        "date": ""
    };

    getUpdate().then(update => {
        newestVersion = update.newestVersion;
        updateAvailable = update.updateAvailable;
    }).catch(console.error);
</script>

<main transition:fade>
    <div class="scale">
        <div class="wrapper">
            {#if updateAvailable}
                <div class="update-available" on:click={browseWebsite} >
                    <p>LiquidBounce v{newestVersion.clientVersion}{#if !newestVersion.release } (dev){/if}</p>
                    <p>for Minecraft {newestVersion.minecraftVersion} has been released!</p>
                    <br>

                    <p>Release date: {newestVersion.date}</p>
                    <p>Commit: {newestVersion.commitId}</p><br>

                    <p>Go to https://liquidbounce.net/</p>
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
                <MainButton text="Client GUI" icon="customize" on:click={openClickGui}/>
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
</style>
