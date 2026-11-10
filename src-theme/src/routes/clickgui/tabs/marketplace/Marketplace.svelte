<script lang="ts">
    import {onMount} from "svelte";
    import ScaledClickGuiContent from "../../ScaledClickGuiContent.svelte";
    import WindowPanel from "../WindowPanel.svelte";
    import ConfigBrowser from "./browse/ConfigBrowser.svelte";
    import ItemBrowser from "./browse/ItemBrowser.svelte";
    import ConfigDetail from "./detail/ConfigDetail.svelte";
    import ItemDetail from "./detail/ItemDetail.svelte";
    import DeleteDialog from "./dialog/DeleteDialog.svelte";
    import ListingDialog from "./dialog/ListingDialog.svelte";
    import LoadDialog from "./dialog/LoadDialog.svelte";
    import PublishedDialog from "./dialog/PublishedDialog.svelte";
    import UpdateDialog from "./dialog/UpdateDialog.svelte";
    import Sidebar from "./Sidebar.svelte";
    import {getMarketplaceTags} from "../../../../integration/rest";
    import {setItem} from "../../../../integration/persistent_storage";
    import {listen} from "../../../../integration/ws";
    import {
        type ConfigFilters,
        dialog,
        type ListedType,
        refreshCurrentServer,
        refreshTracker,
        refreshUser,
        type View
    } from "./marketplace";

    const STORAGE_KEY = "clickgui.marketplace";

    const stored: { type?: ListedType; filters?: Partial<ConfigFilters> } =
        JSON.parse(localStorage.getItem(STORAGE_KEY) ?? "{}");

    let type = $state<ListedType>(stored.type ?? "Config");
    let filters = $state<ConfigFilters>({sort: "Top", featured: false, onServer: false, tags: [], ...stored.filters});
    let view = $state<View>({kind: "browse"});
    let tags = $state<string[]>([]);
    let itemChanges = $state(0);

    $effect(() => {
        setItem(STORAGE_KEY, JSON.stringify({type, filters}));
    });

    onMount(() => {
        refreshUser();
        refreshTracker();
        refreshCurrentServer();
        getMarketplaceTags().then(names => tags = names, () => tags = []);
    });

    listen("configTrackerChange", refreshTracker);
    listen("userLoggedIn", refreshAccount);
    listen("userLoggedOut", refreshAccount);

    function refreshAccount() {
        refreshUser();
        // Whether the tracked config is the account's own
        refreshTracker();
    }

    function open(next: View) {
        // Chromium does not blur an input that leaves the page, and the client would go on taking keys as typing
        if (document.activeElement instanceof HTMLElement) {
            document.activeElement.blur();
        }
        view = next;
    }

    function selectType(next: ListedType) {
        type = next;
        open({kind: "browse"});
    }

    function closeDialog() {
        $dialog = null;
    }
</script>

<ScaledClickGuiContent>
    <WindowPanel title="Marketplace" icon="world" width={1040} fill>
        <!-- The page stays loaded while the player joins and leaves servers, with nothing telling it -->
        <div class="marketplace" role="presentation" onpointerenter={refreshCurrentServer}>
            <Sidebar {type} bind:filters {tags} browsing={view.kind === "browse"} onselect={selectType}/>

            <!-- The list stays behind a detail page, to come back to where it was scrolled -->
            <div class="view" class:hidden={view.kind !== "browse"}>
                {#if type === "Config"}
                    <ConfigBrowser bind:filters onopen={id => open({kind: "config", id})}/>
                {:else}
                    {#key type}
                        <ItemBrowser {type} sort={filters.sort} changes={itemChanges}
                                     onopen={id => open({kind: "item", id})}/>
                    {/key}
                {/if}
            </div>
            {#if view.kind !== "browse"}
                <div class="view">
                    {#key view.id}
                        {#if view.kind === "config"}
                            <ConfigDetail id={view.id} onback={() => open({kind: "browse"})} onopen={open}/>
                        {:else}
                            <ItemDetail id={view.id} onback={() => open({kind: "browse"})}
                                        onchange={() => itemChanges++}/>
                        {/if}
                    {/key}
                </div>
            {/if}
        </div>
    </WindowPanel>
</ScaledClickGuiContent>

{#if $dialog?.kind === "load"}
    <LoadDialog config={$dialog.config} onclose={closeDialog}/>
{:else if $dialog?.kind === "publish"}
    <ListingDialog {tags} onclose={closeDialog}/>
{:else if $dialog?.kind === "edit"}
    <ListingDialog editing={$dialog.detail} {tags} ondone={$dialog.ondone} onclose={closeDialog}/>
{:else if $dialog?.kind === "update"}
    <UpdateDialog onclose={closeDialog}/>
{:else if $dialog?.kind === "delete"}
    <DeleteDialog config={$dialog.config} ondone={$dialog.ondone} onclose={closeDialog}/>
{:else if $dialog?.kind === "published"}
    {@const published = $dialog.config}
    <PublishedDialog config={published} onclose={closeDialog}
                     onopen={() => { closeDialog(); open({kind: "config", id: published.id}); }}/>
{/if}

<style lang="scss">
  .marketplace {
    display: grid;
    grid-template-columns: 210px minmax(0, 1fr);
    width: 100%;
    min-height: 0;
  }

  .view {
    grid-column: 2;
    display: flex;
    flex-direction: column;
    min-width: 0;
    min-height: 0;

    &.hidden {
      display: none;
    }
  }
</style>
