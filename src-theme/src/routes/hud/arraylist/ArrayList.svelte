<script>
    import {flip} from "svelte/animate";
    import {fly} from "svelte/transition";
    import Module from "./Module.svelte";

    export let getModules;
    export let listen;


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
        let moduleName = event.moduleName;
        let moduleEnabled = event.enabled;
        let hidden = event.hidden;

        if (moduleEnabled && !hidden) {
            modules.push({
                name: moduleName,
                enabled: moduleEnabled,
            });
        } else {
            modules = modules.filter((c) => c.name != moduleName);
        }

        sortModules();
    }

    let modules = [];

    function constructArray() {
        getModules().then(mods => {
            modules = [];
            for (const mod of mods) {
                const name = mod.name;
                const enabled = mod.enabled;
                const hidden = mod.hidden;

                if (enabled && !hidden) {
                    modules.push({
                        name: name,
                    });
                }
            }

            sortModules();
        }).catch(console.error);
    }
    constructArray();

    listen("toggleModule", handleToggleModule);
    listen("refreshArrayList", constructArray);
</script>

<div class="arraylist">
    {#each modules as aModule (aModule)}
        <div animate:flip={{ duration: 200 }} transition:fly={{ x: 10, duration: 200 }}>
            <Module name={aModule.name}/>
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
