<script lang="ts">
    import { quintOut } from "svelte/easing";
    import {scale} from "svelte/transition";
    import HUDPanel from "./HUDPanel.svelte";
</script>

<div class="hud-editor" transition:scale|global={{ duration: 200, easing:quintOut }}>
    <HUDPanel/>
</div>

<style lang="scss">
    .hud-editor {
        height: 100vh;
        display: flex;
        justify-content: center; 
        align-items: center;
    }
</style>