<script lang="ts">
    import {onMount} from "svelte";
    import {fly} from "svelte/transition";
    import {cubicOut} from "svelte/easing";
    import SideNav, {type NavItem} from "../../common/SideNav.svelte";
    import ConfigBrowser from "./browse/ConfigBrowser.svelte";
    import ItemBrowser from "./browse/ItemBrowser.svelte";
    import ConfigDetail from "./detail/ConfigDetail.svelte";
    import ItemDetail from "./detail/ItemDetail.svelte";
    import DeleteDialog from "./dialog/DeleteDialog.svelte";
    import ListingDialog from "./dialog/ListingDialog.svelte";
    import LoadDialog from "./dialog/LoadDialog.svelte";
    import PublishedDialog from "./dialog/PublishedDialog.svelte";
    import UpdateDialog from "./dialog/UpdateDialog.svelte";
    import {escapeLayer} from "../../../../integration/escape";
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
        TYPE_TITLES,
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

    // Stand-ins from the menu icons, the theme has none of its own for these
    const TYPE_ICONS: Record<ListedType, string> = {
        Config: "img/menu/icon-clickgui.svg",
        Theme: "img/menu/icon-change-background.svg",
        Addon: "img/menu/icon-customize.svg",
        Script: "img/menu/icon-scripts.svg",
    };

    const items: NavItem[] = Object.entries(TYPE_TITLES).map(([listed, title]) => ({
        id: listed,
        label: title,
        icon: TYPE_ICONS[listed as ListedType],
    }));

    const detailLayer = escapeLayer(showList);

    $effect(() => {
        setItem(STORAGE_KEY, JSON.stringify({type, filters}));
    });

    $effect(() => {
        detailLayer.setOpen(view.kind !== "browse");
    });

    onMount(() => {
        refreshUser();
        refreshTracker();
        refreshCurrentServer();
        getMarketplaceTags().then(names => tags = names, () => tags = []);
    });

    // Listeners are removed by identity, and another instance of the tab can still be fading out
    listen("configTrackerChange", () => refreshTracker());
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

    function showList() {
        open({kind: "browse"});
    }

    function selectType(next: ListedType) {
        type = next;
        showList();
    }

    function closeDialog() {
        $dialog = null;
    }
</script>

<!-- The page stays loaded while the player joins and leaves servers, with nothing telling it -->
<div class="marketplace" role="presentation" onpointerenter={refreshCurrentServer}>
    <SideNav {items} active={type} label="Marketplace" onselect={id => selectType(id as ListedType)}/>

    <section class="main">
        <!-- The list stays behind a detail page, to come back to where it was scrolled -->
        <div class="view" class:hidden={view.kind !== "browse"}>
            {#if type === "Config"}
                <ConfigBrowser bind:filters {tags} onopen={id => open({kind: "config", id})}/>
            {:else}
                {#key type}
                    <ItemBrowser {type} bind:sort={filters.sort} changes={itemChanges}
                                 onopen={id => open({kind: "item", id})}/>
                {/key}
            {/if}
        </div>
        {#if view.kind !== "browse"}
            <div class="view detail" in:fly={{x: 20, duration: 180, easing: cubicOut}}>
                <button type="button" class="back" onclick={showList}>{TYPE_TITLES[type]}</button>
                {#key view.id}
                    {#if view.kind === "config"}
                        <ConfigDetail id={view.id} ondelete={showList} onopen={open}/>
                    {:else}
                        <ItemDetail id={view.id} onchange={() => itemChanges++}/>
                    {/if}
                {/key}
            </div>
        {/if}
    </section>
</div>

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
    flex: 1;
    min-width: 0;
    display: flex;
  }

  .main {
    flex: 1;
    min-width: 0;
    display: flex;
    background-color: var(--clickgui-pane-background-color);
  }

  .view {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-width: 0;
    min-height: 0;

    &.hidden {
      display: none;
    }
  }

  .back {
    flex: none;
    align-self: flex-start;
    margin: 12px 0 0 14px;
    padding: 5px 10px 5px 26px;
    border: none;
    border-radius: 3px;
    background: url("/img/menu/icon-back.svg") 7px center / 13px no-repeat;
    font-family: "Inter", sans-serif;
    font-size: 12px;
    font-weight: 600;
    color: var(--clickgui-text-dimmed-color);
    cursor: pointer;
    transition: ease background-color .15s, ease color .15s;

    &:hover, &:focus-visible {
      color: var(--clickgui-text-color);
      background-color: var(--clickgui-control-hover-background-color);
    }
  }
</style>
