<script>
    import Module from "./Module.svelte";

    function getTextWidth(text, font) {
        const canvas = getTextWidth.canvas || (getTextWidth.canvas = document.createElement("canvas"));
        const context = canvas.getContext("2d");
        context.font = font;
        const metrics = context.measureText(text);

        return metrics.width;
    }

    function sortModules() {
        modules = modules.sort((a, b) => getTextWidth(b.name, "Montserrat") - getTextWidth(a.name, "Montserrat"));
    }

    function handleToggleModule(event) {
        const m = event.getModule().getName();

        if (event.getNewState()) {
            modules.push({
                name: m
            });
        } else {
            modules = modules.filter(c => c.name != m);
        }

        sortModules();
    }

    let modules = [];

    try {
        const moduleIterator = client.getModuleManager().iterator();

        while (moduleIterator.hasNext()) {
            const m = moduleIterator.next();
            if (!m.getEnabled()) {
                continue;
            }
            modules.push({
                name: m.getName()
            });
        }

        sortModules();
    } catch (err) {
        console.log(err);
    }

    try {
        client.on("toggleModule", handleToggleModule);
    } catch (err) {
        console.log(err);
    }
</script>


<div class="arraylist">
    {#each modules as aModule}
        <Module name={aModule.name} />
    {/each}
</div>

<style>
    .arraylist {
        font-family: "Montserrat", sans-serif;

        position: fixed;
        top: 0;
        right: -10px;
        display: flex;
        flex-direction: column;
        align-items: flex-end;
    }
</style>