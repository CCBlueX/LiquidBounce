<script lang="ts">
    import MainButton from "./buttons/MainButton.svelte";
    import ChildButton from "./buttons/ChildButton.svelte";
    import ButtonContainer from "../common/buttons/ButtonContainer.svelte";
    import IconTextButton from "../common/buttons/IconTextButton.svelte";
    import IconButton from "../common/buttons/IconButton.svelte";
    import {browse, exitClient, openScreen} from "../../../integration/rest";
    import Menu from "../common/Menu.svelte";
    import {fly} from "svelte/transition";
    import {backIn} from "svelte/easing";
</script>

<Menu>
    <div class="content">
        <div class="main-buttons">
            <div out:fly|global={{duration: 400, x: -500, easing: backIn }} in:fly|global={{duration: 400, x: -500}}>
                <MainButton title="Singleplayer" icon="singleplayer" on:click={() => openScreen("singleplayer")}/>
            </div>
            <div out:fly|global={{duration: 400, x: -500, delay: 100, easing: backIn}} in:fly|global={{duration: 400, x: -500, delay: 100}}>
                <MainButton title="Multiplayer" icon="multiplayer" let:parentHovered
                            on:click={() => openScreen("multiplayer")}>
                    <ChildButton title="Realms" icon="realms" {parentHovered}
                                 on:click={() => openScreen("multiplayer_realms")}/>
                </MainButton>
            </div>
            <div out:fly|global={{duration: 400, x: -500, delay: 200, easing: backIn}} in:fly|global={{duration: 400, x: -500, delay: 200}}>
                <MainButton title="Proxy Manager" icon="proxymanager" on:click={() => openScreen("proxymanager")}/>
            </div>
            <div out:fly|global={{duration: 400, x: -500, delay: 300, easing: backIn}} in:fly|global={{duration: 400, x: -500, delay: 300}}>
                <MainButton title="Options" icon="options" on:click={() => openScreen("options")}/>
            </div>
        </div>

        <div class="additional-buttons" transition:fly|global={{duration: 700, y: 100}}>
            <ButtonContainer>
                <IconTextButton icon="icon-exit.svg" title="Exit" on:click={exitClient}/>
            </ButtonContainer>
        </div>

        <div class="social-buttons" transition:fly|global={{duration: 700, y: 100}}>
            <ButtonContainer>
                <IconButton title="Forum" icon="nodebb" on:click={() => browse("MAINTAINER_FORUM")}/>
                <IconButton title="GitHub" icon="github" on:click={() => browse("MAINTAINER_GITHUB")}/>
                <IconButton title="Guilded" icon="guilded" on:click={() => browse("MAINTAINER_GUILDED")}/>
                <IconButton title="Twitter" icon="twitter" on:click={() => browse("MAINTAINER_TWITTER")}/>
                <IconButton title="YouTube" icon="youtube" on:click={() => browse("MAINTAINER_YOUTUBE")}/>
                <IconTextButton title="liquidbounce.net" icon="icon-liquidbounce.net.svg"
                                on:click={() => browse("CLIENT_WEBSITE")}/>
            </ButtonContainer>
        </div>
    </div>
</Menu>

<style>
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
