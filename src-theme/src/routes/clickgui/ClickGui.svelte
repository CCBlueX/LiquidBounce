<script>
    import Panel from "./clickgui/Panel.svelte";
    import SearchBar from "./SearchBar.svelte";

    let clickGuiOpened = true;
    let categories = [];
    let panels;
    const modules = [];

    let clickGuiModule;

    try {
        categories = client.getModuleManager().getCategories();
        panels = client.getModuleManager().getCategories()
            .map(category => {
                return {
                    name: category,
                    top: 30 + categories.indexOf(category) * 45,
                    left: 30
                }
            });

        const moduleIterator = client.getModuleManager().iterator();
        while (moduleIterator.hasNext()) {
            const next = moduleIterator.next();
            const module = {
                category: next.getCategory().getReadableName(),
                name: next.getName(),
                instance: next,
                enabled: next.getEnabled()
            };
            modules.push(module);
            if ("clickgui" === module.name.toLowerCase()) {
                clickGuiModule = module;
            }
        }
        // console.log(JSON.stringify(modules))

    } catch (err) {
        console.log(err);
    }

    function getModulesOfCategory(category) {
        return modules.filter(m => m.category === category);
    }

    

    let modulesColor = kotlin.colorToHex(clickGuiModule.instance.getModuleColor())
    let headerColor = kotlin.colorToHex(clickGuiModule.instance.getHeaderColor())
    let accentColor = kotlin.colorToHex(clickGuiModule.instance.getAccentColor())
    let accendDimmed = kotlin.colorToHex(clickGuiModule.instance.getAccentColor())
    let textColor = kotlin.colorToHex(clickGuiModule.instance.getTextColor())
    let textDimmedColor = kotlin.colorToHex(clickGuiModule.instance.getDimmedTextColor())
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

            <SearchBar root="{clickGuiModule}" modules={modules}/>
            {#each panels as panel}
                <Panel name={panel.name} modules={getModulesOfCategory(panel.name)} startTop={panel.top}
                       startLeft={panel.left}/>
            {/each}
        </div>
    {/if}
</main>

<style>
    .clickgui-container {
        height: 100vh;
        width: 100vw;
        -webkit-user-select: none;
        -ms-user-select: none;
        user-select: none;
        cursor: default;
    }
</style>
