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

    function openOptions() {
        ui.open("options", screen);
    }

    function scheduleStop() {
        minecraft.scheduleStop()
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
                    <ChildButton text="Realms" icon="realms" {hovered} />
                </MainButton>
                <MainButton text="Customize" icon="customize" />
                <MainButton text="Options" icon="options" on:click={openOptions} />
            </MainButtons>

            <SecondaryButtonsLeft>
                <IconTextButton text="Change Background" icon="change-background" />
                <IconTextButton text="Exit" icon="exit" on:click={scheduleStop} />
            </SecondaryButtonsLeft>


            <SecondaryButtonsRight>
                <IconButton text="Forum" icon="nodebb" />
                <IconButton text="GitHub" icon="github" />
                <IconButton text="Guilded" icon="guilded" />
                <IconButton text="Twitter" icon="twitter" />
                <IconButton text="YouTube" icon="youtube" />
                <IconTextButton text="liquidbounce.net" icon="liquidbounce.net" />
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