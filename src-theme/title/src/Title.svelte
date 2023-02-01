<script>
    import Account from "./elements/Account.svelte";
    import Logo from "../../shared_res/src/menus/Logo.svelte";
    import ChildButton from "../../shared_res/src/menus/buttons/ChildButton.svelte";
    import MainButton from "./elements/buttons/MainButton.svelte";
    import MainButtons from "./elements/buttons/MainButtons.svelte";
    import IconButton from "../../shared_res/src/menus/buttons/IconButton.svelte";
    import IconTextButton from "../../shared_res/src/menus/buttons/IconTextButton.svelte";
    import ButtonWrapperLeft from "../../shared_res/src/menus/buttons/ButtonWrapperLeft.svelte";
    import ButtonWrapperRight from "../../shared_res/src/menus/buttons/ButtonWrapperRight.svelte";

    function openProxyManager() {
        ui.open("proxymanager", screen);
    }

    function openAltManager() {
        ui.open("altmanager", screen);
    }

    function openSingleplayer() {
        ui.open("singleplayer", screen);
    }

    function openMultiplayer() {
        ui.open("multiplayer", screen);
    }

    function openRealms() {
        ui.open("multiplayer_realms", screen);
    }

    function openOptions() {
        ui.open("options", screen);
    }

    function scheduleStop() {
        client.exitClient();
    }

    function browseForum() {
        utils.browse("https://forums.ccbluex.net"); 
    }

    function browseGitHub() {
        utils.browse("https://github.com/CCBlueX");  
    }

    function browseGuilded() {
        utils.browse("https://guilded.gg/CCBlueX?r=pmbDp7K4");
    }

    function browseTwitter() {
        utils.browse("https://twitter.com/CCBlueX");
    }

    function browseYouTube() {
        utils.browse("https://youtube.com/CCBlueX");
    }

    function browseWebsite() {
        utils.browse("https://liquidbounce.net");
    }

    const username = client.getSessionService().getUsername();
    const faceUrl = client.getSessionService().getFaceUrl();
    const lastUsed = client.getSessionService().getLastUsed();

    const location = client.getSessionService().getLocation();
</script>

<main>
    <div class="scale">
        <div class="wrapper">
            <Logo />
            <Account username={username} location={location} faceUrl={faceUrl} lastUsed={lastUsed} on:proxyManagerClick={openProxyManager} on:altManagerClick={openAltManager} />
            <MainButtons>
                <MainButton text="Singleplayer" icon="singleplayer" on:click={openSingleplayer} />
                <MainButton text="Multiplayer" icon="multiplayer" on:click={openMultiplayer} let:hovered>
                    <ChildButton text="Realms" icon="realms" {hovered} on:click={openRealms} />
                </MainButton>
                <MainButton text="Customize" icon="customize" />
                <MainButton text="Options" icon="options" on:click={openOptions} />
            </MainButtons>

            <ButtonWrapperLeft>
                <IconTextButton text="Change Background" icon="change-background" />
                <IconTextButton text="Exit" icon="exit" on:click={scheduleStop} />
            </ButtonWrapperLeft>


            <ButtonWrapperRight>
                <IconButton text="Forum" icon="nodebb" on:click={browseForum} />
                <IconButton text="GitHub" icon="github" on:click={browseGitHub} />
                <IconButton text="Guilded" icon="guilded" on:click={browseGuilded} />
                <IconButton text="Twitter" icon="twitter" on:click={browseTwitter} />
                <IconButton text="YouTube" icon="youtube" on:click={browseYouTube} />
                <IconTextButton text="liquidbounce.net" icon="liquidbounce.net" on:click={browseWebsite} />
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
