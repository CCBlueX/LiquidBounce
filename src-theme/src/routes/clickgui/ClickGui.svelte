<script>
    import Panel from "./clickgui/Panel.svelte";
    import SearchBar from "./SearchBar.svelte";

    import { listen } from "../../client/ws.svelte";
    import { getModules, toggleModule, getClickGuiOptions } from "../../client/api.svelte";

    let clickGuiOpened = true;
    // todo: request from API
    const categories = [
        "Movement",
        "Combat",
        "Render",
        "Exploit",
        "Player",
        "World",
        "Misc",
        "Fun",
    ];
    let panels = [];
    let modules = [];

    try {
        getModules().then(mods => {
            for (const mod of mods) {
                const name = mod.name;
                const category = mod.category;
                const enabled = mod.enabled;

                const module = {
                    category: category,
                    name: name,
                    enabled: enabled
                };
                modules.push(module);
            }

            panels = categories
                .map(category => {
                    const filtered = modules.filter(m => m.category === category);

                    return {
                        name: category,
                        top: 30 + categories.indexOf(category) * 45,
                        left: 30,
                        modules: filtered
                    }
                });
        }).catch(console.error);
    } catch (err) {
        console.log(err);
    }

    let options = {
        modulesColor: "rgba(0,0,0,0.5)",
        headerColor: "rgba(0, 0, 0, 0.68)",
        accentColor: "#4677ff",
        textColor: "#ffffff",
        textDimmed: "rgba(211,211,211,255)",
        searchAlwaysOnTop: true,
        autoFocus: true
    };

    getClickGuiOptions().then(opts => {
        options = opts;
    }).catch(console.error);
</script>

<main>
    {#if clickGuiOpened}
        <div class="clickgui-container"
             style=
                     "--modules: {options.modulesColor};
        --header: {options.headerColor};
        --accent: {options.accentColor};
        --accent-dimmed: {options.accentColor};
        --text: {options.textColor};
        --textdimmed: {options.textDimmed};">
            <SearchBar settings={options} modules={modules} listen={listen} toggleModule={toggleModule} />
            {#each panels as panel}
                <Panel name={panel.name} modules={panel.modules} listen={listen} toggleModule={toggleModule} startTop={panel.top}
                       startLeft={panel.left}/>
            {/each}
        </div>
    {/if}
</main>

<style>
    main {
        background-color: rgba(0, 0, 0, 0.5);
    }

    .clickgui-container {
        height: 100vh;
        width: 100vw;
        -webkit-user-select: none;
        -ms-user-select: none;
        user-select: none;
        cursor: default;
    }
</style>
