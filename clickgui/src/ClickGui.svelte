<script>
    import Panel from "./clickgui/Panel.svelte";

    let clickGuiOpened = true;

    const categories = ["Combat", "Render"];
    const modules = [];
    
    try {
        const moduleIterator = client.getModuleManager().iterator();

        while (moduleIterator.hasNext()) {
            const m = moduleIterator.next();
            modules.push({
                name: m.getName(),
                category: m.getCategory().getReadableName(),
                enabled: m.getEnabled(),
                setEnabled: m.setEnabled,
                settings: valueParser.parse(m.getContainedSettingsRecursively())
            });
        }
    } catch (err) {
        console.log(err);
    }

/*     modules.unshift(
        {
            name: "TestModule",
            category: "Combat",
            enabled: false,
            setEnabled: null,
            settings: [
                {
                    type: "boolean",
                    name: "Test",
                    value: true
                },
                {
                    type: "boolean",
                    name: "Test",
                    value: false
                },
                {
                    type: "range",
                    name: "Range",
                    min: 0,
                    max: 8,
                    step: 0.1,
                    value1: 3,
                    value2: null
                },
                {
                    type: "range",
                    name: "CPS",
                    min: 0,
                    max: 20,
                    step: 1,
                    value1: 4,
                    value2: 12
                },
                {
                    type: "list",
                    name: "Mode",
                    values: ["Multi", "Single", "Switch"],
                    value: "Single"
                },
                {
                    type: "color",
                    name: "Color",
                    value: "2A4AA1"
                },
                {
                    type: "text",
                    name: "Text",
                    value: "This is a text"
                }
            ]
        }
    ); */

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