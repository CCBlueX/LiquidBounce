<script lang="ts">
    import OptionBar from "../common/OptionBar.svelte";
    import MenuList from "../common/menulist/MenuList.svelte";
    import BottomButtonWrapper from "../common/buttons/BottomButtonWrapper.svelte";
    import ButtonContainer from "../common/buttons/ButtonContainer.svelte";
    import IconTextButton from "../common/buttons/IconTextButton.svelte";
    import Menu from "../common/Menu.svelte";
    import Search from "../common/Search.svelte";
    import SwitchSetting from "../common/setting/SwitchSetting.svelte";
    import MenuListItem from "../common/menulist/MenuListItem.svelte";
    import MenuListItemButton from "../common/menulist/MenuListItemButton.svelte";
    import {onMount} from "svelte";
    import {connectToServer, getServers, openScreen} from "../../../integration/rest";
    import type {Server, ServerPingedEvent} from "../../../integration/types";
    import {listen} from "../../../integration/ws";
    import TextComponent from "../common/TextComponent.svelte";
    import MenuListItemTag from "../common/menulist/MenuListItemTag.svelte";

    let servers: Server[] = [];

    onMount(async () => {
        servers = await getServers();
    });

    listen("serverPinged", (pingedEvent: ServerPingedEvent) => {
        const server = pingedEvent.server;
        const index = servers.findIndex(s => s.address === server.address);
        if (index !== -1) {
            servers[index] = server;
        }
    });

    function getPingColor(ping: number) {
        if (ping <= 50) {
            return "#2DCC70";
        } else if (ping <= 100) {
            return "#F1C40F";
        } else {
            return "#E84C3D";
        }
    }
</script>

<Menu>
    <OptionBar>
        <Search/>
        <SwitchSetting title="Favorites only" value={true}/>
        <SwitchSetting title="Current version" value={false}/>
        <SwitchSetting title="Online" value={true}/>
    </OptionBar>

    <MenuList>
        {#each servers as {name, icon, address, label, players, version, ping}}
            <MenuListItem imageText="{ping}ms" imageTextBackgroundColor={getPingColor(ping)} image="data:image/png;base64,{icon}"
                          title={name} on:doubleClick={() => connectToServer(address)}>
                <TextComponent slot="subtitle" textComponent={label}/>

                <svelte:fragment slot="tag">
                    <MenuListItemTag text="{players.online}/{players.max} Players"/>
                    <MenuListItemTag text={version}/>
                </svelte:fragment>

                <svelte:fragment slot="active-visible">
                    <MenuListItemButton title="Delete" icon="trash"/>
                    <MenuListItemButton title="Favorite" icon="star"/>
                    <MenuListItemButton title="Edit" icon="pen-2"/>
                </svelte:fragment>

                <svelte:fragment slot="always-visible">
                    <MenuListItemButton title="Join" icon="play" on:click={() => connectToServer(address)}/>
                </svelte:fragment>
            </MenuListItem>
        {/each}
    </MenuList>

    <BottomButtonWrapper>
        <ButtonContainer>
            <IconTextButton icon="exit" title="Add"/>
            <IconTextButton icon="exit" title="Direct"/>
            <IconTextButton icon="exit" title="Refresh"/>
        </ButtonContainer>

        <ButtonContainer>
            <IconTextButton icon="exit" title="Back" on:click={() => openScreen("title")}/>
        </ButtonContainer>
    </BottomButtonWrapper>
</Menu>
