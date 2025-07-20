<script lang="ts">
    import OptionBar from "../common/optionbar/OptionBar.svelte";
    import MenuList from "../common/menulist/MenuList.svelte";
    import BottomButtonWrapper from "../common/buttons/BottomButtonWrapper.svelte";
    import ButtonContainer from "../common/buttons/ButtonContainer.svelte";
    import IconTextButton from "../common/buttons/IconTextButton.svelte";
    import Menu from "../common/Menu.svelte";
    import Search from "../common/Search.svelte";
    import MenuListItem from "../common/menulist/MenuListItem.svelte";
    import MenuListItemButton from "../common/menulist/MenuListItemButton.svelte";
    import {onMount} from "svelte";
    import {
        browse,
        connectToServer,
        getClientInfo,
        getModule,
        getProtocols,
        getSelectedProtocol,
        getServers,
        getSpooferSettings,
        openScreen,
        orderServers,
        removeServer as removeServerRest,
        setModuleEnabled,
        setSelectedProtocol,
        setSpooferSettings
    } from "../../../integration/rest";
    import type {ClientInfo, ConfigurableSetting, Protocol, Server} from "../../../integration/types";
    import {listen} from "../../../integration/ws";
    import TextComponent from "../common/TextComponent.svelte";
    import MenuListItemTag from "../common/menulist/MenuListItemTag.svelte";
    import SingleSelect from "../common/setting/select/SingleSelect.svelte";
    import {REST_BASE} from "../../../integration/host";
    import AddServerModal from "./AddServerModal.svelte";
    import DirectConnectModal from "./DirectConnectModal.svelte";
    import EditServerModal from "./EditServerModal.svelte";
    import type {ServerPingedEvent} from "../../../integration/events";
    import ButtonSetting from "../common/setting/ButtonSetting.svelte";
    import Divider from "../common/optionbar/Divider.svelte";
    import WrappedSetting from "../common/setting/WrappedSetting.svelte";
    import SwitchSetting from "../common/setting/SwitchSetting.svelte";

    let onlineOnly = false;
    let searchQuery = "";
    let addServerModalVisible = false;
    let directConnectModalVisible = false;

    let editServerModalVisible = false;
    let currentEditServer: Server | null = null;

    $: {
        let filteredServers = servers;
        if (onlineOnly) {
            filteredServers = filteredServers.filter(s => s.ping > 0);
        }
        if (searchQuery) {
            filteredServers = filteredServers.filter(s => s.name.toLowerCase().includes(searchQuery.toLowerCase()));
        }
        renderedServers = filteredServers;
    }

    let clientInfo: ClientInfo | null = null;
    let autoConfig = false;
    let spooferConfigurable: ConfigurableSetting | null = null;
    let servers: Server[] = [];
    let renderedServers: Server[] = [];
    let protocols: Protocol[] = [];
    let selectedProtocol: Protocol = {
        name: "",
        version: -1
    };

    // The amount of times the server list has been sorted.
    // It is only used in the key-block below to cause a full re-render after the server have been sorted.
    // This is necessary because LiquidBounce references servers by their index (the id).
    // The id does not change when the element is being sorted.
    // I'm not keying on 'servers' because I don't want to re-render the entire list every time a ping event is received.
    // This is a hack and there should be a better solution.
    let timesSorted = 0;

    onMount(async () => {
        clientInfo = await getClientInfo();
        spooferConfigurable = await getSpooferSettings();
        autoConfig = (await getModule("AutoConfig")).enabled;
        await refreshServers();
        renderedServers = servers;
        protocols = await getProtocols();
        selectedProtocol = await getSelectedProtocol();
    });

    listen("serverPinged", (pingedEvent: ServerPingedEvent) => {
        const server = pingedEvent.server;
        servers = servers.map((s) => {
            if (s.address === server.address) {
                const clone = structuredClone(server);
                clone.id = s.id;
                clone.name = s.name;
                clone.resourcePackPolicy = s.resourcePackPolicy;
                return clone;
            } else {
                return s;
            }
        });
    });

    async function refreshServers() {
        servers = await getServers();
    }

    async function removeServer(index: number) {
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

    async function handleServerSort(e: CustomEvent<{ newOrder: number[] }>) {
        await orderServers(e.detail.newOrder);
        await refreshServers();
        renderedServers = servers;
        timesSorted++; // See declaration
    }

    function handleSearch(e: CustomEvent<{ query: string }>) {
        searchQuery = e.detail.query;
    }

    function editServer(server: Server) {
        currentEditServer = server;
        editServerModalVisible = true;
    }

    async function updateSpooferSettings() {
        if (!spooferConfigurable) {
            return;
        }

        await setSpooferSettings(spooferConfigurable);
        spooferConfigurable = await getSpooferSettings();
    }

    async function updateAutoConfigState() {
        await setModuleEnabled("AutoConfig", autoConfig);
    }
</script>

<AddServerModal bind:visible={addServerModalVisible} on:serverAdd={refreshServers}/>
{#if currentEditServer}
    <EditServerModal bind:visible={editServerModalVisible} address={currentEditServer.address}
                     name={currentEditServer.name} on:serverEdit={refreshServers} id={currentEditServer.id}
                     resourcePackPolicy={currentEditServer.resourcePackPolicy}/>
{/if}
<DirectConnectModal bind:visible={directConnectModalVisible}/>
<Menu>
    <OptionBar>
        <Search on:search={handleSearch}/>

        <SwitchSetting title="Online only" bind:value={onlineOnly}/>
        <Divider/>
        <SwitchSetting title="Auto Config" bind:value={autoConfig} on:change={updateAutoConfigState}/>
        {#if spooferConfigurable}
            <WrappedSetting bind:value={spooferConfigurable} on:change={updateSpooferSettings} path="multiplayer.spoofer"/>
        {/if}
        {#if clientInfo && clientInfo.viaFabricPlus}
            <SingleSelect title="Version" value={selectedProtocol.name} options={protocols.map(p => p.name)}
                          on:change={changeProtocolVersion}/>
            <ButtonSetting title="ViaFabricPlus" on:click={() => openScreen("viafabricplus_protocol_selection")}/>
        {:else}
            <ButtonSetting title="Install ViaFabricPlus" on:click={() => browse("VIAFABRICPLUS")}/>
        {/if}
    </OptionBar>

    <MenuList sortable={renderedServers.length === servers.length} elementCount={servers.length}
              on:sort={handleServerSort}>
        {#key timesSorted}
            {#each renderedServers as server}
                <MenuListItem imageText={server.ping > 0 ? `${server.ping}ms` : null}
                              imageTextBackgroundColor={getPingColor(server.ping)}
                              image={server.ping < 0 || !server.icon
                            ? `${REST_BASE}/api/v1/client/resource?id=minecraft:textures/misc/unknown_server.png`
                            :`data:image/png;base64,${server.icon}`}
                              title={server.name}
                              on:dblclick={() => connectToServer(server.address)}>
                    <TextComponent allowPreformatting={true} preFormattingMonospace={false} slot="subtitle"
                                   fontSize={18}
                                   textComponent={server.ping <= 0 ? "§CCan't connect to server" : server.label}/>

                    <svelte:fragment slot="tag">
                        {#if server.ping > 0}
                            <MenuListItemTag text="{server.players.online}/{server.players.max} Players"/>
                            <MenuListItemTag text={server.version}/>
                        {/if}
                    </svelte:fragment>

                    <svelte:fragment slot="active-visible">
                        <MenuListItemButton title="Remove" icon="trash" on:click={() => removeServer(server.id)}/>
                        <MenuListItemButton title="Edit" icon="pen-2" on:click={() => editServer(server)}/>
                    </svelte:fragment>

                    <svelte:fragment slot="always-visible">
                        <MenuListItemButton title="Join" icon="play" on:click={() => connectToServer(server.address)}/>
                    </svelte:fragment>
                </MenuListItem>
            {/each}
        {/key}
    </MenuList>

    <BottomButtonWrapper>
        <ButtonContainer>
            <IconTextButton icon="icon-plus-circle.svg" title="Add" on:click={() => addServerModalVisible = true}/>
            <IconTextButton icon="icon-plane.svg" title="Direct" on:click={() => directConnectModalVisible = true}/>
            <IconTextButton icon="icon-refresh.svg" title="Refresh" on:click={refreshServers}/>
        </ButtonContainer>

        <ButtonContainer>
            <IconTextButton icon="icon-back.svg" title="Back" on:click={() => openScreen("title")}/>
        </ButtonContainer>
    </BottomButtonWrapper>
</Menu>
