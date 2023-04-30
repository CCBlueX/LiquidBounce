<script>
    import Panel from "./clickgui/Panel.svelte";
    import SearchBar from "./SearchBar.svelte";

    let clickGuiOpened = true;
    let categories = [];
    let panels;
    const modules = [];

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
            const m = moduleIterator.next();
            modules.push({
                category: m.getCategory().getReadableName(),
                name: m.getName(),
                instance: m,
                enabled: m.getEnabled()
            });
        }
    } catch (err) {
        console.log(err);
    }

    function getModulesOfCategory(category) {
        return modules.filter(m => m.category === category);
    }
</script>

<main>
    {#if clickGuiOpened}
        <div class="clickgui-container">
            <SearchBar modules={modules}/>
            {#each panels as panel}
                <Panel name={panel.name} modules={getModulesOfCategory(panel.name)} startTop={panel.top} startLeft={panel.left}/>
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
