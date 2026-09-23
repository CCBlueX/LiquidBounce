<script lang="ts">
    import {onMount} from "svelte";
    import ScaledClickGuiContent from "../../ScaledClickGuiContent.svelte";
    import Switch from "../../setting/common/Switch.svelte";
    import Segmented from "./Segmented.svelte";
    import Button from "./Button.svelte";
    import Chip from "./Chip.svelte";
    import Label from "./Label.svelte";
    import Message from "./Message.svelte";
    import Row from "./Row.svelte";
    import Tag from "./Tag.svelte";
    import Address from "./Address.svelte";
    import Toast from "./Toast.svelte";
    import ConfigRow from "./ConfigRow.svelte";
    import ItemRow from "./ItemRow.svelte";
    import TrackerMenu from "./TrackerMenu.svelte";
    import ConfigDetail from "./ConfigDetail.svelte";
    import ItemDetail from "./ItemDetail.svelte";
    import LoadPlanModal from "./LoadPlanModal.svelte";
    import PublishModal from "./PublishModal.svelte";
    import PublishedModal from "./PublishedModal.svelte";
    import UpdateModal from "./UpdateModal.svelte";
    import {
        applyMarketplaceTheme,
        changeConfigTracker,
        getClientUser,
        getConfigTracker,
        getInstalledMarketplaceItems,
        getMarketplaceConfigs,
        getMarketplaceContext,
        getMarketplaceItems,
        getMarketplaceTags,
        installMarketplaceItem,
        loginClientUser,
        MarketplaceError,
        removeMarketplaceItem,
        reportMarketplaceConfig,
        updateMarketplaceItem
    } from "../../../../integration/rest";
    import type {
        ConfigTracker,
        MarketplaceConfig,
        MarketplaceContext,
        MarketplaceInstalledItem,
        MarketplaceItem,
        MarketplaceLinkedConfig,
        MarketplacePagination,
        MarketplacePublished
    } from "../../../../integration/types";
    import {listen} from "../../../../integration/ws";
    import {setItem} from "../../../../integration/persistent_storage";
    import {attempt, message, notify, notifyInstalled, typeName, typing, UNKNOWN_PACK, UNKNOWN_SERVER, visible} from "./marketplace";

    const TYPES = {
        "Configs": "Config",
        "Themes": "Theme",
        "Add-ons": "Addon",
        "Scripts": "Script"
    } as const;
    type TypeLabel = keyof typeof TYPES;

    const SEARCH_DELAY = 300;

    function stored(key: string, fallback: string) {
        return localStorage.getItem(`clickgui.marketplace.${key}`) ?? fallback;
    }

    let type = $state(stored("type", "Configs") as TypeLabel);
    let sort = $state(stored("sort", "Top"));
    let featuredOnly = $state(stored("featured", "false") === "true");
    let onServer = $state(stored("server", "false") === "true");
    let tags = $state<string[]>(JSON.parse(stored("tags", "[]")));
    let search = $state("");
    let query = "";

    let context = $state<MarketplaceContext | null>(null);
    let tracker = $state<ConfigTracker | null>(null);
    let loggedIn = $state(false);
    let tagOptions = $state<string[]>([]);

    let configs = $state<MarketplaceConfig[]>([]);
    let items = $state<MarketplaceItem[]>([]);
    let pagination: MarketplacePagination | null = null;
    let codeResult = $state(false);
    let unfeatured = $state(0);
    let loading = $state(false);
    let offline = $state(false);
    let error = $state<string | null>(null);
    let installed = $state<MarketplaceInstalledItem[]>([]);
    let busy = $state<number | null>(null);
    let request = 0;
    let searchTimeout: ReturnType<typeof setTimeout> | undefined;

    let view = $state<{ kind: "browse" } | { kind: "config" | "item"; id: number }>({kind: "browse"});

    let loadTarget = $state<MarketplaceLinkedConfig | null>(null);
    let loadOpen = $state(false);
    let publishOpen = $state(false);
    let published = $state<MarketplacePublished | null>(null);
    let publishedOpen = $state(false);
    let updateOpen = $state(false);

    const configTab = $derived(type === "Configs");
    const server = $derived(onServer ? context?.server : undefined);
    const filtered = $derived(search.trim() !== "" || (configTab && (featuredOnly || !!server || tags.length > 0)));

    onMount(async () => {
        await Promise.all([refreshContext(), refreshTracker(), refreshUser()]);
        await reload();
    });

    listen("configTrackerChange", refreshTracker);
    listen("userLoggedIn", refreshAccount);
    listen("userLoggedOut", refreshAccount);

    async function refreshAccount() {
        await Promise.all([refreshUser(), refreshContext(), refreshTracker()]);
        await reload();
    }

    async function refreshContext() {
        context = await attempt(getMarketplaceContext) ?? null;
    }

    async function refreshUser() {
        loggedIn = (await attempt(getClientUser) ?? null) !== null;
    }

    async function refreshTracker() {
        tracker = await attempt(getConfigTracker) ?? null;
        configs = configs.map(config => ({
            ...config,
            tracking: tracker && config.id === tracker.id ? tracker.state : "None"
        }));
    }

    async function fetchPage(page: number) {
        if (type === "Configs") {
            return await getMarketplaceConfigs({
                page,
                query,
                tags,
                server: !!server,
                featured: featuredOnly,
                sort: sort === "New" ? "new" : "top"
            });
        }
        return await getMarketplaceItems(TYPES[type], page, query, sort === "New" ? "new" : "top");
    }

    async function reload() {
        const id = ++request;
        loading = true;
        error = null;
        try {
            const response = await fetchPage(1);
            if (id !== request) {
                return;
            }

            offline = false;
            if (tagOptions.length === 0) {
                loadTags();
            }
            pagination = response.pagination;
            if ("code" in response) {
                configs = response.items;
                codeResult = response.code;
                unfeatured = response.unfeatured;
            } else {
                items = response.items;
            }
        } catch (e) {
            if (id !== request) {
                return;
            }

            if (e instanceof MarketplaceError && e.status === 503) {
                offline = true;
                installed = await attempt(getInstalledMarketplaceItems) ?? [];
            } else {
                error = message(e);
            }
        } finally {
            if (id === request) {
                loading = false;
            }
        }
    }

    async function loadTags() {
        tagOptions = await getMarketplaceTags().catch(() => []);
    }

    async function loadMore() {
        if (loading || offline || !pagination || pagination.current >= pagination.pages) {
            return;
        }

        const id = request;
        loading = true;
        const response = await attempt(() => fetchPage(pagination!!.current + 1));
        loading = false;
        if (!response || id !== request) {
            return;
        }

        pagination = response.pagination;
        if ("code" in response) {
            configs = [...configs, ...response.items];
        } else {
            items = [...items, ...response.items];
        }
    }

    function persistFilters() {
        setItem("clickgui.marketplace.type", type);
        setItem("clickgui.marketplace.sort", sort);
        setItem("clickgui.marketplace.featured", featuredOnly.toString());
        setItem("clickgui.marketplace.server", onServer.toString());
        setItem("clickgui.marketplace.tags", JSON.stringify(tags));
        reload();
    }

    function changeType(label: string) {
        type = label as TypeLabel;
        configs = [];
        items = [];
        pagination = null;
        persistFilters();
    }

    function changeSort(label: string) {
        sort = label;
        persistFilters();
    }

    function toggleTag(tag: string) {
        tags = tags.includes(tag) ? tags.filter(t => t !== tag) : [...tags, tag];
        persistFilters();
    }

    function handleSearch() {
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(() => {
            query = search.trim();
            reload();
        }, SEARCH_DELAY);
    }

    function clearFilters() {
        search = "";
        query = "";
        featuredOnly = false;
        onServer = false;
        tags = [];
        persistFilters();
    }

    function showUnfeatured() {
        featuredOnly = false;
        persistFilters();
    }

    function openLoad(config: MarketplaceLinkedConfig) {
        loadTarget = config;
        loadOpen = true;
    }

    async function report(config: MarketplaceConfig, works: boolean) {
        const result = await attempt(() => reportMarketplaceConfig(config.id, works));
        if (result) {
            configs = configs.map(c => c.id === config.id ? {...c, works: result.works, fails: result.fails} : c);
            notify(`Reported ${config.address} as ${works ? "working" : "broken"}`);
        }
    }

    async function changeTracker(action: "revert" | "restore" | "detach") {
        const result = await attempt(() => changeConfigTracker(action));
        if (result) {
            tracker = result;
            notify({
                revert: "Reverted your edits",
                restore: "Restored your settings from before",
                detach: "Detached, your settings stay"
            }[action]);
        }
    }

    async function itemAction(item: MarketplaceItem, action: () => Promise<unknown>) {
        if (busy !== null) {
            return;
        }

        busy = item.id;
        await action();
        busy = null;
    }

    function install(item: MarketplaceItem) {
        return itemAction(item, async () => {
            notify(`Installing ${item.name}...`);
            const result = await attempt(() => installMarketplaceItem(item.id));
            if (result) {
                notifyInstalled(result);
                await reload();
            }
        });
    }

    function update(item: MarketplaceItem) {
        return itemAction(item, async () => {
            const updated = await attempt(() => updateMarketplaceItem(item.id));
            if (updated) {
                items = items.map(i => i.id === updated.id ? updated : i);
                notify(`Updated ${item.name}.`);
            }
        });
    }

    function remove(item: MarketplaceItem) {
        return itemAction(item, async () => {
            await attempt(() => removeMarketplaceItem(item.id));
            await reload();
        });
    }

    function apply(item: MarketplaceItem) {
        return itemAction(item, () => attempt(() => applyMarketplaceTheme(item.id)));
    }

    async function login() {
        notify("Continue in your browser");
        await attempt(loginClientUser);
    }

    function open(kind: "config" | "item", id: number) {
        view = {kind, id};
    }

    async function back() {
        view = {kind: "browse"};
        await reload();
    }
