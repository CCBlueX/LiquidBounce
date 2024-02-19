<script lang="ts">
    import OptionBar from "../common/OptionBar.svelte";
    import MenuList from "../common/MenuList.svelte";
    import BottomButtonWrapper from "../common/buttons/BottomButtonWrapper.svelte";
    import ButtonContainer from "../common/buttons/ButtonContainer.svelte";
    import IconTextButton from "../common/buttons/IconTextButton.svelte";
    import Menu from "../common/Menu.svelte";
    import Search from "../common/Search.svelte";
    import SwitchSetting from "../common/setting/SwitchSetting.svelte";
    import {pop} from "svelte-spa-router";
</script>

<Menu>
    <OptionBar>
        <Search/>
        <SwitchSetting title="Favorites only" value={true}/>
        <SwitchSetting title="Current version" value={false}/>
        <SwitchSetting title="Online" value={true}/>
    </OptionBar>

    <MenuList>

    </MenuList>

    <BottomButtonWrapper>
        <ButtonContainer>
            <IconTextButton icon="exit" title="Add"/>
            <IconTextButton icon="exit" title="Direct"/>
            <IconTextButton icon="exit" title="Refresh"/>
        </ButtonContainer>

        <ButtonContainer>
            <IconTextButton icon="exit" title="Back" on:click={pop}/>
        </ButtonContainer>
    </BottomButtonWrapper>
</Menu>