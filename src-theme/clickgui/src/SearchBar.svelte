<script>
    export let root;
    export let modules;

    // Initial state of search bar visibility.
    let visible = root.instance.getSearchAlwaysOnTop();
    let autofocus = root.instance.getSearchAutoFocus();
    let value = "";
    let filteredModules = [];
    let selectedModule = null;

    const filterModules = () => {
        // If the input is empty, show nothing
        if (value.length === 0) {
            filteredModules = [];
            return;
        }

        filteredModules = modules.filter(module => module.name.toLowerCase().includes(value.toLowerCase()));
    };

    function isElementVisible(container, element) {
        const containerTop = container.scrollTop;
        const containerBottom = containerTop + container.clientHeight;
        const elementTop = element.offsetTop;
        const elementBottom = elementTop + element.clientHeight;

        return elementTop >= containerTop && elementBottom <= containerBottom;
    }

    function scrollToElement(container, element) {
        if (!isElementVisible(container, element)) {
            element.scrollIntoView({behavior: 'smooth', block: 'nearest'});
        }
    }

    const handleToggleClick = (module) => {
        module.instance.setEnabled(!module.enabled);
    };

    const handleHiglight = (module) => {
        const elem = document.getElementById(module.name + "-module");
        scrollToElement(elem.parentElement, elem)
        elem.classList.add("module-highlight");
        setTimeout(() => {
            elem.classList.remove("module-highlight");
        }, 1000);
    }

    try {
        events.on("toggleModule", event => {
            const targetModule = event.getModule().getName();
            modules.find(module => targetModule === module.name).enabled = event.getNewState();
            filterModules();
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


    function onInput() {
        filterModules();

        if (0 < filteredModules.length) {
            selectedModule = filteredModules[0];
        } else {
            selectedModule = null;
        }
    }

    window.addEventListener("keydown", event => {
        const key = event.which;

        // Move selection up and down
        if (38 === key) {
            if (null === selectedModule) {
                selectedModule = filteredModules[filteredModules.length - 1];
            } else {
                const index = filteredModules.indexOf(selectedModule);
                if (0 === index) {
                    selectedModule = filteredModules[filteredModules.length - 1];
                } else {
                    selectedModule = filteredModules[index - 1];
                }
            }
        } else if (40 === event.which) {
            if (null === selectedModule) {
                selectedModule = filteredModules[0];
            } else {
                const index = filteredModules.indexOf(selectedModule);
                if (filteredModules.length - 1 === index) {
                    selectedModule = filteredModules[0];
                } else {
                    selectedModule = filteredModules[index + 1];
                }
            }
        } else if (13 === event.which) {
            if (null !== selectedModule) {
                handleToggleClick(selectedModule);
            }
        } else {

        }

        // Scroll to selected module
        if (null !== selectedModule) {
            const index = filteredModules.indexOf(selectedModule);
            const element = document.getElementsByClassName("search-bar-list-item")[index];
            element.scrollIntoView({behavior: "smooth", block: "nearest", inline: "start"});
        }
    });
</script>

{#if visible}
    <div class="search-bar">
        <div class="search-bar-input-container">
            <input bind:value type="text" placeholder="Search" on:input={onInput} autofocus={autofocus}>
        </div>
        {#if 0 < filteredModules.length}
            <div class="search-bar-list">
                {#each filteredModules as module}
                    <div class="search-bar-list-item"
                         on:mousedown={(e) => (e.button === 0 ? handleToggleClick : handleHiglight)(module)}
                         class:selected={selectedModule === module}>
                        <span class:active={module.enabled}>
                            {module.name}
                        </span>
                    </div>
                {/each}
            </div>
        {:else}
            {#if 0 < value.length}
                <div class="search-bar-list">
                    <div class="search-bar-list-item">
                        No modules found
                    </div>
                </div>
            {/if}
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
        color: var(--textdimmed);
        flex: 1;
        font-size: 18px;
        height: 50px;
        padding: 10px 20px;
        transition: all 0.2s ease-in-out;

        &:not(:placeholder-shown) {
          border-radius: 10px 10px 0 0;
        }
      }
    }

    &-list {
      background-color: rgba(0, 0, 0, 0.60);
      border-radius: 0 0 10px 10px;
      border-top: solid 2px var(--accent);
      max-height: 220px;
      overflow: auto;
      padding: 10px 0;

      &-item {
        color: var(--textdimmed);
        cursor: pointer;
        font-size: 16px;
        padding: 10px 15px;

        &:hover {
          color: var(--text);
        }

        &.selected {
          background-color: rgba(0, 0, 0, 0.68);
        }
      }
    }
  }

  .active {
    color: var(--accent);
  }
</style>
