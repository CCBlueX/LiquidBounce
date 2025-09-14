<script lang="ts">
    import {listen} from "../../../../../integration/ws.js";
    import type {PlayerData} from "../../../../../integration/types";
    import type {TargetChangeEvent} from "../../../../../integration/events";
    import {REST_BASE} from "../../../../../integration/host";
    import AvatarView from "../../../common/PlayerView/AvatarView.svelte";
    import {removeColorCodes} from "../../../../../util/color_utils";
    import {popOut, popIn} from "../../../../../util/animate_utils";
    import {visible} from "../TargetHud";

    let target: PlayerData | null = null;
    let distant = 0;
    let displayHealth = 0;
    let previousDisplayHealth = 0;

    function updateHealthAnimation() {
        const step = () => {
            const diff = (target?.actualHealth ?? 0) - displayHealth;
            displayHealth += diff * 0.15;

            const prevDiff = displayHealth - previousDisplayHealth;
            previousDisplayHealth += prevDiff * 0.1;

            requestAnimationFrame(step);
        };
        step();
    }

    updateHealthAnimation();

    listen("targetChange", (data: TargetChangeEvent) => {
        target = data.target;
        distant = data.distant;
    });
</script>

{#if $visible && target}
    {#if !target.isDead}
        <div class="targethud hud-container"
             in:popIn|global={{ duration: 400 }}
             out:popOut|global={{ duration: 300 }}
        >
            <div class="main-wrapper">
                <div class="avatar">
                    <div class="avatar-inner">
                        <AvatarView skinUrl={`${REST_BASE}/api/v1/client/resource/skin?uuid=${target.uuid}`}/>
                    </div>
                </div>
                <div class="info-container">
                    <div class="info">Name: {removeColorCodes(target.username)}</div>
                    <div class="info">Distance: {distant.toFixed(1)}</div>
                    <div class="info">Health: {target.health.toFixed(1)}</div>
                </div>
            </div>

            <div class="health-container">
                <div class="health-bg">
                    <div
                            class="health-fill previous-health"
                            style="width: {Math.min(Math.floor((previousDisplayHealth / (target.maxHealth + target.absorption)) * 100), 100)}%;"
                    ></div>
                    <div
                            class="health-fill"
                            style="width: {Math.min(Math.floor((displayHealth / (target.maxHealth + target.absorption)) * 100), 100)}%;"
                    ></div>
                </div>
            </div>
        </div>
    {/if}
{/if}

<style lang="scss">
  @use "../../../../../colors" as *;

  ;
  .targethud {
    color: $text-color;
    font-size: 14px;
  }

  .main-wrapper {
    display: flex;
    align-items: flex-start;
    gap: 10px;

  }

  .avatar {
    width: 50px;
    height: 50px;
    position: relative;

    .avatar-inner {
      position: absolute;
      top: 45%;
      left: 50%;
      transform: scale(5.7);
      transform-origin: center center;
      filter: drop-shadow(0 0 4px rgba($base, 0.5));
    }
  }

  .info-container {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }

  .info {
    font-weight: 500;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .health-container {
    position: relative;
    margin-top: 5px;
    height: 16px;
  }

  .health-bg {
    position: relative;
    height: 8px;
    border-radius: 2px;
    overflow: hidden;
    background-color: rgba(0, 0, 0, 0.3);
    box-shadow: 0 0 8px rgba(black, 0.2),
    0 0 12px rgba(black, 0.3),
    0 0 20px rgba(black, 0.3),
    0 0 24px rgba(black, 0.2);
  }

  .previous-health {
    position: absolute;
    top: 0;
    left: 0;
    height: 100%;
    background: linear-gradient(
                    135deg in oklch,
                    color-mix(in srgb, var(--primary-color) 50%, transparent) 10%,
                    color-mix(in srgb, var(--secondary-color) 50%, transparent) 90%
    );
    opacity: 0.5;
    border-radius: 2px;
  }

  .health-fill {
    position: absolute;
    top: 0;
    left: 0;
    height: 100%;
    background: linear-gradient(
                    135deg in oklch,
                    color-mix(in srgb, var(--primary-color) 80%, transparent) 10%,
                    color-mix(in srgb, var(--secondary-color) 80%, transparent) 90%
    );
    border-radius: 2px;
    transition: width 0.1s ease;
  }

</style>
