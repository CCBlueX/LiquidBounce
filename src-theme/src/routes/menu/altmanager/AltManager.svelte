<script lang="ts">
    import {
        getAccounts,
        loginToAccount,
        openScreen,
        restoreSession,
        setAccountFavorite,
        removeAccount as restRemoveAccount
    } from "../../../integration/rest.js";
    import BottomButtonWrapper from "../common/buttons/BottomButtonWrapper.svelte";
    import SwitchSetting from "../common/setting/SwitchSetting.svelte";
    import OptionBar from "../common/OptionBar.svelte";
    import MenuListItem from "../common/menulist/MenuListItem.svelte";
    import Menu from "../common/Menu.svelte";
    import ButtonContainer from "../common/buttons/ButtonContainer.svelte";
    import MenuListItemTag from "../common/menulist/MenuListItemTag.svelte";
    import MenuList from "../common/menulist/MenuList.svelte";
    import IconTextButton from "../common/buttons/IconTextButton.svelte";
    import Search from "../common/Search.svelte";
    import MenuListItemButton from "../common/menulist/MenuListItemButton.svelte";
    import type {Account} from "../../../integration/types";
    import {onMount} from "svelte";
    import MultiSelect from "../common/setting/select/MultiSelect.svelte";
    import AddAccountModal from "./addaccount/AddAccountModal.svelte";

    let premiumOnly = false;
    let favoritesOnly = false;
    let accounts: Account[] = [];
    let renderedAccounts: Account[] = [];
    let searchQuery = "";

    let addAccountModalVisible = false;

    $: {
        let filteredAccounts = accounts;
        if (premiumOnly) {
            filteredAccounts = filteredAccounts.filter(a => a.type !== "Cracked");
        }
        if (favoritesOnly) {
            filteredAccounts = filteredAccounts.filter(a => a.favorite);
        }
        if (searchQuery) {
            filteredAccounts = filteredAccounts.filter(a => a.username.toLowerCase().includes(searchQuery.toLowerCase()));
        }
        renderedAccounts = filteredAccounts;
    }

    async function refreshAccounts() {
        accounts = await getAccounts();
    }

    onMount(async () => {
        await refreshAccounts();
        renderedAccounts = accounts;
    });

    function handleSearch(e: CustomEvent<{ query: string }>) {
        searchQuery = e.detail.query;
    }

    function handleAccountSort() {

    }

    async function removeAccount(id: number) {
        await restRemoveAccount(id);
        await refreshAccounts();
    }

    async function loginToRandomAccount() {
        const account = renderedAccounts[Math.floor(Math.random() * renderedAccounts.length)];
        await loginToAccount(account.id);
    }

    async function toggleFavorite(index: number, favorite: boolean) {
        await setAccountFavorite(index, favorite);
        await refreshAccounts();
    }
</script>

<AddAccountModal bind:visible={addAccountModalVisible} on:modify={refreshAccounts}/>
<Menu>
    <OptionBar>
        <Search on:search={handleSearch}/>
        <SwitchSetting title="Premium only" bind:value={premiumOnly}/>
        <SwitchSetting title="Favorites only" bind:value={favoritesOnly}/>
        <MultiSelect title="Account Types" options={["Mojang", "TheAltening"]} values={["Mojang", "TheAltening"]}/>
    </OptionBar>

    <MenuList sortable={false} on:sort={handleAccountSort}>
        {#each renderedAccounts as account}
            <MenuListItem
                    image={account.avatar}
                    title={account.username}>
                <svelte:fragment slot="subtitle">
                    {account.uuid}
                </svelte:fragment>

                <svelte:fragment slot="tag">
                    <MenuListItemTag text={account.type}/>
                </svelte:fragment>

                <svelte:fragment slot="active-visible">
                    <MenuListItemButton title="Delete" icon="trash" on:click={() => removeAccount(account.id)}/>
                    <MenuListItemButton title="Favorite" icon={account.favorite ? "favorite-filled" : "favorite" } on:click={() => toggleFavorite(account.id, !account.favorite)}/>
                </svelte:fragment>

                <svelte:fragment slot="always-visible">
                    <MenuListItemButton title="Login" icon="play" on:click={() => loginToAccount(account.id)}/>
                </svelte:fragment>
            </MenuListItem>
        {/each}
    </MenuList>

    <BottomButtonWrapper>
        <ButtonContainer>
            <IconTextButton icon="icon-plus-circle.svg" title="Add" on:click={() => addAccountModalVisible = true}/>
            <IconTextButton icon="icon-plane.svg" title="Direct"/>
            <IconTextButton icon="altmanager/icon-random.svg" title="Random" on:click={loginToRandomAccount}/>
            <IconTextButton icon="icon-refresh.svg" title="Restore" on:click={restoreSession}/>
        </ButtonContainer>

        <ButtonContainer>
            <IconTextButton icon="icon-back.svg" title="Back" on:click={() => openScreen("title")}/>
        </ButtonContainer>
    </BottomButtonWrapper>
</Menu>
