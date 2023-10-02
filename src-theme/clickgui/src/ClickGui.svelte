<script>
  import Panel from "./clickgui/Panel.svelte";
  import SearchBar from "./SearchBar.svelte";

  let clickGuiOpened = true;
  let categories = [];
  let panels = [];
  const modules = [];

  let clickGuiModule;

  try {
    const moduleManager = client.getModuleManager();
    categories = moduleManager.getCategories();
    panels = categories.map((category, index) => ({
      name: category,
      top: 30 + index * 45,
      left: 30,
    }));

    const moduleIterator = moduleManager.iterator();
    while (moduleIterator.hasNext()) {
      const next = moduleIterator.next();
      const module = {
        category: next.getCategory().getReadableName(),
        name: next.getName(),
        instance: next,
        enabled: next.getEnabled(),
      };
      modules.push(module);
      if (module.name.toLowerCase() === "clickgui") {
        clickGuiModule = module;
      }
    }
  } catch (err) {
    console.error(err);
  }

  function getModulesOfCategory(category) {
    return modules.filter((m) => m.category === category);
  }
</script>

<main>
  {#if clickGuiOpened}
    <div class="clickgui-container">
      <SearchBar {root} {modules} />
      {#each panels as panel}
        <Panel {name} {modules} {getModulesOfCategory(panel.name)} {panel.top} {panel.left} />
      {/each}
    </div>
  {/if}
</main>

<style>
  .clickgui-container {
    height: 100vh;
    width: 100vw;
    user-select: none;
    cursor: default;
  }
</style>
