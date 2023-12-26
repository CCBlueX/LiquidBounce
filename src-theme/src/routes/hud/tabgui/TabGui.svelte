<script>
    import { fly } from "svelte/transition";
    import Category from "./Category.svelte";
    import Module from "./Module.svelte";

    export let listen;
    export let getModules;
    //export let getCategories;
    export let toggleModule;

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
    const modules = [];

    try {
        getModules().then(mods => {
            for (const mod of mods) {
                const name = mod.name;
                const category = mod.category;
                const enabled = mod.enabled;

                function setEnabled(enabled) {
                    toggleModule(name, enabled);
                }

                modules.push({
                    name: name,
                    category: category,
                    enabled: enabled,
                    setEnabled: setEnabled,
                });
            }
        }).catch(console.error);
    } catch (err) {
        console.log(err);
    }

    function getActiveModules() {
        return modules.filter((m) => m.category === categories[activeCategory]);
    }

    let activeModules = [];

    let activeModule = -1;
    let activeCategory = 0;

    let elCategories = document.createElement("div");

    function handleKeydown(event) {
        const action = event.action;
        if (action !== 1) {
            return;
        }

        const key = event.key.name;

        switch (key) {
            case "key.keyboard.down": {
               if (activeModules.length === 0) {
                    activeCategory += 1;
                    activeCategory %= categories.length;
                } else {
                    activeModule += 1;
                    activeModule %= activeModules.length;
                }

                break;
            }
            case "key.keyboard.up": {
                if (activeModules.length === 0) {
                    activeCategory -= 1 - categories.length;
                    activeCategory %= categories.length;
                } else {
                    activeModule -= 1 - activeModules.length;
                    activeModule %= activeModules.length;
                }

                break;
            }
            case "key.keyboard.right": {
                activeModules = [];
                activeModules = getActiveModules();
                activeModule = 0;

                break;
            }
            case "key.keyboard.left": {
                activeModules = [];
                activeModule = -1
                break;
            }
            case "key.keyboard.enter": {
                if (activeModule != -1) {
                    const m = activeModules[activeModule];
                    m.setEnabled(!m.enabled);
                }

                break;
            }
        }
    }

    function handleToggleModule(event) {
        const module = event.moduleName;
        const enabled = event.enabled;

        modules.find((m) => m.name === module).enabled = enabled;
        if (activeModules.length > 0) {
            activeModules = getActiveModules();
        }
    }

    try {
        listen("key", handleKeydown);
        listen("toggleModule", handleToggleModule);
    } catch (err) {
        window.addEventListener("keydown", handleKeydown);
        console.log(err);
    }
</script>

<div class="tabgui">
    <div class="categories" bind:this={elCategories}>
        {#each categories as category, i}
            <Category name={category} active={i === activeCategory} />
        {/each}
    </div>

    {#if activeModules.length > 0}
        <div style="height: {elCategories.offsetHeight}px" class="modules" transition:fly={{ x: -10, duration: 200 }}>
            {#each activeModules as aModule, i}
                <Module
                    name={aModule.name}
                    enabled={aModule.enabled}
                    active={activeModule === i}
                />
            {/each}
        </div>
    {/if}
</div>

<style>
    .tabgui {
        font-family: "Montserrat", sans-serif;

        position: absolute;
        top: 90px;
        left: 15px;
        display: flex;
        flex-direction: row;
    }

    .categories {
        background-clip: content-box;
        display: flex;
        flex-direction: column;
        border-radius: 5px;
        overflow: hidden;
    }

    .modules {
        background-clip: content-box;
        background-color: rgba(0, 0, 0, 0.5);
        margin-left: 6px;
        border-radius: 5px;
        overflow: hidden;
        min-width: 100px;
        display: flex;
        flex-direction: column;
        overflow: auto;
    }

    ::-webkit-scrollbar {
        width: 0;
    }
</style>
