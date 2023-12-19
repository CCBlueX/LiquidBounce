<script>
    import Panel from "./clickgui/Panel.svelte";
    import SearchBar from "./SearchBar.svelte";

    import { listen } from "../../client/ws.svelte";
    import { getModules, toggleModule } from "../../client/api.svelte";

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

    /*
    * val searchAlwaysOnTop by boolean("SearchAlwaysOnTop", true)
    val searchAutoFocus by boolean("SearchAutoFocus", true)
    val moduleColor by color("ModuleColor", Color4b(0, 0, 0, 127)) // rgba(0, 0, 0, 0.5)
    val headerColor by color("HeaderColor", Color4b(0, 0, 0, 173)) // rgba(0, 0, 0, 0.68)
    val accentColor by color("AccentColor", Color4b(70, 119, 255, 255)) // #4677ff
    val textColor by color("TextColor", Color4b(255, 255, 255, 255)) // White
    val dimmedTextColor by color("DimmedTextColor", Color4b(211, 211, 211, 255)) // lightgrey
    *
    * todo: request from clickgui settings
    * */
    let clickGuiSettings = {
        modulesColor: "rgba(0,0,0,0.1)",
        headerColor: "rgba(0, 0, 0, 0.4)",
        accentColor: "#4677ff",
        textColor: "#ffffff",
        textDimmed: "rgba(211,211,211,255)",
        searchAlwaysOnTop: true,
        autoFocus: true
    };

    let modulesColor = clickGuiSettings.modulesColor;
    let headerColor = clickGuiSettings.headerColor;
    let accentColor = clickGuiSettings.accentColor;
    let accendDimmed = clickGuiSettings.accentColor;
    let textColor = clickGuiSettings.textColor;
    let textDimmedColor = clickGuiSettings.textDimmed;

    /**
     *
     */
</script>

<main>
    {#if clickGuiOpened}
        <div class="clickgui-container"
             style=
                     "--modules: {modulesColor};
        --header: {headerColor};
        --accent: {accentColor};
        --accent-dimmed: {accendDimmed};
        --text: {textColor};
        --textdimmed: {textDimmedColor};">
            <SearchBar settings={clickGuiSettings} modules={modules} listen={listen} toggleModule={toggleModule} />
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
