<script lang="ts">
    import type {PlayerData} from "../../../../../integration/types";
    import type {TargetChangeEvent} from "../../../../../integration/events.js";
    import {listen} from "../../../../../integration/ws.js";
    import {calcArmorValue} from "../../../../../util/Client/calcArmorValue"
    import {armorValue, targetId} from '../../island/Island';
    import {REST_BASE} from "../../../../../integration/host";
    import {popOut, popIn} from "../../../../../util/animate_utils";
    import {removeColorCodes} from "../../../../../util/color_utils";
    import {onMount, onDestroy} from "svelte";
    import {calcArmorColor, detectTeamColor, type TeamColor} from "../../../../../util/Client/calcArmorColor";
    import ArmorStatus from "../../../common/ItemView/ArmorStatus.svelte";
    import AvatarView from "../../../common/PlayerView/AvatarView.svelte";
    import {primaryColor, secondaryColor} from '../../../../../util/Theme/ThemeManager';
    import {get} from "svelte/store";
    import {visible} from "../TargetHud";


    let displayHealth = 0;
    let animationFrameId: number | null = null;
    let target: PlayerData | null = null;
    let shrink = false;
    let lastNotifiedArmor: number | undefined;
    let clearArmorTimer: ReturnType<typeof setTimeout>;
    let teamColor: TeamColor = null;
    let teamColorHex: string | null = null;
    let lastHealth: number | null = null;
    let attacked = false;
    let particleId = 0;
    let particles: Particle[] = [];
    let animationFrame: number;
    let simulatedHurtTime = 0;
    let hurtTimeTick: ReturnType<typeof setInterval>;
    let lastAttackTime = 0;
    let lastParticleSpawnTime = 0;
    const ATTACK_COOLDOWN = 450;
    const MAX_PARTICLES = 50;
    const PARTICLE_LIFETIME = 2000;
    const PARTICLE_FADE_TIME = 500;
    const PARTICLE_COUNT = 20;
    const PARTICLE_SPAWN_COOLDOWN = 470;
    const CLEAR_DELAY = 10_000;

    interface Particle {
        id: number;
        x: number;
        y: number;
        vx: number;
        vy: number;
        size: number;
        opacity: number;
        color: string;
        bornAt: number;
        fadeStart: number;
        shadowColor: string;
    }

    function animateHealth(targetHealth: number) {
        if (animationFrameId) {
            cancelAnimationFrame(animationFrameId);
        }
        const animate = () => {
            const diff = targetHealth - displayHealth;
            if (Math.abs(diff) < 0.05) {
                displayHealth = targetHealth;
                return;
            }
            displayHealth += diff * 0.1;
            animationFrameId = requestAnimationFrame(animate);
        };
        animate();
    }



    function getRandomThemeColor(): string {

        const pc = get(primaryColor);
        const sc = get(secondaryColor);


        const [r1, g1, b1] = pc.rgb.split(',').map((n) => Number(n.trim()));
        const [r2, g2, b2] = sc.rgb.split(',').map((n) => Number(n.trim()));

        const ratio = Math.random();
        const r = Math.round(r1 + (r2 - r1) * ratio);
        const g = Math.round(g1 + (g2 - g1) * ratio);
        const b = Math.round(b1 + (b2 - b1) * ratio);

        return `rgb(${r}, ${g}, ${b})`;
    }

    function spawnParticles(hurtTimeTick = 1) {
        const now = Date.now();
        shrink = true;
        setTimeout(() => {
            shrink = false
        }, 450);
        if (now - lastParticleSpawnTime < PARTICLE_SPAWN_COOLDOWN) return;
        lastParticleSpawnTime = now;

        const avatar = document.querySelector('.avatar') as HTMLElement;
        const hud = document.querySelector('.targethud') as HTMLElement;
        if (!avatar || !hud) return;

        particles = particles.filter(p => now - p.bornAt < PARTICLE_LIFETIME);
        const availableSlots = MAX_PARTICLES - particles.length;

        const maxAllowedPerSpawn = Math.floor(MAX_PARTICLES / 2);
        const count = Math.min(
            Math.max(Math.floor(PARTICLE_COUNT * hurtTimeTick), 2),
            availableSlots,
            maxAllowedPerSpawn
        );
        const avatarRect = avatar.getBoundingClientRect();
        const hudRect = hud.getBoundingClientRect();
        const globalOffset = 16;

        const centerX = avatarRect.left - hudRect.left + avatarRect.width / 2 + globalOffset;
        const centerY = avatarRect.top - hudRect.top + avatarRect.height / 2 + globalOffset;

        for (let i = 0; i < count; i++) {
            const angle = Math.random() * Math.PI * 2;
            const speed = 0.5 + Math.random() * 2 * hurtTimeTick;
            const size = 3 + Math.random() * 7;
            const lifetimeOffset = Math.random() * 500;
            const particleColor = getRandomThemeColor();

            particles.push({
                id: particleId++,
                x: centerX,
                y: centerY,
                vx: Math.cos(angle) * speed,
                vy: Math.sin(angle) * speed,
                size,
                opacity: 1,
                color: particleColor,
                shadowColor: particleColor,
                bornAt: now,
                fadeStart: now + PARTICLE_LIFETIME - PARTICLE_FADE_TIME - lifetimeOffset
            });
        }
    }

    function updateParticles() {
        const now = Date.now();
        particles = particles.map(p => {

            const lifeProgress = (now - p.bornAt) / PARTICLE_LIFETIME;


            const newX = p.x + p.vx;
            const newY = p.y + p.vy;

            let opacity = p.opacity;
            if (now > p.fadeStart) {
                opacity = 1 - (now - p.fadeStart) / PARTICLE_FADE_TIME;
            }


            let size = p.size;
            if (lifeProgress > 0.8) {
                const shrinkProgress = (lifeProgress - 0.8) / 0.2;
                size = p.size * (1 - shrinkProgress * 0.7);
            }

            return {
                ...p,
                x: newX,
                y: newY,
                opacity: Math.max(opacity, 0),
                size: Math.max(size, 0.1)
            };
        }).filter(p => now - p.bornAt < PARTICLE_LIFETIME && p.opacity > 0.01);

        animationFrame = requestAnimationFrame(updateParticles);
    }

    function updateTargetArmorValue(event: TargetChangeEvent) {
        if (!event.target) return;

        event.target.armor = calcArmorValue(event.target.armorItems);
    }

    onMount(() => {
        updateParticles();
        spawnParticles(1);
        hurtTimeTick = setInterval(() => {
            if (simulatedHurtTime > 0) {
                simulatedHurtTime -= 1;
            }
        }, 50);
    });
    onDestroy(() => {
        cancelAnimationFrame(animationFrame);
        clearInterval(hurtTimeTick);
    });



    listen("targetChange", (data: TargetChangeEvent) => {
        const now = Date.now();
        const t = data.target;
        const newTarget = data.target;
        target = newTarget;
        targetId.set(t?.username ?? null);
        const newArmor = t
            ? calcArmorValue(t.armorItems)
            : undefined;
        clearTimeout(clearArmorTimer);
        clearArmorTimer = setTimeout(() => {
            armorValue.set(undefined);
            lastNotifiedArmor = undefined;
        }, CLEAR_DELAY);
        if (newArmor !== lastNotifiedArmor) {
            armorValue.set(newArmor);
            lastNotifiedArmor = newArmor;
        }
        if (newTarget) {
            updateTargetArmorValue(data);
            const armorColor = calcArmorColor(newTarget.armorItems);
            teamColor = detectTeamColor(armorColor);
            teamColorHex = armorColor ? `#${armorColor.toString(16).padStart(6, '0')}` : null;
        }
        if (newTarget) {
            updateTargetArmorValue(data);
            armorValue.set(Math.floor(newTarget.armor));

            if (now - lastAttackTime >= ATTACK_COOLDOWN) {
                attacked = true;
                setTimeout(() => attacked = false, 450);
                lastAttackTime = now;
            }

            if (lastHealth !== null && newTarget.actualHealth < lastHealth) {
                simulatedHurtTime = Math.max(simulatedHurtTime, 10);
                const avatar = document.querySelector('.avatar') as HTMLElement | null;
                if (avatar) {
                    avatar.classList.remove('hurt');
                    void avatar.offsetWidth;
                    avatar.classList.add('hurt');
                }
                const damage = lastHealth - newTarget.actualHealth;
                spawnParticles(Math.min(damage / 10, 5));
            }

            animateHealth(newTarget.actualHealth);
        } else {
            armorValue.set(undefined);
            displayHealth = 0;
        }

        lastHealth = newTarget?.actualHealth ?? null;
    });


