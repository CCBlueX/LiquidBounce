<script>
    import ItemStackDisplay from "./ItemStackDisplay.svelte";

    export let stack;
</script>

<ItemStackDisplay
        {stack}
        dyedMode={true}
/>
