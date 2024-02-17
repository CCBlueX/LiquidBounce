<script>
    import Panel from "./clickgui/Panel.svelte";
    import SearchBar from "./SearchBar.svelte";

    
    import { getModules, toggleModule, getClickGuiOptions } from "../../client/api.svelte";
    import { blur } from "svelte/transition";

    // todo: request from API
    const categories = [
        "Movement",
        "Combat",
        "Render",
        "Exploit",
        "Player",
        "Client",
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
                    description: mod.description,
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
        autoFocus: true,
        shadow: true
    };

    getClickGuiOptions().then(opts => {
        options = opts;
    }).catch(console.error);
</script>

<div class="clickgui-container"
     style="--modules: {options.modulesColor};
        --header: {options.headerColor};
        --accent: {options.accentColor};
        --accent-dimmed: {options.accentColor};
        --text: {options.textColor};
        --textdimmed: {options.textDimmed};" transition:blur={{ duration: 200 }}>
    <SearchBar settings={options} {modules} toggleModule={toggleModule} />
    {#each panels as panel}
        <Panel name={panel.name} modules={panel.modules} settings={options} toggleModule={toggleModule} startTop={panel.top}
               startLeft={panel.left}/>
    {/each}
</div>

<style>
    .clickgui-container {
        height: 100vh;
        width: 100vw;
        -webkit-user-select: none;
        -ms-user-select: none;
        user-select: none;
        cursor: default;
        position: fixed;
        top: 0;
        left: 0;
        background-color: rgba(0, 0, 0, 0.0);
    }

    :global(.clickgui-shadow) {
        box-shadow: 0 0 10px rgba(0, 0, 0, 0.5);
    }
</style>
