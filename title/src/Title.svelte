<script>
    import Account from "./elements/Account.svelte";
    import Logo from "./elements/Logo.svelte";
    import ChildButton from "./elements/main-buttons/ChildButton.svelte";
    import MainButton from "./elements/main-buttons/MainButton.svelte";
    import MainButtons from "./elements/main-buttons/MainButtons.svelte";
    import IconButton from "./elements/secondary-buttons/IconButton.svelte";
    import IconTextButton from "./elements/secondary-buttons/IconTextButton.svelte";
    import SecondaryButtonsLeft from "./elements/secondary-buttons/SecondaryButtonsLeft.svelte";
    import SecondaryButtonsRight from "./elements/secondary-buttons/SecondaryButtonsRight.svelte";

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
        minecraft.scheduleStop()
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
</script>

<main>
    <div class="scale">
        <div class="wrapper">
            <Logo />
            <Account username="heafie" location="de" lastUsed="2021-05-07" on:proxyManagerClick={openProxyManager} on:altManagerClick={openAltManager} />
            <MainButtons>
                <MainButton text="Singleplayer" icon="singleplayer" on:click={openSingleplayer} />
                <MainButton text="Multiplayer" icon="multiplayer" on:click={openMultiplayer} let:hovered>
                    <ChildButton text="Realms" icon="realms" {hovered} on:click={openRealms} />
                </MainButton>
                <MainButton text="Customize" icon="customize" />
                <MainButton text="Options" icon="options" on:click={openOptions} />
            </MainButtons>

            <SecondaryButtonsLeft>
                <IconTextButton text="Change Background" icon="change-background" />
                <IconTextButton text="Exit" icon="exit" on:click={scheduleStop} />
            </SecondaryButtonsLeft>


            <SecondaryButtonsRight>
                <IconButton text="Forum" icon="nodebb" on:click={browseForum} />
                <IconButton text="GitHub" icon="github" on:click={browseGitHub} />
                <IconButton text="Guilded" icon="guilded" on:click={browseGuilded} />
                <IconButton text="Twitter" icon="twitter" on:click={browseTwitter} />
                <IconButton text="YouTube" icon="youtube" on:click={browseYouTube} />
                <IconTextButton text="liquidbounce.net" icon="liquidbounce.net" on:click={browseWebsite} />
            </SecondaryButtonsRight>
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