<script lang="ts">
    import { REST_BASE } from "../../../../integration/host";

    export let identifier: string;
</script>

<div class="block-result">
    {identifier}
    <img class="icon" src="{REST_BASE}/api/v1/client/resource?id=minecraft:textures/block/{identifier.replace("minecraft:", "")}.png" alt={identifier} />
</div>