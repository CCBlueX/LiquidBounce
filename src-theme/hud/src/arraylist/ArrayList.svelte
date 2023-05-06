<script>
    import {flip} from "svelte/animate";
    import {fly} from "svelte/transition";
    import Module from "./Module.svelte";

    function getTextWidth(s) {
        if (getTextWidth.ruler === undefined) {
            getTextWidth.ruler = document.getElementById("ruler");
        }

        const ruler = getTextWidth.ruler;
        ruler.innerText = s;

        return ruler.offsetWidth;
    }

    function sortModules() {
        modules = modules.sort(
            (a, b) => getTextWidth(b.name) - getTextWidth(a.name)
        );
    }

    function handleToggleModule(event) {
        const module = event.getModule();
        const m = module.getName();

        if (event.getNewState() && !module.isHidden()) {
            modules.push({
                name: m,
            });
        } else {
            modules = modules.filter((c) => c.name != m);
        }

        sortModules();
    }

    let modules = [];

    try {
        const moduleIterator = client.getModuleManager().iterator();

        while (moduleIterator.hasNext()) {
            const m = moduleIterator.next();

            if (!m.getEnabled() || m.isHidden()) {
                continue;
            }

            modules.push({
                name: m.getName(),
            });
        }

        sortModules();
    } catch (err) {
        console.log(err);
    }

    try {
        events.on("toggleModule", handleToggleModule);
    } catch (err) {
        console.log(err);
    }
</script>

<div class="arraylist">
    {#each modules as aModule (aModule)}
        <div animate:flip={{ duration: 200 }} transition:fly={{ x: 10, duration: 200 }}>
            <Module name={aModule.name} />
        </div>
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
