<script lang="ts">
import ModernTargetHud from "./Mode/Modern_TargetHud.svelte";
import HuJiTargetHud from "./Mode/HuJi_TargetHud.svelte";
import SimpleTargetHud from "./Mode/Simple_TargetHud.svelte";
import {listen} from "../../../../integration/ws";
import type {TargetChangeEvent} from "../../../../integration/events";
import {visible} from "./TargetHud";
import type {PlayerData} from "../../../../integration/types";
export let settings: { [name: string]: any };
let target: PlayerData | null = null;
    const modes: Record<string, any> = {
        Modern:ModernTargetHud,
        Simple:SimpleTargetHud,
        户籍:HuJiTargetHud,

    };
let hideTimeout: ReturnType<typeof setTimeout>;

let ModeComponent: any;

$: ModeComponent = modes[settings?.mode] ?? ModernTargetHud;
function startHideTimeout(settings: { [name: string]: any }) {
    clearTimeout(hideTimeout);
    hideTimeout = setTimeout(() => visible.set(false), settings?.timeout ?? 2000);
}

listen("targetChange", (data: TargetChangeEvent) => {
    target = data.target;
    startHideTimeout(settings);
    visible.set(true);
});
startHideTimeout(settings);
</script>
{#if $visible && target }
<svelte:component this={ModeComponent} {settings} />
{/if}
