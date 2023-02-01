<script>
    import Panel from "./clickgui/Panel.svelte";

    let clickGuiOpened = true;

    const categories = client.getModuleManager().getCategories();
    const modules = [];
    
    try {
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
            {#each categories as category}
                <Panel category={category} modules={getModulesOfCategory(category)} />
            {/each}
        </div>
    {/if}
</main>

<style>
    .clickgui-container {
        background-color: rgba(0, 0, 0, .4);
        height: 100vh;
        width: 100vw;
        -webkit-user-select: none;
        -ms-user-select: none; 
        user-select: none; 
        cursor: default;
    }
</style>
