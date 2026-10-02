<script lang="ts">
    import {onMount} from "svelte";
    import ScaledClickGuiContent from "../../ScaledClickGuiContent.svelte";
    import Switch from "../../setting/common/Switch.svelte";
    import SegmentedControl from "../../common/SegmentedControl.svelte";
    import PillButton from "../../common/PillButton.svelte";
    import Chip from "../../common/Chip.svelte";
    import SectionLabel from "../../common/SectionLabel.svelte";
    import Notice from "../../common/Notice.svelte";
    import ListRow from "../../common/ListRow.svelte";
    import ItemAction from "./ItemAction.svelte";
    import ConfigDetail from "./detail/ConfigDetail.svelte";
    import ItemDetail from "./detail/ItemDetail.svelte";
    import LoadDialog from "./loading/LoadDialog.svelte";
    import {
        applyMarketplaceTheme,
        getClientUser,
        getCurrentServer,
        getInstalledMarketplaceItems,
        getMarketplaceConfigs,
        getMarketplaceItems,
        getMarketplaceTags,
        getModule,
        getModuleSettings,
        installMarketplaceItem,
        loginClientUser,
        MarketplaceError,
        removeMarketplaceItem,
        reportMarketplaceConfig,
        updateMarketplaceItem
    } from "../../../../integration/rest";
    import type {
        MarketplaceConfig,
        MarketplaceInstalledItem,
        MarketplaceItem,
        MarketplacePagination
    } from "../../../../integration/types";
    import {listen} from "../../../../integration/ws";
    import {setItem} from "../../../../integration/persistent_storage";
    import {
        configBadges,
        configLine,
        dialog,
        itemBadges,
        reports,
        reviews,
        typeName,
        UNKNOWN_PACK,
        UNKNOWN_SERVER,
        version
    } from "./marketplace";
    import {compactNumber, errorMessage} from "../../../../util/utils";
    import {typing, visible} from "../../../../integration/util";

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

    let currentServer = $state<string | undefined>();
    let autoConfigOnlyFeatured = $state(false);
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

    const configTab = $derived(type === "Configs");
    const server = $derived(onServer ? currentServer : undefined);
    const filtered = $derived(search.trim() !== "" || (configTab && (featuredOnly || !!server || tags.length > 0)));

    onMount(async () => {
        await Promise.all([refreshServer(), refreshAutoConfig(), refreshUser()]);
        await reload();
    });

    listen("userLoggedIn", refreshAccount);
    listen("userLoggedOut", refreshAccount);

    async function refreshAccount() {
        await refreshUser();
        await reload();
    }

    async function refreshServer() {
        currentServer = (await getCurrentServer().catch(() => null))?.rootDomain;
    }

    // With OnlyFeatured, AutoConfig loads nothing on a server that has no featured config
    async function refreshAutoConfig() {
        const [module, settings] = await Promise.all([
            getModule("AutoConfig"),
            getModuleSettings("AutoConfig")
        ]).catch(() => []);
        autoConfigOnlyFeatured = !!module?.enabled
            && !!settings?.value.some(setting => setting.name === "OnlyFeatured" && setting.value === true);
    }

    async function refreshUser() {
        loggedIn = (await getClientUser().catch(() => null)) !== null;
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
                installed = await getInstalledMarketplaceItems().catch(() => []);
            } else {
                error = errorMessage(e);
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
        const response = await fetchPage(pagination!!.current + 1).catch(() => null);
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

    async function report(config: MarketplaceConfig, works: boolean) {
        const result = await reportMarketplaceConfig(config.id, works).catch(() => null);
        if (result) {
            configs = configs.map(c => c.id === config.id ? {...c, works: result.works, fails: result.fails} : c);
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
            await installMarketplaceItem(item.id).catch(() => null);
            await reload();
        });
    }

    function update(item: MarketplaceItem) {
        return itemAction(item, async () => {
            const updated = await updateMarketplaceItem(item.id).catch(() => null);
            if (updated) {
                items = items.map(i => i.id === updated.id ? updated : i);
            }
        });
    }

    function remove(item: MarketplaceItem) {
        return itemAction(item, async () => {
            await removeMarketplaceItem(item.id).catch(() => null);
            await reload();
        });
    }

    function apply(item: MarketplaceItem) {
        return itemAction(item, () => applyMarketplaceTheme(item.id).catch(() => null));
    }

    async function login() {
        await loginClientUser().catch(() => null);
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
                <ConfigDetail id={view.id} {loggedIn} onback={back} onopen={open}/>
            {/key}
        {:else if view.kind === "item"}
            {#key view.id}
                <ItemDetail id={view.id} onback={back}/>
            {/key}
        {:else}
            <div class="toolbar">
                <SegmentedControl options={Object.keys(TYPES)} value={type} onchange={changeType}/>
                <input class="search" type="text" spellcheck="false" bind:value={search} oninput={handleSearch}
                       placeholder={configTab ? "Search or paste a share code" : `Search ${type.toLowerCase()}`}/>
                <SegmentedControl options={["Top", "New"]} value={sort} onchange={changeSort}/>
                <div class="spacer"></div>
                {#if !loggedIn}
                    <PillButton title="Log in" primary onclick={login}/>
                {/if}
            </div>

            {#if configTab && !offline}
                <div class="filters">
                    {#if currentServer}
                        <Chip text="On {currentServer}" active={onServer}
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
                    <Notice title="Can't reach the marketplace">
                        {#snippet actions()}
                            <PillButton title="Retry" primary onclick={reload}/>
                        {/snippet}
                    </Notice>
                    {#if installed.length > 0}
                        <div class="section"><SectionLabel text="On this client"/></div>
                        {#each installed as item (item.id)}
                            <ListRow image={UNKNOWN_PACK} title={item.name} subtitle={typeName(item.type)}/>
                        {/each}
                    {/if}
                {:else if error}
                    <Notice title="Couldn't load the marketplace">
                        {error}
                        {#snippet actions()}
                            <PillButton title="Retry" primary onclick={reload}/>
                        {/snippet}
                    </Notice>
                {:else if configTab}
                    {#if codeResult && configs.length > 0}
                        <div class="section"><SectionLabel text="Share code"/></div>
                    {/if}
                    {#each configs as config (config.id)}
                        {#snippet reportButtons()}
                            <PillButton title="Works" onclick={() => report(config, true)}/>
                            <PillButton title="Broken" onclick={() => report(config, false)}/>
                        {/snippet}
                        <ListRow image={config.image ?? UNKNOWN_SERVER} address={config.address} subtitle={configLine(config)}
                                 badges={[
                                     ...configBadges(config, config.overlayOn && `Overlay on ${config.overlayOn}`),
                                     ...config.tags
                                 ]}
                                 onclick={() => open("config", config.id)}
                                 hover={loggedIn ? reportButtons : undefined}>
                            {#snippet meta()}
                                <span class="reports">{reports(config.works, config.fails)}</span>
                                <span class="downloads">{compactNumber(config.downloads)} downloads</span>
                            {/snippet}
                            {#snippet actions()}
                                <PillButton title="Load" primary onclick={() => dialog.set({kind: "load", config})}/>
                            {/snippet}
                        </ListRow>
                    {/each}
                    {#if !loading && configs.length === 0}
                        {#if codeResult}
                            <Notice title="No config has this share code">
                                {#snippet actions()}
                                    <PillButton title="Clear search" onclick={clearFilters}/>
                                {/snippet}
                            </Notice>
                        {:else if featuredOnly && unfeatured > 0}
                            <Notice title="No featured config{server ? ` for ${server}` : ''}">
                                {unfeatured} {unfeatured === 1 ? "config matches" : "configs match"}, none featured yet.
                                {server && autoConfigOnlyFeatured
                                    ? "AutoConfig loads nothing here while its OnlyFeatured setting is on." : ""}
                                {#snippet actions()}
                                    <PillButton title="Show {unfeatured === 1 ? 'it' : `${unfeatured} configs`}" primary
                                                onclick={showUnfeatured}/>
                                    <PillButton title="Clear filters" onclick={clearFilters}/>
                                {/snippet}
                            </Notice>
                        {:else}
                            <Notice title="No config found">
                                {#snippet actions()}
                                    {#if filtered}
                                        <PillButton title="Clear filters" onclick={clearFilters}/>
                                    {/if}
                                {/snippet}
                            </Notice>
                        {/if}
                    {/if}
                {:else}
                    {#each items as item (item.id)}
                        {#snippet removeButton()}
                            <PillButton title="Remove" disabled={busy === item.id} onclick={() => remove(item)}/>
                        {/snippet}
                        <ListRow image={item.image ?? UNKNOWN_PACK} title={item.name} by={item.author} subtitle={item.summary}
                                 badges={itemBadges(item, item.subscribed && !!item.installed && version(item.installed))}
                                 dim={!!item.notFor} onclick={() => open("item", item.id)}
                                 hover={item.subscribed ? removeButton : undefined}>
                            {#snippet meta()}
                                {#if item.notFor}
                                    <span>Not for {item.notFor}</span>
                                {/if}
                                {#if item.rating !== undefined}
                                    <span>{item.rating.toFixed(1)} from {reviews(item.reviews)}</span>
                                {/if}
                                <span class="downloads">{compactNumber(item.downloads)} downloads</span>
                            {/snippet}
                            {#snippet actions()}
                                <ItemAction {item} busy={busy === item.id} oninstall={() => install(item)}
                                            onupdate={() => update(item)} onapply={() => apply(item)}/>
                            {/snippet}
                        </ListRow>
                    {/each}
                    {#if !loading && items.length === 0}
                        <Notice title="Nothing found"/>
                    {/if}
                {/if}
                <div class="end" use:visible={loadMore}></div>
            </div>
        {/if}
    </div>

    <LoadDialog/>
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

  .reports {
    min-width: 110px;
    text-align: right;
  }

  .downloads {
    min-width: 90px;
    text-align: right;
  }
</style>
