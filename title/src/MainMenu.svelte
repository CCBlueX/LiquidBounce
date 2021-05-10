<script>
    import Account from "./elements/Account.svelte";
    import Logo from "./elements/Logo.svelte";
    import ChildButton from "./elements/main-buttons/ChildButton.svelte";
    import MainButton from "./elements/main-buttons/MainButton.svelte";
    import MainButtons from "./elements/main-buttons/MainButtons.svelte";
    import IconButton from "./elements/secondary-buttons/IconButton.svelte";
    import IconTextButton from "./elements/secondary-buttons/IconTextButton.svelte";
    import SecondaryButtons from "./elements/secondary-buttons/SecondaryButtons.svelte";

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

        <div class="secondary-buttons left">
            <SecondaryButtons>
                <IconTextButton text="Change Background" icon="change-background" />
                <IconTextButton text="Exit" icon="exit" on:click={scheduleStop} />
            </SecondaryButtons>
        </div>

        <div class="secondary-buttons right">
            <SecondaryButtons>
                <IconButton text="Forum" icon="nodebb" />
                <IconButton text="GitHub" icon="github" />
                <IconButton text="Guilded" icon="guilded" />
                <IconButton text="Twitter" icon="twitter" />
                <IconButton text="YouTube" icon="youtube" />
                <IconTextButton text="liquidbounce.net" icon="liquidbounce.net" />
            </SecondaryButtons>
        </div>
    </div>
</main>

<style>
	main {
        height: 100vh;
        width: 100vw;
        background-image: url("../img/background.png");
        background-size: cover;
        padding: 50px;
	}

    .secondary-buttons {
        position: absolute;
        bottom: 0;
    }

    .secondary-buttons.left {
        left: 0;
    }

    .secondary-buttons.right {
        right: 0;
    }

    .wrapper {
        position: relative;
        height: 100%;
    }
</style>