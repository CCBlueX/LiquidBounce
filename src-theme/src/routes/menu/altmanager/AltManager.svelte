<script lang="ts">
    import {getAccounts, loginToAccount, openScreen} from "../../../integration/rest.js";
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
    let accounts: Account[] = [];
    let renderedAccounts: Account[] = [];
    let searchQuery = "";

    let addAccountModalVisible = false;

    $: {
        let filteredAccounts = accounts;
        if (premiumOnly) {
            filteredAccounts = filteredAccounts.filter(a => a.type !== "Cracked");
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
        accounts = await getAccounts();
        renderedAccounts = accounts;
    });

    function handleSearch(e: CustomEvent<{ query: string }>) {
        searchQuery = e.detail.query;
    }

    function handleAccountSort() {

    }
</script>

<AddAccountModal bind:visible={addAccountModalVisible} on:modify={refreshAccounts}/>
<Menu>
    <OptionBar>
        <Search on:search={handleSearch}/>
        <SwitchSetting title="Premium only" bind:value={premiumOnly}/>
        <MultiSelect title="Account Types" options={["Mojang", "TheAltening"]} values={["Mojang", "TheAltening"]}/>
    </OptionBar>

    <MenuList sortable={false} on:sort={handleAccountSort}>
        {#each renderedAccounts as account, index}
            <MenuListItem
                    image={account.avatar}
                    title={account.username}>
                <span slot="subtitle">
                    {account.uuid}
                </span>


                <svelte:fragment slot="tag">
                    <MenuListItemTag text={account.type}/>
                </svelte:fragment>

                <svelte:fragment slot="active-visible">
                    <MenuListItemButton title="Copy" icon="trash"/>
                    <MenuListItemButton title="Delete" icon="trash"/>
                    <MenuListItemButton title="Edit" icon="pen-2"/>
                </svelte:fragment>

                <svelte:fragment slot="always-visible">
                    <MenuListItemButton title="Login" icon="play" on:click={() => loginToAccount(index)}/>
                </svelte:fragment>
            </MenuListItem>
        {/each}
    </MenuList>

    <BottomButtonWrapper>
        <ButtonContainer>
            <IconTextButton icon="plus-circle" title="Add" on:click={() => addAccountModalVisible = true}/>
            <IconTextButton icon="plus-circle" title="Direct"/>
            <IconTextButton icon="plus-circle" title="Clipboard"/>
            <IconTextButton icon="plus-circle" title="Import"/>
            <IconTextButton icon="plus-circle" title="Random"/>
        </ButtonContainer>

        <ButtonContainer>
            <IconTextButton icon="back" title="Back" on:click={() => openScreen("title")}/>
        </ButtonContainer>
    </BottomButtonWrapper>
</Menu>
