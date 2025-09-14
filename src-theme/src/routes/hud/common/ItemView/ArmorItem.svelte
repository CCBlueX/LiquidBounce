<script>
    import ItemStackDisplay from "./ItemStackDisplay.svelte";

    export let stack;
</script>

<ItemStackDisplay
        {stack}
        showDurability={stack.maxDamage > 0}
        dyedMode={true}
        durabilityPosition="outside"
/>
