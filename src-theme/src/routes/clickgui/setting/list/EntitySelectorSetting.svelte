<script lang="ts">
    import {createEventDispatcher, onMount} from "svelte";
    import {slide} from "svelte/transition";
    import type {
        ChooseSetting,
        EntitySelectorSetting,
        ListSetting,
        ModuleSetting,
        NamedItem,
        PlayerFilterMode,
        RegistryListSetting
    } from "../../../../integration/types";
    import {getRegistryItems, setTyping} from "../../../../integration/rest";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";
    import {setItem} from "../../../../integration/persistent_storage";
    import ExpandArrow from "../common/ExpandArrow.svelte";
    import Dropdown from "../common/Dropdown.svelte";
    import SearchableList from "./SearchableList.svelte";
    import ListItem from "./ListItem.svelte";
    import SettingButton from "../common/SettingButton.svelte";
    import RemovableItem from "../common/RemovableItem.svelte";
    import BulkListActions from "../common/BulkListActions.svelte";

    export let setting: ModuleSetting;
    export let path: string;

    const cSetting = setting as EntitySelectorSetting;
    const entityTypes = cSetting.value.find(value => value.name === "EntityTypes") as RegistryListSetting;
    const playerMode = cSetting.value.find(value => value.name === "PlayerMode") as ChooseSetting;
    const usernames = cSetting.value.find(value => value.name === "Usernames") as ListSetting;
    const playerModes: PlayerFilterMode[] = [
        "AllowAll",
        "Whitelist",
        "Blacklist",
        "FriendsOnly",
        "NonFriendsOnly"
    ];

    const thisPath = `${path}.${cSetting.name}`;
    let expanded = localStorage.getItem(thisPath) === "true";
    let items: NamedItem[] = [];

    $: setItem(thisPath, expanded.toString());
    $: showUsernames = playerMode.value === "Whitelist" || playerMode.value === "Blacklist";

    const dispatch = createEventDispatcher();

    onMount(async () => {
        const registryItems = await getRegistryItems(entityTypes.registry);
        items = Object.entries(registryItems)
            .map(([identifier, item]) => ({
                value: identifier,
                name: item.name,
                icon: item.icon
            }))
            .sort((a, b) => a.value.localeCompare(b.value));
    });

    function handleChange() {
        setting = {...cSetting};
        dispatch("change");
    }

    function handlePlayerModeChange() {
        handleChange();
        items = [...items];
    }

    function handleEntityToggle(event: CustomEvent<{ value: string, enabled: boolean }>) {
        if (event.detail.enabled) {
            entityTypes.value = [...entityTypes.value, event.detail.value];
        } else {
            entityTypes.value = entityTypes.value.filter(value => value !== event.detail.value);
        }
        handleChange();
    }

    function addUsername() {
        usernames.value = ["", ...usernames.value];
        handleChange();
        items = [...items];
    }

    function removeUsername(index: number) {
        usernames.value = usernames.value.filter((_, currentIndex) => currentIndex !== index);
        handleChange();
        items = [...items];
    }

    function selectAll() {
        entityTypes.value = items.map(item => item.value);
        handleChange();
    }

    function deselectAll() {
        entityTypes.value = [];
        handleChange();
    }
</script>

<div class="setting">
    <!-- svelte-ignore a11y-no-static-element-interactions -->
    <div class="head" class:expanded on:contextmenu|preventDefault={() => expanded = !expanded}>
        <div class="name">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>
        <div class="head-actions">
            <BulkListActions on:selectAll={selectAll} on:deselectAll={deselectAll}/>
            <ExpandArrow bind:expanded/>
        </div>
    </div>

    {#if expanded}
        <div in:slide|global={{duration: 200, axis: "y"}} out:slide|global={{duration: 200, axis: "y"}}>
            <SearchableList {items} let:item>
                <div class="entity-entry">
                    <div class="entity-row">
                        <div class="entity-toggle">
                            <ListItem
                                value={item.value}
                                name={item.name}
                                icon={item.icon}
                                enabled={entityTypes.value.includes(item.value)}
                                on:toggle={handleEntityToggle}
                            />
                        </div>

                        {#if item.value === "minecraft:player"}
                            <div class="player-mode">
                                <Dropdown
                                    name={null}
                                    options={playerModes}
                                    bind:value={playerMode.value}
                                    on:change={handlePlayerModeChange}
                                />
                            </div>
                        {/if}
                    </div>

                    {#if item.value === "minecraft:player" && showUsernames}
                        <div class="username-editor">
                            <SettingButton value="Add username" on:click={addUsername}/>
                            {#each usernames.value as _, index}
                                <RemovableItem on:remove={() => removeUsername(index)}>
                                    <input
                                        class="username"
                                        type="text"
                                        placeholder="Username"
                                        spellcheck="false"
                                        bind:value={usernames.value[index]}
                                        on:input={handleChange}
                                        on:focusin={async () => await setTyping(true)}
                                        on:focusout={async () => await setTyping(false)}
                                    >
                                </RemovableItem>
                            {/each}
                        </div>
                    {/if}
                </div>
            </SearchableList>
        </div>
    {/if}
</div>

<style lang="scss">
  .setting {
    padding: 7px 0;
  }

  .head {
    display: flex;
    justify-content: space-between;
    transition: ease margin-bottom .2s;

    &.expanded {
      margin-bottom: 10px;
    }

    .name {
      color: var(--clickgui-text-color);
      font-size: 12px;
      font-weight: 600;
    }
  }

    .head-actions {
        display: flex;
        align-items: stretch;
    }

  .entity-entry {
    margin-right: 5px;
  }

  .entity-row {
    display: flex;
    align-items: center;
    min-height: 32px;
    gap: 8px;
  }

  .entity-toggle {
    min-width: 0;
    flex: 1;
  }

  .player-mode {
    width: 130px;
    flex: 0 0 130px;
  }

  .username-editor {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 4px 0 8px 30px;
  }

  .username {
    width: 100%;
    background-color: var(--clickgui-input-background-color);
    font-family: monospace;
    font-size: 12px;
    color: var(--clickgui-text-color);
    border: none;
    border-bottom: solid 2px var(--clickgui-input-border-color);
    padding: 6px;
    border-radius: 3px;
  }
</style>