</script>

<ScaledClickGuiContent>
    <div class="marketplace" use:typing>
        {#if view.kind === "config"}
            {#key view.id}
                <ConfigDetail id={view.id} {loggedIn} {tracker} tags={tagOptions}
                              onback={back}
                              onload={openLoad}
                              onupdate={() => updateOpen = true}
                              onopen={open}/>
            {/key}
        {:else if view.kind === "item"}
            {#key view.id}
                <ItemDetail id={view.id} onback={back}/>
            {/key}
        {:else}
            <div class="toolbar">
                <Segmented options={Object.keys(TYPES)} value={type} onchange={changeType}/>
                <input class="search" type="text" spellcheck="false" bind:value={search} oninput={handleSearch}
                       placeholder={configTab ? "Search or paste a share code" : `Search ${type.toLowerCase()}`}/>
                <Segmented options={["Top", "New"]} value={sort} onchange={changeSort}/>
                <div class="spacer"></div>
                {#if configTab && tracker && (tracker.state !== "None" || tracker.backup)}
                    <TrackerMenu {tracker} {loggedIn} online={!offline}
                                 onchange={changeTracker}
                                 onpublish={() => publishOpen = true}
                                 onupdate={() => updateOpen = true}
                                 onopen={() => tracker && open("config", tracker.id)}/>
                {/if}
                {#if !loggedIn}
                    <Button title="Log in" primary onclick={login}/>
                {:else if configTab && !offline}
                    <Button title="Publish..." onclick={() => publishOpen = true}/>
                {/if}
            </div>

            {#if configTab && !offline}
                <div class="filters">
                    {#if context?.server}
                        <Chip text="On {context.server}" active={onServer}
                              onclick={() => { onServer = !onServer; persistFilters(); }}/>
                    {/if}
                    <div class="switch">
                        <Switch name="Featured only" bind:value={featuredOnly} on:change={persistFilters}/>
                    </div>
                    {#if tagOptions.length > 0}
                        <span class="divider"></span>
                        {#each tagOptions as tag (tag)}
                            <Chip text={tag} active={tags.includes(tag)} onclick={() => toggleTag(tag)}/>
                        {/each}
                    {/if}
                </div>
            {/if}

            <div class="panel">
                {#if offline}
                    <Message title="Can't reach the marketplace">
                        {#snippet actions()}
                            <Button title="Retry" primary onclick={reload}/>
                        {/snippet}
                    </Message>
                    {#if (tracker && tracker.state !== "None") || installed.length > 0}
                        <div class="section"><Label text="On this client"/></div>
                        {#if tracker && tracker.state !== "None"}
                            <Row image={tracker.image ?? UNKNOWN_SERVER} active>
                                {#snippet title()}<Address address={tracker!!.address}/>{/snippet}
                                {#snippet tags()}<Tag text={tracker!!.state === "Editing" ? "Edited" : "Tracked"}/>{/snippet}
                                {#snippet subtitle()}Config{/snippet}
                            </Row>
                        {/if}
                        {#each installed as item (item.id)}
                            <Row image={UNKNOWN_PACK}>
                                {#snippet title()}{item.name}{/snippet}
                                {#snippet subtitle()}{typeName(item.type)}{/snippet}
                            </Row>
                        {/each}
                    {/if}
                {:else if error}
                    <Message title="Couldn't load the marketplace">
                        {error}
                        {#snippet actions()}
                            <Button title="Retry" primary onclick={reload}/>
                        {/snippet}
                    </Message>
                {:else if configTab}
                    {#if codeResult && configs.length > 0}
                        <div class="section"><Label text="Share code"/></div>
                    {/if}
                    {#each configs as config (config.id)}
                        <ConfigRow {config} {loggedIn}
                                   onload={() => openLoad(config)}
                                   onopen={() => open("config", config.id)}
                                   onreport={works => report(config, works)}/>
                    {/each}
                    {#if !loading && configs.length === 0}
                        {#if codeResult}
                            <Message title="No config has this share code">
                                {#snippet actions()}
                                    <Button title="Clear search" onclick={clearFilters}/>
                                {/snippet}
                            </Message>
                        {:else if featuredOnly && unfeatured > 0}
                            <Message title="No featured config{server ? ` for ${server}` : ''}">
                                {unfeatured} {unfeatured === 1 ? "config matches" : "configs match"}, none featured yet.
                                {server && context?.autoConfig && context?.onlyFeatured
                                    ? "AutoConfig loads nothing here while its OnlyFeatured setting is on." : ""}
                                {#snippet actions()}
                                    <Button title="Show {unfeatured === 1 ? 'it' : `${unfeatured} configs`}" primary
                                            onclick={showUnfeatured}/>
                                    <Button title="Clear filters" onclick={clearFilters}/>
                                {/snippet}
                            </Message>
                        {:else}
                            <Message title="No config found">
                                {#snippet actions()}
                                    {#if filtered}
                                        <Button title="Clear filters" onclick={clearFilters}/>
                                    {/if}
                                {/snippet}
                            </Message>
                        {/if}
                    {/if}
                {:else}
                    {#each items as item (item.id)}
                        <ItemRow {item} busy={busy === item.id}
                                 oninstall={() => install(item)}
                                 onupdate={() => update(item)}
                                 onremove={() => remove(item)}
                                 onapply={() => apply(item)}
                                 onopen={() => open("item", item.id)}/>
                    {/each}
                    {#if !loading && items.length === 0}
                        <Message title="Nothing found"/>
                    {/if}
                {/if}
                <div class="end" use:visible={loadMore}></div>
            </div>
        {/if}
    </div>

    <LoadPlanModal bind:open={loadOpen} config={loadTarget}/>
    <PublishModal bind:open={publishOpen} {tracker} {context} tags={tagOptions}
                  onpublished={p => { published = p; publishedOpen = true; }}/>
    <PublishedModal bind:open={publishedOpen} {published} onopen={id => open("config", id)}/>
    <UpdateModal bind:open={updateOpen} {tracker}/>
    <Toast/>
</ScaledClickGuiContent>

<style lang="scss">
  .marketplace {
    position: absolute;
    top: 70px;
    bottom: 24px;
    left: 50%;
    transform: translateX(-50%);
    width: min(1120px, calc(100% - 48px));
    display: flex;
    flex-direction: column;
    row-gap: 10px;
  }

  .toolbar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px 10px;
  }

  .search {
    all: unset;
    font-family: "Inter", sans-serif;
    font-size: 13px;
    font-weight: 500;
    color: var(--clickgui-text-color);
    background-color: var(--clickgui-search-background-color);
    box-shadow: 0 0 10px var(--clickgui-search-shadow-color);
    border-radius: 30px;
    padding: 9px 18px;
    flex: 0 1 280px;
    min-width: 180px;
    box-sizing: border-box;

    &::placeholder {
      color: var(--clickgui-text-dimmed-color);
    }
  }

  .spacer {
    flex: 1;
  }

  .filters {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 6px;
  }

  .switch {
    padding: 0 6px;
  }

  .divider {
    width: 1px;
    height: 16px;
    margin: 0 4px;
    background-color: var(--clickgui-global-settings-divider-color);
  }

  .panel {
    flex: 0 1 auto;
    min-height: 0;
    overflow: auto;
    background-color: var(--clickgui-window-background-color);
    border-radius: 5px;
    box-shadow: 0 0 10px var(--clickgui-window-shadow-color);

    &::-webkit-scrollbar {
      width: 2px;
    }

    &::-webkit-scrollbar-thumb {
      border-radius: 2px;
    }
  }

  .section {
    padding: 0 14px;
    border-bottom: solid 1px var(--clickgui-global-settings-divider-color);
  }

  .end {
    min-height: 1px;
  }
</style>
