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
        getProtocols,
        getSelectedProtocol,
        getServers,
        openScreen,
        orderServers,
        removeServer as removeServerRest,
        setSelectedProtocol
    } from "../../../integration/rest";

    import type {Protocol, Server, ServerPingedEvent} from "../../../integration/types";
    import {listen} from "../../../integration/ws";
    import TextComponent from "../common/TextComponent.svelte";
    import MenuListItemTag from "../common/menulist/MenuListItemTag.svelte";
    import SingleSelect from "../common/setting/select/SingleSelect.svelte";
    import {REST_BASE} from "../../../integration/host";
    import AddServerModal from "./AddServerModal.svelte";
    import DirectConnectModal from "./DirectConnectModal.svelte";
    import EditServerModal from "./EditServerModal.svelte";

    let onlineOnly = false;
    let searchQuery = "";
    let addServerModalVisible = false;
    let directConnectModalVisible = false;

    let editServerModalVisible = false;
    let currentEditServer: {
        server: Server,
        index: number
    } | null = null;

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
        servers = servers.map(s => s.address === server.address ? server : s);
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

    function editServer(server: Server, index: number) {
        currentEditServer = {
            server,
            index
        };
        editServerModalVisible = true;
    }
</script>

<AddServerModal bind:visible={addServerModalVisible} on:serverAdd={refreshServers}/>
{#if currentEditServer}
    <EditServerModal bind:visible={editServerModalVisible} address={currentEditServer.server.address}
                     name={currentEditServer.server.name} on:serverEdit={refreshServers} index={currentEditServer.index}
                     resourcePackPolicy={currentEditServer.server.resourcePackPolicy}/>
{/if}
<DirectConnectModal bind:visible={directConnectModalVisible}/>
<Menu>
    <OptionBar>
        <Search on:search={handleSearch}/>
        <SwitchSetting title="Online only" bind:value={onlineOnly}/>
        <SingleSelect title="Version" value={selectedProtocol.name} options={protocols.map(p => p.name)}
                      on:change={changeProtocolVersion}/>
    </OptionBar>

    <MenuList sortable={renderedServers.length === servers.length} on:sort={handleServerSort}>
        {#each renderedServers as server, index}
            <MenuListItem imageText={server.ping >= 0 ? `${server.ping}ms` : null}
                          imageTextBackgroundColor={getPingColor(server.ping)}
                          image={server.ping < 0
                            ? `${REST_BASE}/api/v1/client/resource?id=minecraft:textures/misc/unknown_server.png`
                            :`data:image/png;base64,${server.icon}`}
                          title={server.name}>
                <TextComponent slot="subtitle" fontSize={18}
                               textComponent={server.ping < 0 ? "§CCan't connect to server" : server.label}/>

                <svelte:fragment slot="tag">
                    {#if server.ping >= 0}
                        <MenuListItemTag text="{server.players.online}/{server.players.max} Players"/>
                        <MenuListItemTag text={server.version}/>
                    {/if}
                </svelte:fragment>

                <svelte:fragment slot="active-visible">
                    <MenuListItemButton title="Delete" icon="trash" on:click={() => removeServer(index)}/>
                    <MenuListItemButton title="Edit" icon="pen-2" on:click={() => editServer(server, index)}/>
                </svelte:fragment>

                <svelte:fragment slot="always-visible">
                    <MenuListItemButton title="Join" icon="play" on:click={() => connectToServer(server.address)}/>
                </svelte:fragment>
            </MenuListItem>
        {/each}
    </MenuList>

    <BottomButtonWrapper>
        <ButtonContainer>
            <IconTextButton icon="plus-circle" title="Add" on:click={() => addServerModalVisible = true}/>
            <IconTextButton icon="plane" title="Direct" on:click={() => directConnectModalVisible = true}/>
            <IconTextButton icon="refresh" title="Refresh" on:click={refreshServers}/>
        </ButtonContainer>

        <ButtonContainer>
            <IconTextButton icon="back" title="Back" on:click={() => openScreen("title")}/>
        </ButtonContainer>
    </BottomButtonWrapper>
</Menu>
