<script lang="ts">
    import {
        openScreen,
        getProxies,
        connectToProxy as connectToProxyRest,
        removeProxy as removeProxyRest,
    } from "../../../integration/rest.js";
    import BottomButtonWrapper from "../common/buttons/BottomButtonWrapper.svelte";
    import OptionBar from "../common/OptionBar.svelte";
    import MenuListItem from "../common/menulist/MenuListItem.svelte";
    import Menu from "../common/Menu.svelte";
    import ButtonContainer from "../common/buttons/ButtonContainer.svelte";
    import MenuListItemTag from "../common/menulist/MenuListItemTag.svelte";
    import MenuList from "../common/menulist/MenuList.svelte";
    import IconTextButton from "../common/buttons/IconTextButton.svelte";
    import Search from "../common/Search.svelte";
    import MenuListItemButton from "../common/menulist/MenuListItemButton.svelte";
    import type {Proxy, World} from "../../../integration/types";
    import {onMount} from "svelte";
    import {REST_BASE} from "../../../integration/host";
    import AddProxyModal from "./AddProxyModal.svelte";
    import SwitchSetting from "../common/setting/SwitchSetting.svelte";
    import MultiSelect from "../common/setting/select/MultiSelect.svelte";

    $: {
        let filteredProxies = proxies;

        if (searchQuery) {
            filteredProxies = filteredProxies.filter(p => p.host.toLowerCase().includes(searchQuery.toLowerCase()));
        }

        renderedProxies = filteredProxies;
    }

    let addProxyModalVisible = false;

    let searchQuery = "";
    let favoritesOnly = false;

    let proxies: Proxy[] = [];
    let renderedProxies = proxies;

    onMount(async () => {
        await refreshProxies();
        renderedProxies = proxies;
    });

    async function refreshProxies() {
        proxies = await getProxies();
    }

    function handleSearch(e: CustomEvent<{ query: string }>) {
        searchQuery = e.detail.query;
    }

    function handleWorldSort() {

    }

    async function removeProxy(id: number) {
        await removeProxyRest(id);
        await refreshProxies();
    }

    async function connectToProxy(id: number) {
        await connectToProxyRest(id);
        console.log("connected")
    }

    async function connectToRandomProxy() {
        const proxy = renderedProxies[Math.floor(Math.random() * renderedProxies.length)];
        await connectToProxy(proxy.id);
    }
</script>

<AddProxyModal bind:visible={addProxyModalVisible} on:proxyAdd={refreshProxies}/>
<Menu>
    <OptionBar>
        <Search on:search={handleSearch}/>
        <SwitchSetting title="Favorites Only" bind:value={favoritesOnly}/>
        <MultiSelect title="Country" options={["Germany", "USA", "Russia"]} values={["Germany", "USA", "Russia"]}/>
    </OptionBar>

    <MenuList sortable={false} on:sort={handleWorldSort}>
        {#each renderedProxies as proxy}
            <MenuListItem
                    image={`${REST_BASE}/api/v1/client/resource?id=minecraft:textures/misc/unknown_server.png`}
                    title="{proxy.host}:{proxy.port}">
                <svelte:fragment slot="subtitle">
                    <span>Germany</span>
                </svelte:fragment>

                <svelte:fragment slot="tag">
                    <MenuListItemTag text="Germany"/>
                    {#if proxy.username && proxy.password}
                        <MenuListItemTag text="Authenticated"/>
                    {/if}
                </svelte:fragment>

                <svelte:fragment slot="active-visible">
                    <MenuListItemButton title="Delete" icon="trash" on:click={() => removeProxy(proxy.id)}/>
                </svelte:fragment>

                <svelte:fragment slot="always-visible">
                    <MenuListItemButton title="Connect" icon="play" on:click={() => connectToProxy(proxy.id)}/>
                </svelte:fragment>
            </MenuListItem>
        {/each}
    </MenuList>

    <BottomButtonWrapper>
        <ButtonContainer>
            <IconTextButton icon="icon-plus-circle.svg" title="Add" on:click={() => addProxyModalVisible = true}/>
            <IconTextButton icon="icon-random.svg" title="Random" on:click={connectToRandomProxy}/>
            <IconTextButton icon="icon-random.svg" title="Check"/>
        </ButtonContainer>

        <ButtonContainer>
            <IconTextButton icon="icon-back.svg" title="Back" on:click={() => openScreen("title")}/>
        </ButtonContainer>
    </BottomButtonWrapper>
</Menu>

<style lang="scss">
  .world-name {
    font-weight: 500;
  }
</style>