<script lang="ts">
    import {
        deleteScreen,
        getAccounts,
        loginToAccount as loginToAccountRest,
        orderAccounts,
        removeAccount as restRemoveAccount,
        restoreSession,
        setAccountFavorite
    } from "../../../integration/rest.js";
    import BottomButtonWrapper from "../common/buttons/BottomButtonWrapper.svelte";
    import SwitchSetting from "../common/setting/SwitchSetting.svelte";
    import OptionBar from "../common/optionbar/OptionBar.svelte";
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
    import {listen} from "../../../integration/ws";
    import {notification} from "../common/header/notification_store";
    import type {
        AccountManagerAdditionEvent,
        AccountManagerLoginEvent,
    } from "../../../integration/events.js";
    import DirectLoginModal from "./directLogin/DirectLoginModal.svelte";

    let premiumOnly = false;
    let favoritesOnly = false;
    let accountTypes = ["Mojang", "TheAltening"];
    let accounts: Account[] = [];
    let renderedAccounts: Account[] = [];
    let searchQuery = "";

    let addAccountModalVisible = false;
    let directLoginModalVisible = false;

    $: {
        let filteredAccounts = accounts;
        if (premiumOnly) {
            filteredAccounts = filteredAccounts.filter(a => a.type !== "Cracked");
        }
        if (favoritesOnly) {
            filteredAccounts = filteredAccounts.filter(a => a.favorite);
        }
        if (!accountTypes.includes("Mojang")) {
            filteredAccounts = filteredAccounts.filter(a => a.type !== "Cracked" && a.type !== "Microsoft")
        }
        if (!accountTypes.includes("TheAltening")) {
            filteredAccounts = filteredAccounts.filter(a => a.type !== "TheAltening")
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

    async function handleAccountSort(e: CustomEvent<{ newOrder: number[] }>) {
        await orderAccounts(e.detail.newOrder);
        await refreshAccounts();
        renderedAccounts = accounts;
    }

    async function removeAccount(id: number) {
        await restRemoveAccount(id);
        await refreshAccounts();
    }

    async function loginToRandomAccount() {
        const account = renderedAccounts[Math.floor(Math.random() * renderedAccounts.length)];
        if (account) {
            await loginToAccount(account.id);
        }
    }

    async function toggleFavorite(index: number, favorite: boolean) {
        await setAccountFavorite(index, favorite);
        await refreshAccounts();
    }

    async function loginToAccount(id: number) {
        notification.set({
            title: "AltManager",
            message: "Logging in...",
            error: false
        });
        await loginToAccountRest(id);
    }

    listen("accountManagerAddition", (e: AccountManagerAdditionEvent) => {
        addAccountModalVisible = false;
        refreshAccounts();
    });

    listen("accountManagerLogin", (e: AccountManagerLoginEvent) => {
        directLoginModalVisible = false;
    });
</script>

<DirectLoginModal bind:visible={directLoginModalVisible}/>
<AddAccountModal bind:visible={addAccountModalVisible}/>
<Menu>
    <OptionBar>
        <Search on:search={handleSearch}/>
        <SwitchSetting title="Premium Only" bind:value={premiumOnly}/>
        <SwitchSetting title="Favorites Only" bind:value={favoritesOnly}/>
        <MultiSelect title="Account Type" options={["Mojang", "TheAltening"]} bind:values={accountTypes}/>
    </OptionBar>

    <MenuList sortable={accounts.length === renderedAccounts.length} elementCount={accounts.length}
              on:sort={handleAccountSort}>
        {#key accounts}
            {#each renderedAccounts as account}
                <MenuListItem
                        image={account.avatar}
                        title={account.username}
                        favorite={account.favorite}
                        on:dblclick={() => loginToAccount(account.id)}>
                    <svelte:fragment slot="subtitle">
                        <pre class="uuid">{account.uuid}</pre>
                    </svelte:fragment>

                    <svelte:fragment slot="tag">
                        <MenuListItemTag text={account.type}/>
                    </svelte:fragment>

                    <svelte:fragment slot="active-visible">
                        <MenuListItemButton title="Delete" icon="trash" on:click={() => removeAccount(account.id)}/>
                        <MenuListItemButton title="Favorite" icon={account.favorite ? "favorite-filled" : "favorite" }
                                            on:click={() => toggleFavorite(account.id, !account.favorite)}/>
                    </svelte:fragment>

                    <svelte:fragment slot="always-visible">
                        <MenuListItemButton title="Login" icon="play" on:click={() => loginToAccount(account.id)}/>
                    </svelte:fragment>
                </MenuListItem>
            {/each}
        {/key}
    </MenuList>

    <BottomButtonWrapper>
        <ButtonContainer>
            <IconTextButton icon="icon-plus-circle.svg" title="Add" on:click={() => addAccountModalVisible = true}/>
            <IconTextButton icon="icon-plane.svg" title="Direct" on:click={() => directLoginModalVisible = true}/>
            <IconTextButton icon="icon-random.svg" disabled={renderedAccounts.length === 0} title="Random"
                            on:click={loginToRandomAccount}/>
            <IconTextButton icon="icon-refresh.svg" title="Restore" on:click={restoreSession}/>
        </ButtonContainer>

        <ButtonContainer>
            <IconTextButton icon="icon-back.svg" title="Back" on:click={() => deleteScreen()}/>
        </ButtonContainer>
    </BottomButtonWrapper>
</Menu>

<style lang="scss">
  .uuid {
    font-family: monospace;
  }
</style>
