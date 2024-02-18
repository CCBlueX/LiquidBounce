<script lang="ts">
    import { onMount } from "svelte";
    import type { ModuleSetting, BlocksSetting } from "../../../../integration/types";
    import { getRegistries } from "../../../../integration/rest";
    import BlockResult from "./BlockResult.svelte";

    export let setting: ModuleSetting;

    const cSetting = setting as BlocksSetting;

    let blocks: string[] = [];

    onMount(async () => {
        blocks = (await getRegistries()).blocks;
        console.log(blocks);
    })
</script>

<div class="setting">
    <input type="text" placeholder="Search" class="search-input">
    <div class="results">
        {#each blocks as b}
            <BlockResult identifier={b} />
        {/each}
    </div>
</div>

<style lang="scss">
    @import "../../../../colors.scss";

    .setting {
        padding: 7px 0px;
    }

    .results {
        max-height: 200px;
        overflow: auto;
    }
</style>