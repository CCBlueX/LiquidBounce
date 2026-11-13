<script lang="ts">
    import {untrack} from "svelte";
    import TextInput from "../../../common/TextInput.svelte";

    const DELAY = 300;

    /**
     * [query] follows what is typed once typing pauses.
     */
    let {query = $bindable(), placeholder}: {
        query: string;
        placeholder: string;
    } = $props();

    let value = $state("");
    let timeout: ReturnType<typeof setTimeout> | undefined;

    $effect(() => {
        // A query set from outside, as by clearing the filters
        const next = query;
        untrack(() => {
            if (next !== value.trim()) {
                clearTimeout(timeout);
                value = next;
            }
        });
    });

    function handleInput() {
        clearTimeout(timeout);
        timeout = setTimeout(() => query = value.trim(), DELAY);
    }
</script>

<TextInput bind:value {placeholder} oninput={handleInput}/>
