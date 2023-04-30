<script>
    export let modules;
    let visible = false;
    let value = "";
    let filteredModules = [];

    const filterModules = () => {
        if (0 >= value.length) {
            filteredModules = [];
            return;
        }
        filteredModules = modules.filter(module => module.name.toLowerCase().includes(value.toLowerCase()));
    };

    const handleToggleClick = (module) => {
        module.instance.setEnabled(!module.enabled);
    };

    try {
        events.on("toggleModule", event => {
            const targetModule = event.getModule().getName();
            modules.find(module => targetModule === module.name).enabled = event.getNewState();
            if (visible) {
                filterModules();
            }
        });
    } catch (error) {
        console.log(error);
    }

    window.addEventListener("keydown", event => {
        if (visible) {
            return;
        }
        const key = event.which;
        const ctrlKey = event.ctrlKey ? event.ctrlKey : 17 === key;
        if (ctrlKey && 70 === key) {
            visible = true;
        }
    });
</script>

{#if visible}
    <div class="search-bar">
        <div class="search-bar-input-container">
            <input bind:value={value} on:keyup={() => filterModules()} type="text" placeholder="Search">
        </div>
        {#if 0 < value.length && 0 < filteredModules.length }
            <div class="search-bar-list">
                {#each filteredModules as module}
                    <div class="search-bar-list-item" on:mousedown={() => handleToggleClick(module)}>
                        <span class:active={module.enabled}>
                            {module.name}
                        </span>
                    </div>
                {/each}
            </div>
        {/if}
    </div>
{/if}

<style lang="scss">
    .search-bar {
        margin: 0 auto 0 auto;
        padding-top: 50px;
        width: 600px;

        &-input-container {
            align-items: center;
            display: flex;

            input {
                background-color: rgba(0, 0, 0, 0.68);
                border-radius: 5rem;
                border: 0;
                color: lightgray;
                flex: 1;
                font-size: 18px;
                height: 50px;
                padding: 10px 20px;

                &:not(:placeholder-shown) {
                    border-radius: 10px 10px 0 0;
                }
            }
        }

        &-list {
            background-color: rgba(0, 0, 0, 0.60);
            border-radius: 0 0 10px 10px;
            border-top: solid 2px #4677ff;
            max-height: 220px;
            overflow: auto;
            padding: 10px 0;

            &-item {
                color: lightgray;
                cursor: pointer;
                font-size: 16px;
                padding: 10px 15px;

                &:hover {
                    color: white;
                }
            }
        }
    }

    .active {
        color: #4677ff;
    }
</style>
