<script lang="ts">
    import MainButton from "./buttons/MainButton.svelte";
    import ButtonContainer from "../common/buttons/ButtonContainer.svelte";
    import IconTextButton from "../common/buttons/IconTextButton.svelte";
    import IconButton from "../common/buttons/IconButton.svelte";
    import {browse, getClientRelease, openScreen} from "../../../integration/rest";
    import Menu from "../common/Menu.svelte";
    import {fly} from "svelte/transition";
    import {onMount} from "svelte";
    import {notification} from "../common/header/notification_store";

    let regularButtonsShown = true;
    let clientButtonsShown = false;

    onMount(() => {
        setTimeout(async () => {
            const release = await getClientRelease();

            if (release && !release.error) {
                notification.set({
                    title: `JMcomicFix ${release.tagName} version has been released!`,
                    message: `Click to copy the download link.`,
                    url: release.downloadUrl,
                    error: false,
                    delay: 50
                });
            }
        }, 2000);
    });

    function toggleButtons() {
        if (clientButtonsShown) {
            clientButtonsShown = false;
            setTimeout(() => {
                regularButtonsShown = true;
            }, 750);
        } else {
            regularButtonsShown = false;
            setTimeout(() => {
                clientButtonsShown = true;
            }, 750);
        }
    }

</script>


<Menu>
    <div class="content">
        <div class="main-buttons">
            {#if regularButtonsShown}
                <MainButton title="Singleplayer" icon="singleplayer" index={0}
                            on:click={() => openScreen("singleplayer")}/>
                <MainButton title="Multiplayer" icon="multiplayer" index={1}
                            on:click={() => openScreen("multiplayer")}/>
                <MainButton title="LiquidBounce" icon="liquidbounce" index={2} on:click={toggleButtons}/>
                <MainButton title="Options" icon="options" index={3} on:click={() => openScreen("options")}/>
            {:else if clientButtonsShown}
                <MainButton title="Proxy Manager" icon="proxymanager" index={0}
                            on:click={() => openScreen("proxymanager")}/>
                <MainButton title="Click GUI" icon="clickgui" index={1} on:click={() => openScreen("clickgui")}/>
                <MainButton title="HUD" icon="layout" index={2} on:click={() => openScreen("layouteditor")}/>
                <MainButton title="Back" icon="back-large" index={3} on:click={toggleButtons}/>
            {/if}
        </div>
        <div class="additional-buttons" transition:fly|global={{duration: 700, y: 100}}>
        </div>
        <div class="social-buttons" transition:fly|global={{duration:300, y:100}}>
            <ButtonContainer>
                <IconButton icon="nodebb" on:click={() => browse("MAINTAINER_FORUM")} title="Forum"/>
                <IconButton icon="github" on:click={() => browse("MAINTAINER_GITHUB")} title="GitHub"/>
                <IconButton icon="discord" on:click={() => browse("MAINTAINER_DISCORD")} title="Discord"/>
                <IconButton icon="twitter" on:click={() => browse("MAINTAINER_TWITTER")} title="Twitter"/>
                <IconButton icon="youtube" on:click={() => browse("MAINTAINER_YOUTUBE")} title="YouTube"/>
                <IconButton icon="bili" on:click={() => browse("MAINTAINER_BILIBILI")} title="BiliBili"/>
                <IconTextButton icon="icon-liquidbounce.net.svg" on:click={() => browse("CLIENT_WEBSITE")}
                                title="DevLog"/>
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
