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
</script>

<Menu>
    <OptionBar>
        <Search/>
        <SwitchSetting title="Favorites only" value={true}/>
        <SwitchSetting title="Current version" value={false}/>
        <SwitchSetting title="Online" value={true}/>
    </OptionBar>

    <MenuList>
        {#each servers as {name, icon, address}}
            <MenuListItem imageText="15ms" imageTextBackgroundColor="#4DAC68" image="data:image/png;base64,{icon}" titleTag="30534 Players" title={name} subtitle="Not implemented">
                <div slot="active-visible">
                    <MenuListItemButton title="Delete" icon="trash" />
                    <MenuListItemButton title="Favorite" icon="star" />
                    <MenuListItemButton title="Edit" icon="pen-2" />
                </div>
                <div slot="always-visible">
                    <MenuListItemButton title="Join" icon="play" on:click={() => connectToServer(address)} />
                </div>
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
