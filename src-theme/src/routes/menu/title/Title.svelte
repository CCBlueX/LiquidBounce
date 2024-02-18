<script lang="ts">
    import Header from "../header/Header.svelte";
    import MainButton from "./buttons/MainButton.svelte";
    import ChildButton from "./buttons/ChildButton.svelte";
    import ButtonContainer from "../common/buttons/ButtonContainer.svelte";
    import IconTextButton from "../common/buttons/IconTextButton.svelte";
    import IconButton from "../common/buttons/IconButton.svelte";
    import {onMount} from "svelte";
    import {getSession, browse, exitClient, openScreen} from "../../../integration/rest";
    import type {Session} from "../../../integration/types";

    let session: Session | null = null;

    onMount(async () => {
        session = await getSession();
    });
</script>

<div class="title">
    {#if session}
        <Header username={session.username} avatar={session.avatar} premium={session.premium}/>
    {/if}

    <div class="content">
        <div class="main-buttons">
            <MainButton title="Singleplayer" icon="singleplayer" on:click={() => openScreen("singleplayer")}/>
            <MainButton title="Multiplayer" icon="multiplayer" let:parentHovered
                        on:click={() => openScreen("multiplayer")}>
                <ChildButton title="Realms" icon="realms" {parentHovered}
                             on:click={() => openScreen("multiplayer_realms")}/>
            </MainButton>
            <MainButton title="Proxy Manager" icon="proxymanager"/>
            <MainButton title="Options" icon="options" on:click={() => openScreen("options")}/>
        </div>

        <div class="additional-buttons">
            <ButtonContainer>
                <IconTextButton icon="exit" title="Exit" on:click={exitClient}/>
            </ButtonContainer>
        </div>

        <div class="social-buttons">
            <ButtonContainer>
                <IconButton title="Forum" icon="nodebb" on:click={() => browse("https://forums.ccbluex.net")}/>
                <IconButton title="GitHub" icon="github" on:click={() => browse("https://github.com/CCBlueX")}/>
                <IconButton title="Guilded" icon="guilded"
                            on:click={() => browse("https://guilded.gg/CCBlueX?r=pmbDp7K4")}/>
                <IconButton title="Twitter" icon="twitter" on:click={() => browse("https://twitter.com/CCBlueX")}/>
                <IconButton title="YouTube" icon="youtube" on:click={() => browse("https://youtube.com/CCBlueX")}/>
                <IconTextButton title="liquidbounce.net" icon="liquidbounce.net"
                                on:click={() => browse("https://liquidbounce.net")}/>
            </ButtonContainer>
        </div>
    </div>
</div>

<style>
    .title {
        width: 100vw;
        height: 100vh;
        padding: 50px;
        position: relative;
        display: flex;
        flex-direction: column;
    }

    .content {
        flex: 1;
        display: grid;
        grid-template-areas:
            "a ."
            "b c";
        grid-template-rows: 1fr max-content;
        grid-template-columns: 1fr max-content;
    }

    .main-buttons {
        display: flex;
        flex-direction: column;
        row-gap: 25px;
        grid-area: a;
    }

    .additional-buttons {
        grid-area: b;
    }

    .social-buttons {
        grid-area: c;
    }
</style>