</script>
<div class="targethud-container" class:draggable={!$visible && !target}>
    {#if $visible && target}
        {#if !target.isDead}
            <div
                    class="targethud"
                    in:popIn|global={{ duration: 400 }}
                    out:popOut|global={{ duration: 300 }}
            >
                <div class="main-wrapper">

                    <!-- AvatarView -->
                    <div class="avatar {shrink ? 'shrink' : ''} {attacked ? 'attacked' : ''}">
                        <div class="avatar-inner">
                            <AvatarView skinUrl={`${REST_BASE}/api/v1/client/resource/skin?uuid=${target.uuid}`}/>
                        </div>
                    </div>
                    <!-- Name -->
                    <div class="name" style={`--team-color: ${teamColorHex}`}>
                        {removeColorCodes(target.username)}
                    </div>

                    <div class="armor-stats">
                        {#if target.offHandStack?.identifier && !target.offHandStack.identifier.includes('air')}
                            <ArmorStatus itemStack={target.offHandStack}/>
                        {/if}
                        {#if target.mainHandStack?.identifier && !target.mainHandStack.identifier.includes('air')}
                            <ArmorStatus itemStack={target.mainHandStack}/>
                        {/if}
                        {#if target.armorItems[3].count > 0}
                            <ArmorStatus itemStack={target.armorItems[3]}/>
                        {/if}
                        {#if target.armorItems[2].count > 0}
                            <ArmorStatus itemStack={target.armorItems[2]}/>
                        {/if}
                        {#if target.armorItems[1].count > 0}
                            <ArmorStatus itemStack={target.armorItems[1]}/>
                        {/if}
                        {#if target.armorItems[0].count > 0}
                            <ArmorStatus itemStack={target.armorItems[0]}/>
                        {/if}
                    </div>
                </div>
                <div class="health-container">
                    <div class="health-bg">
                        {#if target}
                            <div
                                    class="health-fill"
                                    style="width: {Math.floor((displayHealth / (target.maxHealth + target.absorption)) * 100)}%;"
                            >
            <span
                    class="health-text"
                    style="opacity: {displayHealth > 0 ? simulatedHurtTime / 10 : 0};
                   transition: opacity 0.3s ease;"
            >
            {displayHealth % 1 === 0 ? displayHealth.toFixed(0) : displayHealth.toFixed(1)}
          </span>
                            </div>
                        {/if}
                    </div>
                </div>
            </div>
        {/if}
        <div class="particles-container">
            {#each particles as p (p.id)}
                <div
                        class="particle {
          p.size <= 3 ? 'small' :
          p.size <= 5 ? 'medium' :
          'large'
      }"
                        style="
              left: {p.x}px;
              top: {p.y}px;
              width: {p.size}px;
              height: {p.size}px;
              background-color: {p.color};
              opacity: {p.opacity};
              --shadow-color: {p.shadowColor};
          "
                ></div>
            {/each}
        </div>

    {:else}
        <div class="empty-placeholder"></div>
    {/if}
</div>

<style lang="scss">
  @import "../../../../../colors";

  .targethud-container {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    position: absolute;
    min-height: 120px;
    min-width: 270px;
    border: 6px dashed transparent;
    border-radius: 20px;
    transition: background-color, border-color 0.3s ease;

    &:hover {
      background: rgba(204, 204, 204, 0.2);
      border-color: #ccc;
    }

    .empty-placeholder {
      display: none;
    }

    &.draggable {
      cursor: move;

      &:hover {
        border-color: rgba(255, 255, 255, 0.8) !important;
        background: rgba(204, 204, 204, 0.3);
      }
    }
  }

  .targethud {
    position: relative;
    width: 270px;
    border-radius: 20px;
    padding: 10px 0 20px;
    box-shadow: 0 0 20px rgba(0, 0, 0, 0.7),
    0 0 40px rgba(255, 255, 255, 0.05),
    inset 0 0 12px rgba(255, 255, 255, 0.04);
    will-change: transform, opacity;
  }

  .targethud::after {
    content: "";
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;

    transform: rotate(30deg);
    animation: shine 6s infinite;
    z-index: 1;
  }

  .targethud::before {
    content: "";
    position: absolute;
    inset: 0;
    background: linear-gradient(135deg,
            rgba(255, 255, 255, 0.05) 0%,
            rgba(0, 0, 0, 0.3) 50%,
            rgba(255, 255, 255, 0.03) 100%
    );
    border-radius: inherit;
    z-index: 0;
  }

  .main-wrapper {
    position: relative;
    z-index: 1;
    display: grid;
    grid-template-areas:
    "avatar name"
    "avatar armor";
    grid-template-columns: 50px 1fr;
    column-gap: 10px;
    row-gap: 4px;
    padding: 10px 8px 16px 10px;
  }

  .avatar {
    position: relative;
    grid-area: avatar;
    width: 50px;
    height: 50px;

    .avatar-inner {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: scale(5.5);
      transform-origin: center center;
      filter: drop-shadow(0 0 4px rgba($base, 0.5));
    }

    &.shrink .avatar-inner {
      animation: hitScale 0.4s ease-out forwards,
      highFlash 0.4s ease-out;
    }
  }

  .name {
    grid-area: name;
    padding-left: 4px;
    font-size: 16px;
    font-weight: bold;
    color: $text-color;
    text-shadow: 0 0 4px rgba($text-color, 0.9);
    align-self: start;
    letter-spacing: 1px;
    display: flex;
    align-items: center;
    gap: 4px;
/*
    .flag {
      width: 16px;
      height: 16px;
      mask: url("/img/hud/targethud/icon-flag.svg") no-repeat center;
      -webkit-mask: url("/img/hud/targethud/icon-flag.svg") no-repeat center;
      -webkit-mask-size: contain;
      mask-size: contain;
      filter: drop-shadow(0 2px 10px color-mix(in srgb, var(--team-color) 30%, transparent));
      background-color: var(--team-color);
      flex-shrink: 0;
    }
*/
  }

  .armor-stats {
    grid-area: armor;
    display: flex;
    align-items: center;
    position: absolute;
    margin: 0;
    padding: 0;
    gap: 1px;
    justify-content: flex-start;
  }

  .health-container {
    position: absolute;
    bottom: 6px;
    left: 10px;
    right: 16px;
    height: 16px;
    max-width: 95%;
  }

  .health-bg {
    position: relative;
    height: 5px;
    border-radius: 4px;
    overflow: visible;
    max-width: 95%;
    background-color: rgba(0, 0, 0, 0.3);
    box-shadow: 0 0 8px rgba(black, 0.2),
    0 0 12px rgba(black, 0.3),
    0 0 20px rgba(black, 0.3),
    0 0 24px rgba(black, 0.2);
  }

  .health-fill {
    position: relative;
    height: 100%;
    background: linear-gradient(
                    135deg in oklch,
                    color-mix(in srgb, var(--primary-color) 80%, transparent) 10%,
                    color-mix(in srgb, var(--secondary-color) 80%, transparent) 90%
    );
    border-radius: 4px;
    transition: width 0.1s ease;
    max-width: 95%;
    min-width: 54px;
  }

  .health-fill::before {
    content: "";
    position: absolute;
    inset: 0;
    border-radius: inherit;
    box-shadow: 0 0 4px color-mix(in srgb, var(--primary-color) 20%, transparent),
    0 0 6px color-mix(in srgb, var(--primary-color) 30%, transparent),
    0 0 10px color-mix(in srgb, var(--secondary-color) 30%, transparent),
    0 0 14px color-mix(in srgb, var(--secondary-color) 20%, transparent);
    z-index: -1;
  }

  .health-text {
    position: absolute;
    left: 100%;
    margin-left: 8px;
    bottom: 0;
    transform: translateY(25%);
    font-size: 14px;
    color: white;
    transition: opacity 0.3s ease;
    white-space: nowrap;
    text-shadow: 0 0 2px black;
    pointer-events: none;
    opacity: 1;
    max-width: 60px;
    text-overflow: ellipsis;
    overflow: hidden;
  }

  .particles-container {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    z-index: 10;
  }

  .particle {
    position: absolute;
    pointer-events: none;
    transform: translate(-50%, -50%);
    z-index: 20;
    transition: opacity 0.2s linear;

    box-shadow: 0 0 2px 1px color-mix(in srgb, var(--shadow-color) 60%, transparent);
  }

  .particle.small {
    box-shadow: 0 0 4px 2px color-mix(in srgb, var(--shadow-color) 80%, transparent),
    0 0 8px 3px color-mix(in srgb, var(--shadow-color) 30%, transparent);
  }

  .particle.medium {
    box-shadow: 0 0 8px 4px color-mix(in srgb, var(--shadow-color) 60%, transparent),
    0 0 16px 6px color-mix(in srgb, var(--shadow-color) 20%, transparent);
  }

  .particle.large {
    box-shadow: 0 0 12px 6px color-mix(in srgb, var(--shadow-color) 50%, transparent),
    0 0 24px 10px color-mix(in srgb, var(--shadow-color) 15%, transparent);
  }

  @keyframes hitScale {
    0% {
      transform: scale(5.5);
    }
    50% {
      transform: scale(6);
    }
    80% {
      transform: scale(5.3);
    }
    100% {
      transform: scale(5.5);
    }
  }

  @keyframes highFlash {
    0%, 100% {
      filter: drop-shadow(0 0 4px rgba($base, 0.5)) brightness(1);
    }
    50% {
      filter: drop-shadow(0 0 4px rgba($base, 0.8)) brightness(1.3) saturate(2) hue-rotate(0deg);

    }
  }


  @keyframes shine {
    0%, 100% {
      opacity: 0;
    }
    50% {
      opacity: 0.8;
    }
  }
</style>
