<!--
  - This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
  -
  - Copyright (c) 2015 - 2025 CCBlueX
  -
  - LiquidBounce is free software: you can redistribute it and/or modify
  - it under the terms of the GNU General Public License as published by
  - the Free Software Foundation, either version 3 of the License, or
  - (at your option) any later version.
  -
  - LiquidBounce is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  - GNU General Public License for more details.
  -
  - You should have received a copy of the GNU General Public License
  - along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
  -->

<script lang="ts">
    import {listen} from "../../../integration/ws";
    import type {BedState, PlayerData, SurroundingBlock} from "../../../integration/types";
    import {REST_BASE} from "../../../integration/host";
    import {distance, distanceSq} from "../../../util/math_utils";

    interface HudComponentProps {
        settings: { [name: string]: any };
    }

    const {settings}: HudComponentProps = $props();

    let beds: BedState[] = $state([]);
    let playerData: PlayerData | null = $state(null);
    $effect(() => {
        if (playerData && beds.length > 1) {
            const playerPos = playerData.position;
            beds.sort((a, b) => distanceSq(a.pos, playerPos) - distanceSq(b.pos, playerPos));
        }
    });

    listen("bedStateChange", (event) => beds = event.bedStates);

    listen("clientPlayerData", (event) => playerData = event.playerData);

    const itemIconUrl = (identifier: string) => `${REST_BASE}/api/v1/client/resource/itemTexture?id=${identifier}`;

    /**
     * @param centerX center (zero) X
     * @param centerZ center (zero) Z
     * @param yaw yaw in degrees. positive-Z = 0 deg.
     * @param targetX target (object) X
     * @param targetZ target (object) Z
     * @return angle difference in radians
     */
    const calculateRelativeDirection = (centerX: number, centerZ: number, yaw: number, targetX: number, targetZ: number) => {
        const dx = targetX - centerX;
        const dz = targetZ - centerZ;
        const targetAngleRad = Math.atan2(-dx, dz);

        // -\pi to \pi
        let angleDiff = targetAngleRad - yaw * Math.PI / 180;

        // normalize
        if (angleDiff < 0) angleDiff += 2 * Math.PI;

        return angleDiff;
    }

    // compact = false
    const processSurroundingBlocks = (list: SurroundingBlock[]) => {
        const result: { layer: number, blocks: { block: string, count: number }[], }[] = [];

        if (!list?.length) {
            return result;
        }

        let currentLayer = list[0].layer;
        let currentBlocks: { block: string, count: number }[] = [];

        list.forEach(item => {
            if (item.layer !== currentLayer) {
                result.push({layer: currentLayer, blocks: currentBlocks});
                currentLayer = item.layer;
                currentBlocks = [];
            }
            currentBlocks.push({block: item.block, count: item.count});
        });

        result.push({layer: currentLayer, blocks: currentBlocks});

        return result;
    }

    const ROMAN_NUMERALS = ["", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"];
</script>

{#if beds?.length}
    <div class="container">
        {#each beds as bed (bed.trackedBlockPos)}
            <div class="row">
                <code class="distance">{playerData ? `${Math.floor(distance(bed.pos, playerData.position))}m` : '---m'}</code>
                <img class="bed" src={itemIconUrl(bed.block)} alt={bed.block}/>
                {#if settings.showDirection}
                    <span class="direction">
                        {#if playerData}
                            <div style="transform: rotate({calculateRelativeDirection(playerData.position.x, playerData.position.z, playerData.yaw, bed.pos.x, bed.pos.z)}rad)">↑</div>
                        {:else}
                            -
                        {/if}
                    </span>
                {/if}
                {#if bed.surroundingBlocks?.length}
                    <hr/>
                {/if}
                {#if settings.compact}
                    {#each bed.compactSurroundingBlocks as {block, count} (block)}
                        <div class="block">
                            <img src={itemIconUrl(block)} alt={block}/>
                            <code class="count">{count}</code>
                        </div>
                    {/each}
                {:else}
                    {#each processSurroundingBlocks(bed.surroundingBlocks) as {blocks, layer} (layer)}
                        {#if layer}
                            <code style="color: white">{ROMAN_NUMERALS[layer]}</code>
                        {/if}
                        {#each blocks as {block, count} (block)}
                            <div class="block">
                                <img src={itemIconUrl(block)} alt={block}/>
                                <code class="count">{count}</code>
                            </div>
                        {/each}
                    {/each}
                {/if}
            </div>
        {/each}
    </div>
{/if}

<style lang="scss">
  @use "../../../colors" as *;

  .container {
    background-color: rgba($hotbar-base-color, 0.5);
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding: 4px;
    border-radius: 5px;
  }

  .row {
    display: flex;
    align-items: center;
    gap: 2px;

    .distance {
      width: 4ch;
      text-align: right;
      color: white;
    }

    .block {
      position: relative;
      width: 24px;
      height: 24px;

      .count {
        position: absolute;
        right: 0;
        bottom: 0;
        width: 2ch;
        text-align: right;
        color: white;
      }

      img {
        width: 100%;
        height: 100%;
      }
    }

    img.bed {
      width: 24px;
      height: 24px;
    }

    .direction {
      color: white;
    }

    hr {
      height: 18px;
    }
  }
</style>
