<script lang="ts">
    import { fly } from "svelte/transition";
    import type { Module as TModule } from "../../../../integration/types";
    import Module from "./Module.svelte";

    export let modules: TModule[];
    export let height: number;

    let selectedModuleIndex = 0;

    function handleKeyDown(e: KeyboardEvent) {
        switch (e.key) {
            case "ArrowDown":
                selectedModuleIndex =
                    (selectedModuleIndex + 1) % modules.length;
                break;
            case "ArrowUp":
                selectedModuleIndex =
                    (selectedModuleIndex - 1 + modules.length) % modules.length;
                break;
        }
    }
</script>

<svelte:window on:keydown={handleKeyDown} />

<div
    class="modules"
    style="height: {height}px"
    transition:fly={{ x: -10, duration: 200 }}
>
    {#each modules as { name, enabled }, index}
        <Module {name} {enabled} selected={selectedModuleIndex === index} />
    {/each}
</div>

<style lang="scss">
    @import "../../../../colors.scss";

    .modules {
        background-clip: content-box;
        background-color: rgba($tabgui-base-color, 0.5);
        margin-left: 6px;
        border-radius: 5px;
        overflow: hidden;
        min-width: 100px;
        display: flex;
        flex-direction: column;
        overflow: auto;

        &::-webkit-scrollbar {
            width: 0;
        }
    }
</style>
