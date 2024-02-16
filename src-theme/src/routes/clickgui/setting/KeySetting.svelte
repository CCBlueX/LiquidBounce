<script lang="ts">
    import type { ModuleSetting, KeySetting } from "../../../integration/types";
    import { listen } from "../../../integration/ws";

    export let setting: ModuleSetting;

    const cSetting = setting as KeySetting;

    let binding = false;

    listen("key", (e: any) => {
        if (!binding) {
            return;
        }

        if (e.key.name !== "key.keyboard.escape") {
            cSetting.value = e.key.code;
        } else {
            cSetting.value = -1;
        }

        setting = { ...cSetting };

        binding = false;
    });
</script>

<div class="setting">
    <button class="change-bind" on:click={() => (binding = true)}>
        {#if !binding}
            {#if cSetting.value === -1}
                <span class="none">None</span>
            {:else}
                <span>{cSetting.value}</span>
            {/if}
        {:else}
            <span>Press any key</span>
        {/if}
    </button>
</div>

<style lang="scss">
    @import "../../../colors.scss";

    .setting {
        padding: 7px 0px;
    }

    .change-bind {
        background-color: transparent;
        border: solid 1px $accent-color;
        border-radius: 3px;
        cursor: pointer;
        padding: 5px;
        font-weight: 500;
        color: $clickgui-text-color;
        font-size: 12px;
        font-family: "Inter", sans-serif;
        width: 100%;

        .none {
            color: $clickgui-text-dimmed-color;
        }
    }
</style>
