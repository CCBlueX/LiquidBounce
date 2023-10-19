<script>
    import { fly } from "svelte/transition";
    import Category from "./Category.svelte";
    import Module from "./Module.svelte";

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
        const moduleIterator = client.getModuleManager().iterator();

        while (moduleIterator.hasNext()) {
            const m = moduleIterator.next();
            modules.push({
                name: m.getName(),
                category: m.getCategory().getReadableName(),
                enabled: m.getEnabled(),
                setEnabled: m.setEnabled,
            });
        }
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
        if (event.getAction() !== 1) {
            return;
        }

        switch (event.getKey().toString()) {
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
        modules.find(
            (m) => m.name === event.getModule().getName()
        ).enabled = event.getNewState();
        if (activeModules.length > 0) {
            activeModules = getActiveModules();
        }
    }

    try {
        events.on("key", handleKeydown);
        events.on("toggleModule", handleToggleModule);
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
        overflow: hidden;

        /* Change to (rounded) parallelogram */
        transform: skew(-20deg); /* Tilt negative values from upper-left to lower-right */
        transform-origin: 0% 0%; /* Set the origin of the transformation to the top left */
        border-radius: 5px; /* Add rounded corners as needed */
    }


    .modules {
        background-clip: content-box;
        background-color: rgba(0, 0, 0, 0.5);
        margin-left: 6px;
        overflow: hidden;
        min-width: 100px;
        display: flex;
        flex-direction: column;
        overflow: auto;

        /* Change to (rounded) parallelogram */
        transform: skew(-20deg); /* Tilt negative values from upper-left to lower-right */
        transform-origin: 0% 0%; /* Set the origin of the transformation to the top left */
        border-radius: 5px; /* Add rounded corners as needed */
    }


    ::-webkit-scrollbar {
        width: 0;
    }
</style>
