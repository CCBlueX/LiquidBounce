<script lang="ts">
    import { quintOut } from "svelte/easing";
    import {scale} from "svelte/transition";
    import HUDPanel from "./HUDPanel.svelte";
</script>

<div class="hudeditor" transition:scale|global={{ duration: 200, easing:quintOut }}>
    <HUDPanel/>
</div>