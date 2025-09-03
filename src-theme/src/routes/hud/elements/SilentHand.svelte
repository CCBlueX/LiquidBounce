<script lang="ts">
    import { onMount } from "svelte"
    import type {
        BlockCountChangeEvent,
        ClientPlayerDataEvent, EventMap,
    } from "../../../integration/events"
    import type { PlayerData} from "../../../integration/types"
    import { listen } from "../../../integration/ws"
    import { getPlayerData, getModules } from "../../../integration/rest"
    import { FadeOut } from "../../../util/animate_utils"
    import { REST_BASE } from "../../../integration/host"

    let selectingHotbar = false
    let silentHotbarEnabled = false
    let count: number | undefined
    let playerData: PlayerData | null = null

    listen("blockCountChange", (e: BlockCountChangeEvent) => count = e.count)
    listen("clientPlayerData", (e: ClientPlayerDataEvent) => playerData = e.playerData)
    listen("selectingHotbarSlotSilently"as keyof EventMap, () =>selectingHotbar = true)
    listen("resetHotbarSlotSilently"as keyof EventMap, () => selectingHotbar = false)
    listen("moduleToggle", () => checkSilentHotbar())

    async function checkSilentHotbar() {
        const modules = await getModules()
        silentHotbarEnabled = modules.some(m => m.name === "SilentHotbar" && m.enabled)
    }

    onMount(async () => {
        playerData = await getPlayerData()
        await checkSilentHotbar()
    })
</script>

{#if silentHotbarEnabled && selectingHotbar}
    {#if count === undefined && playerData?.mainHandStack && playerData.mainHandStack.identifier !== "minecraft:air"}
        <div class="silent-hand-container" out:FadeOut|global={{ duration: 200 }}>
            <div class="item-icon hud-container">
                <img
                        class="icon"
                        src={`${REST_BASE}/api/v1/client/resource/itemTexture?id=${playerData.mainHandStack.identifier}`}
                        alt={playerData.mainHandStack.identifier}
                />
            </div>
        </div>
    {/if}
{/if}


<style lang="scss">
  @use "../../../colors" as *;

  .silent-hand-container {
    display: flex;
    justify-content: center;
    align-items: center;
    position: absolute;
  }

  .item-icon {
    width: 64px;
    height: 64px;
    display: flex;
    justify-content: center;
    align-items: center;

    .icon {
      width: 100%;
      height: 100%;
      object-fit: contain;
      display: block;
    }
  }

</style>
