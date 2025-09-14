<script>
    import ItemStackDisplay from "./ItemStackDisplay.svelte";

    export let itemStack;
</script>

<ItemStackDisplay
        stack={itemStack}
        dyedMode={true}
/>
