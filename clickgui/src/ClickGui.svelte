<script>
    import Panel from "./clickgui/Panel.svelte";

    const categories = [{
        name: "Movement",
        expanded: true
    }];
    const modules = [
        {
            name: "InventoryMove",
            category: "Movement"
        },
        {
            name: "InventoryMove",
            category: "Movement"
        }, 
        {
            name: "InventoryMove",
            category: "Movement"
        }, 
        {
            name: "InventoryMove",
            category: "Movement"
        }, 
        {
            name: "InventoryMove",
            category: "Movement"
        }, 
        {
            name: "InventoryMove",
            category: "Movement"
        }, 
        {
            name: "InventoryMove",
            category: "Movement"
        }, 
        {
            name: "InventoryMove",
            category: "Movement"
        }, 
        {
            name: "InventoryMove",
            category: "Movement"
        }, 
    ];

    function getModulesOfCategory(category) {
        return modules.filter(m => m.category === category);
    }
</script>

<main>
    {#each categories as category}
        <Panel category={category.name} expanded={category.expanded} modules={getModulesOfCategory(category.name)} />
    {/each}
</main>