<script lang="ts">
    import {
        editWorld,
        getWorlds,
        openScreen,
        openWorld,
        removeWorld as removeWorldRest
    } from "../../../integration/rest.js";
    import BottomButtonWrapper from "../common/buttons/BottomButtonWrapper.svelte";
    import OptionBar from "../common/optionbar/OptionBar.svelte";
    import MenuListItem from "../common/menulist/MenuListItem.svelte";
    import Menu from "../common/Menu.svelte";
    import ButtonContainer from "../common/buttons/ButtonContainer.svelte";
    import MenuListItemTag from "../common/menulist/MenuListItemTag.svelte";
    import MenuList from "../common/menulist/MenuList.svelte";
    import IconTextButton from "../common/buttons/IconTextButton.svelte";
    import Search from "../common/Search.svelte";
    import MenuListItemButton from "../common/menulist/MenuListItemButton.svelte";
    import type {World} from "../../../integration/types";
    import {onMount} from "svelte";
    import MultiSelect from "../common/setting/select/MultiSelect.svelte";
    import {REST_BASE} from "../../../integration/host";
    import dateFormat from "dateformat";

    let gameModes = ["Survival", "Creative", "Adventure", "Spectator"];
    let difficulties = ["Peaceful", "Easy", "Normal", "Hard"];
    let searchQuery = "";

    let worlds: World[] = [];
    let renderedWorlds = worlds;

    $: {
        let filteredWorlds = worlds;
        filteredWorlds = filteredWorlds.filter(w => gameModes.includes(capitalize(w.gameMode)));
        filteredWorlds = filteredWorlds.filter(w => difficulties.includes(capitalize(w.difficulty)));
        if (searchQuery) {
            filteredWorlds = filteredWorlds.filter(w => w.displayName.toLowerCase().includes(searchQuery.toLowerCase()));
        }

        renderedWorlds = filteredWorlds;
    }

    function capitalize(s: string) {
        return s[0].toUpperCase() + s.slice(1);
    }

    onMount(async () => {
        await refreshWorlds();
        renderedWorlds = worlds;
    });

    async function refreshWorlds() {
        worlds = await getWorlds();
    }

    function handleSearch(e: CustomEvent<{ query: string }>) {
        searchQuery = e.detail.query;
    }

    function handleWorldSort() {

    }

    async function removeWorld(name: string) {
        await removeWorldRest(name);
        await refreshWorlds();
    }
</script>

<Menu>
    <OptionBar>
        <Search on:search={handleSearch}/>
        <MultiSelect title="Game Mode" options={["Survival", "Creative", "Adventure", "Spectator"]}
                     bind:values={gameModes}/>
        <MultiSelect title="Difficulty" options={["Peaceful", "Easy", "Normal", "Hard"]} bind:values={difficulties}/>
    </OptionBar>

    <MenuList sortable={false} on:sort={handleWorldSort}>
        {#each renderedWorlds as world}
            <MenuListItem
                    image={!world.icon ?
                        `${REST_BASE}/api/v1/client/resource?id=minecraft:textures/misc/unknown_server.png` :
                        `data:image/png;base64,${world.icon}`}
                    title={world.displayName}
                    on:dblclick={() => openWorld(world.name)}>
                <svelte:fragment slot="subtitle">
                    <span class="world-name">{world.name}</span>
                    <span>({dateFormat(new Date(world.lastPlayed), "yyyy/mm/dd h:MM:ss TT")})</span>
                </svelte:fragment>

                <svelte:fragment slot="tag">
                    <MenuListItemTag text={capitalize(world.gameMode)}/>
                    <MenuListItemTag text={capitalize(world.difficulty)}/>
                    <MenuListItemTag text="Minecraft {world.version}"/>
                </svelte:fragment>

                <svelte:fragment slot="active-visible">
                    <MenuListItemButton title="Delete" icon="trash" on:click={() => removeWorld(world.name)}/>
                    <MenuListItemButton title="Edit" icon="pen-2" on:click={() => editWorld(world.name)}/>
                </svelte:fragment>

                <svelte:fragment slot="always-visible">
                    <MenuListItemButton title="Open" icon="play" on:click={() => openWorld(world.name)}/>
                </svelte:fragment>
            </MenuListItem>
        {/each}
    </MenuList>

    <BottomButtonWrapper>
        <ButtonContainer>
            <IconTextButton icon="icon-plus-circle.svg" title="Add" on:click={() => openScreen("create_world")}/>
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
