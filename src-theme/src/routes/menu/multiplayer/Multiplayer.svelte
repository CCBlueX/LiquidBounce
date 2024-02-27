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
    import {
        connectToServer,
        getServers,
        openScreen,
        getProtocols,
        getSelectedProtocol,
        setSelectedProtocol,
        orderServers,
        removeServer as removeServerRest
    } from "../../../integration/rest";
    import type {Protocol, Server, ServerPingedEvent} from "../../../integration/types";
    import {listen} from "../../../integration/ws";
    import TextComponent from "../common/TextComponent.svelte";
    import MenuListItemTag from "../common/menulist/MenuListItemTag.svelte";
    import SingleSelect from "../common/select/SingleSelect.svelte";
    import {REST_BASE} from "../../../integration/host";

    let onlineOnly = false;
    let searchQuery = "";

    $: {
        let filteredServers = servers;
        if (onlineOnly) {
            filteredServers = filteredServers.filter(s => s.ping >= 0);
        }
        if (searchQuery) {
            filteredServers = filteredServers.filter(s => s.name.toLowerCase().includes(searchQuery.toLowerCase()));
        }
        renderedServers = filteredServers;
    }

    let servers: Server[] = [];
    let renderedServers: Server[] = [];
    let protocols: Protocol[] = [];
    let selectedProtocol: Protocol = {
        name: "",
        version: -1
    };

    function calculateNewOrder(oldIndex: number, newIndex: number, length: number): number[] {
        const a = Array.from({length}, (x, i) => i);
        a.splice(oldIndex, 1);
        a.splice(newIndex, 0, oldIndex)
        return a;
    }

    onMount(async () => {
        servers = await getServers();
        renderedServers = servers;
        protocols = await getProtocols();
        selectedProtocol = await getSelectedProtocol();
    });

    listen("serverPinged", (pingedEvent: ServerPingedEvent) => {
        const server = pingedEvent.server;
        const index = servers.findIndex(s => s.address === server.address);
        if (index !== -1) {
            servers[index] = server;
        }
    });

    async function refreshServers() {
        servers = await getServers();
    }

    async function removeServer(index: number) {
        await refreshServers();
        await removeServerRest(index);
        await refreshServers();
    }

    function getPingColor(ping: number) {
        if (ping < 0) {
            return "#E84C3D";
        }

        if (ping <= 50) {
            return "#2DCC70";
        } else if (ping <= 100) {
            return "#F1C40F";
        } else {
            return "#E84C3D";
        }
    }

    async function changeProtocolVersion(e: CustomEvent<{ value: string }>) {
        const p = protocols.find(p => p.name == e.detail.value);
        if (!p) {
            return;
        }

        await setSelectedProtocol(p);
        selectedProtocol = await getSelectedProtocol();
    }

    async function handleServerSort(e: CustomEvent<{ oldIndex: number, newIndex: number }>) {
        await orderServers(calculateNewOrder(e.detail.oldIndex, e.detail.newIndex, servers.length));
    }

    function handleSearch(e: CustomEvent<{ query: string }>) {
        searchQuery = e.detail.query;
    }
</script>

<Menu>
    <OptionBar>
        <Search on:search={handleSearch}/>
        <SwitchSetting title="Online" bind:value={onlineOnly}/>
        <SingleSelect value={selectedProtocol.name} options={protocols.map(p => p.name)}
                      on:change={changeProtocolVersion}/>
    </OptionBar>

    <MenuList sortable={renderedServers.length === servers.length} on:sort={handleServerSort}>
        {#each renderedServers as {name, icon, address, label, players, version, ping}, index}
            <MenuListItem imageText={ping >= 0 ? `${ping}ms` : null} imageTextBackgroundColor={getPingColor(ping)}
                          image={ping < 0
                            ? `${REST_BASE}/api/v1/client/resource?id=minecraft:textures/misc/unknown_server.png`
                            :`data:image/png;base64,${icon}`}
                          title={name}>
                <TextComponent slot="subtitle" textComponent={ping < 0 ? "§CCan't connect to server" : label}/>

                <svelte:fragment slot="tag">
                    {#if ping >= 0}
                        <MenuListItemTag text="{players.online}/{players.max} Players"/>
                        <MenuListItemTag text={version}/>
                    {/if}
                </svelte:fragment>

                <svelte:fragment slot="active-visible">
                    <MenuListItemButton title="Delete" icon="trash" on:click={() => removeServer(index)}/>
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
            <IconTextButton icon="plus-circle" title="Add"/>
            <IconTextButton icon="plane" title="Direct"/>
            <IconTextButton icon="refresh" title="Refresh" on:click={refreshServers}/>
        </ButtonContainer>

        <ButtonContainer>
            <IconTextButton icon="back" title="Back" on:click={() => openScreen("title")}/>
        </ButtonContainer>
    </BottomButtonWrapper>
</Menu>
