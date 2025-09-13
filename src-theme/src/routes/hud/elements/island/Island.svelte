<script lang="ts">
    import {onDestroy, onMount, tick} from "svelte";
    import {fade} from "svelte/transition";
    import {
        getClientInfo,
        getModuleSettings,
        getPlayerData,
        getPlayerInventory,
        getSession
    } from "../../../../integration/rest";
    import type {ClientInfo, ItemStack, PlayerData, Session} from "../../../../integration/types";
    import type {ClientPlayerDataEvent, ClientPlayerInventoryEvent, ProgressEvent,} from "../../../../integration/events";
    import {listen} from "../../../../integration/ws";
    import {Tween} from "svelte/motion";
    import {cubicOut} from "svelte/easing";
    import {
        armorDurabilityStore,
        armorThreshold,
        armorValue,
        blockCount,
        DURABILITY_RECOVERY,
        DURABILITY_THRESHOLD,
        emptySlotCount,
        targetId,
        totemCount
    } from './Island';
    import {get} from 'svelte/store';
    import {calcArmorValue} from "../../../../util/Client/calcArmorValue";
    import {clientName} from "../../../../util/Theme/ThemeManager";
    import {
        checkUsernameVisibility,
        nameProtect,
        NameProtectSetting,
        randomCode,
        showUsername,
        useGarbled
    } from "../../../../util/Theme/NameProtectManager";
    import {Interval} from "../../../../util/timeout_utils";
    import ItemStackView from "../../common/ItemView/ItemStackView.svelte";

    const ALERT_DISPLAY_DURATION_MS = 2500;
    const INVENTORY_FULL_COOLDOWN_MS = 30000;
    const UPDATE_INTERVAL_MS = 50;
    const ARMOR_ALERT_TARGET_COOLDOWN_MS = 60_000;
    const ANIMATION_DURATION_MS = 300;
    const DURABILITY_COOLDOWN_MS = 1000;
    const TOTEM_WARNING_COOLDOWN_MS = 5000;
    const PADDING = 32;
    const PADDING_SIDES = 64;

    const userData = JSON.parse(
        localStorage.getItem('userSettings') ||
        JSON.stringify({username: 'Customer'})
    );

    type AlertType =
        'health' | 'air' | 'blocks' | 'hunger' | 'uniform' | 'totem' |
        'saturation' | 'unbeatable' | 'durability' | 'inventory' | 'eating' | null;
    type AlertState = 'hidden' | 'showing' | 'hiding';
    type ContentType = 'alert' | 'greeting' | 'status' | 'chest';

    interface Alert {
        type: AlertType;
        title: string;
        message: string;
    }

    let lastTotemCount = 0;
    let totemWarningCooldown = false;

    let alertState: AlertState = 'hidden';
    let session: Session | null = null;

    let progressEvent: ProgressEvent | null = null;
    let playerData: PlayerData | null = null;

    let currentAlert: Alert | null = null;
    let alertHideTimeoutId: ReturnType<typeof setTimeout> | null = null;
    let lastInventoryFullAlertTime = 0;
    let eatingProgress = 0;
    let eatingDuration = 0;
    let chestWidth = 0;
    let chestMeasured = false;
    let isActuallyEating = false;
    let eatingAnimationFrame: number;
    let time = "";
    let timeGreeting = "";
    let lastHealthValue = 20;
    let lastAirValue = 300;
    let lastFoodValue = 20;
    let lastEatingStartTime = 0;
    let lastBlockValue: number | undefined = undefined;
    let initialAnimation = true;
    let initialAnimationDone = false;
    let timeLoaded = false;
    let isMounted = true;
    let currentContent: ContentType = 'greeting';
    let nextContent: ContentType | null = null;
    let nextContentWidth = 0;
    let openChest: ItemStack[] = [];
    let animationPhase: 'idle' | 'contract' | 'expand' = 'idle';
    let wrapper: HTMLDivElement | null = null;
    let clientInfo: ClientInfo | null = null;

    const lastArmorAlertTimes = new Map<string, number>();
    const warnedSlots = new Set<string>();
    const timeoutManager = new Interval();
    const warnTimestamps = new Map<string, number>();
    const contentRefs = {
        alert: null as HTMLDivElement | null,
        greeting: null as HTMLDivElement | null,
        status: null as HTMLDivElement | null,
        chest: null as HTMLDivElement | null
    };

    const initialWidth = new Tween(0, {duration: 400, easing: cubicOut});
    const initialOpacity = new Tween(0, {duration: 400, easing: cubicOut});
    const w = new Tween(400, {duration: 300, easing: cubicOut});
    const h = new Tween(40, {duration: 300, easing: cubicOut});

    const getTimeGreeting = (hours: number): string => {
        if (hours >= 5 && hours < 12) return "Good morning";
        if (hours >= 12 && hours < 18) return "Good afternoon";
        if (hours >= 18 && hours < 22) return "Good evening";
        return "Good night";
    };

    const formatTime = (hours: number, minutes: number): string => {
        return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}`;
    };

    const updateTime = (): void => {
        const now = new Date();
        const hours = now.getHours();
        const minutes = now.getMinutes();
        timeGreeting = getTimeGreeting(hours);
        time = formatTime(hours, minutes);
    };


    const clearAlertState = (): void => {
        if (alertHideTimeoutId) {
            clearTimeout(alertHideTimeoutId);
            alertHideTimeoutId = null;
        }
        animationPhase = 'idle';
    };

    const waitUntilNoAlert = async (): Promise<void> => {
        return new Promise<void>((resolve) => {
            const check = () => {
                if (!currentAlert) resolve();
                else setTimeout(check, 100);
            };
            check();
        });
    };

    const checkHealthAlert = (newHealth: number): void => {
        if ($totemCount >= 1) return;
        if (newHealth > 0 && newHealth <= 5 && lastHealthValue > 5) {
            showAlert('health', 'NearDeath', 'Your health is severely inadequate !');
        }
        lastHealthValue = newHealth;
    };

    const checkAirAlert = (newAir: number): void => {
        if (newAir <= 15 && lastAirValue > 15) {
            showAlert('air', 'Suffocating', 'Please emerge as soon as possible !');
        }
        lastAirValue = newAir;
    };

    const checkFoodAlert = (newFood: number): void => {
        if (newFood < 19 && lastFoodValue === 19) {
            showAlert('hunger', 'Cannot Heal', `Stop combat/food recovery shortly !`);
        } else if (newFood <= 7 && lastFoodValue > 7) {
            showAlert('saturation', 'Famine', `Your saturation is critically low (${newFood}/20)`);
        }
        lastFoodValue = newFood;
    };

    const checkTotemAlert = (newCount: number) => {
        if (newCount >= lastTotemCount) {
            lastTotemCount = newCount;
            return;
        }

        if (newCount === 1 && !totemWarningCooldown) {
            showAlert('totem', 'Periled', `You're only left with the last totem !`);
            totemWarningCooldown = true;
            setTimeout(() => totemWarningCooldown = false, TOTEM_WARNING_COOLDOWN_MS);
        }

        lastTotemCount = newCount;
    };

    const checkInventoryFullAlert = (emptySlots: number) => {
        const now = Date.now();
        if (emptySlots === 0 && now - lastInventoryFullAlertTime > INVENTORY_FULL_COOLDOWN_MS) {
            showAlert('inventory', 'Overburdened', 'You cannot bring anything further!');
            lastInventoryFullAlertTime = now;
        }
    };

    const checkArmorDurability = () => {
        const armor = get(armorDurabilityStore);
        const slots = ['helmet', 'chestplate', 'leggings', 'boots'] as const;
        const now = Date.now();

        slots.forEach(slot => {
            const item = armor[slot];
            if (!item) return;

            const name = typeof item.displayName === 'string'
                ? item.displayName
                : JSON.stringify(item.displayName);

            const ratio = item.durability / item.maxDurability;
            if (warnedSlots.has(slot)) {
                if (ratio > DURABILITY_RECOVERY) warnedSlots.delete(slot);
                return;
            }

            const lastWarnTime = warnTimestamps.get(slot) ?? 0;
            if (ratio <= DURABILITY_THRESHOLD && now - lastWarnTime > DURABILITY_COOLDOWN_MS) {
                showAlert('durability', 'Fragile', `${name} Remaining durability is ${Math.round(ratio * 100)}%`);
                warnedSlots.add(slot);
                warnTimestamps.set(slot, now);
            }
        });
    };

    const checkBlockAlert = (newBlock: number | undefined): void => {
        if (newBlock === undefined) return;
        if (newBlock < 16 && (lastBlockValue === undefined || lastBlockValue >= 16)) {
            showAlert('blocks', 'OutOfStock', `You've only got ${newBlock} usable blocks left!`);
        }
        lastBlockValue = newBlock;
    };

    const checkArmorAlert = (
        targetId: string,
        targetArmor: number | undefined,
        playerArmor: number
    ): void => {
        if (targetArmor === undefined) return;

        const now = Date.now();
        const baseThreshold = armorThreshold + playerArmor;
        const extraGap = 16;
        const lastTime = lastArmorAlertTimes.get(targetId) ?? 0;

        if (now - lastTime <= ARMOR_ALERT_TARGET_COOLDOWN_MS) return;

        if (targetArmor > baseThreshold + extraGap) {
            showAlert('unbeatable', 'Overpowered', `You're at an equipment disadvantage!`);
        } else if (targetArmor > baseThreshold) {
            showAlert('uniform', 'Formidable', `You're at an equipment disadvantage!`);
        }
        lastArmorAlertTimes.set(targetId, now);
    };

    const showAlert = (type: AlertType, title: string, message: string, duration?: number): void => {
        const doShow = () => doShowAlert(type, title, message, duration || (type === 'eating' ? 3000 : ALERT_DISPLAY_DURATION_MS));

        if (type === 'eating') return doShow();
        if (initialAnimation && !initialAnimationDone) waitForInitialAnimation(doShow);
        else doShow();
    };

    const waitForInitialAnimation = (callback: () => void) => {
        const check = () => {
            if (!initialAnimation || initialAnimationDone) callback();
            else setTimeout(check, 100);
        };
        check();
    };

    const doShowAlert = (
        type: AlertType,
        title: string,
        message: string,
        duration: number
    ): void => {
        clearAlertState();

        const applyAlert = () => {
            currentAlert = {type, title, message};
            currentContent = 'alert';
            animationPhase = 'expand';

            tick().then(() => {
                const targetEl = contentRefs.alert;
                nextContentWidth = targetEl ? targetEl.scrollWidth + PADDING_SIDES : 300;
                w.set(nextContentWidth);
                h.set(50);

                setTimeout(() => {
                    animationPhase = 'idle';
                    alertState = 'showing';
                    alertHideTimeoutId = setTimeout(() => hideAlert(), duration);
                }, ANIMATION_DURATION_MS);
            });
        };

        if (currentAlert) {
            animationPhase = 'contract';
            setTimeout(() => {
                applyAlert();
            }, ANIMATION_DURATION_MS);
        } else {
            applyAlert();
        }
    };

    const hideAlert = (): void => {
        if (alertState !== 'showing') return;
        clearAlertState();

        if (currentAlert?.type === 'eating') {
            alertState = 'hidden';
            currentAlert = null;
            animationPhase = 'idle';
            switchContent(openChest.length > 0 ? 'chest' : 'status');
        } else {
            alertState = 'hiding';
            animationPhase = 'contract';
            setTimeout(() => {
                currentAlert = null;
                alertState = 'hidden';
                animationPhase = 'idle';
                switchContent(openChest.length > 0 ? 'chest' : 'status');
            }, ANIMATION_DURATION_MS);
        }
    };

    const startEatingProgress = (duration: number) => {
        if (eatingAnimationFrame) cancelAnimationFrame(eatingAnimationFrame);

        eatingProgress = 0;
        const startTime = Date.now();
        const expectedTime = startTime + duration;

        const animate = () => {
            const now = Date.now();
            const remaining = expectedTime - now;

            if (remaining <= 0 || !isActuallyEating) {
                eatingProgress = 1;
                return;
            }

            eatingProgress = 1 - (remaining / duration);
            eatingAnimationFrame = requestAnimationFrame(animate);
        };

        eatingAnimationFrame = requestAnimationFrame(animate);
    };

    const switchContent = (type: ContentType) => {
        if (currentContent === type) return;
        nextContent = type;
        const targetEl = contentRefs[type];

        const baseWidth = contentRefs.status?.scrollWidth || 312;
        nextContentWidth = type === 'chest' ? baseWidth : (targetEl ? targetEl.scrollWidth + PADDING_SIDES : 300);
        animationPhase = 'contract';
        currentContent = type;
        w.set(nextContentWidth);
        h.set(type === 'alert' ? 50 : type === 'chest' ? (openChest.length > 27 ? 200 : 104) : 40);
        animationPhase = 'idle';
        nextContent = null;
    };
    const handleEatingState = (newData: PlayerData) => {
        const nowEating = newData.isEating;

        if (nowEating && newData.eatingStartTime !== lastEatingStartTime) {
            isActuallyEating = true;
            eatingDuration = newData.eatingMaxDuration * 50;
            lastEatingStartTime = newData.eatingStartTime;

            clearAlertState();
            showAlert('eating', 'Eating', 'Consuming food...', eatingDuration);
            startEatingProgress(eatingDuration);
        } else if (!nowEating && isActuallyEating) {
            isActuallyEating = false;
            if (currentAlert?.type === 'eating') hideAlert();
            eatingProgress = 0;
            if (eatingAnimationFrame) cancelAnimationFrame(eatingAnimationFrame);
        }
    };

    const updatePlayerData = async () => {
        const newData = await getPlayerData();
        if (!newData) return;

        if (newData.armorItems) {
            newData.armor = calcArmorValue(newData.armorItems);
        }

        handleEatingState(newData);
        checkHealthAlert(newData.actualHealth);
        checkAirAlert(newData.air);
        checkFoodAlert(newData.food);

        if ($armorValue !== undefined && typeof $targetId === 'string') {
            checkArmorAlert($targetId, $armorValue, newData.armor);
        }

        playerData = newData;
    };

    const updateClientInfo = async () => {
        clientInfo = await getClientInfo();
    };

    const updateAllData = async (): Promise<void> => {
        const newData = await getPlayerData();
        if (!newData) return;
        const inventory = await getPlayerInventory();
        openChest = inventory.openChest ?? [];
        session = await getSession();

        updateTime();
        timeLoaded = true;

        await updateClientInfo();
        await updatePlayerData();
        await checkUsernameVisibility();
        const settings = await getModuleSettings("NameProtect");
        NameProtectSetting(settings);
    };

    const handleInitialAnimationEnd = async () => {
        await waitUntilNoAlert();
        while (!session || !timeLoaded) await new Promise(res => setTimeout(res, 50));
        await tick();

        const greetingEl = contentRefs.greeting;
        while (!greetingEl || greetingEl.scrollWidth === 0) await tick();
        if (!isMounted) return;

        await new Promise(res => setTimeout(res, 1500));
        initialAnimation = false;
        initialAnimationDone = true;
        switchContent(openChest.length > 0 ? 'chest' : 'status');
    };

    $: loaded = timeLoaded && clientInfo;
    $: if ($showUsername || $useGarbled || $nameProtect) {
        tick().then(() => {
            const targetEl = contentRefs[currentContent];
            if (targetEl) {
                const contentWidth = targetEl.scrollWidth;
                w.set(contentWidth);
            }
        });
    }
    $: {
        if ($armorDurabilityStore) checkArmorDurability();
        if ($blockCount !== undefined) checkBlockAlert($blockCount);
        if ($armorValue !== undefined && typeof $targetId === 'string' && playerData?.armor !== undefined) {
            checkArmorAlert($targetId, $armorValue, playerData.armor);
        }
        if ($emptySlotCount !== undefined) checkInventoryFullAlert($emptySlotCount);
    }
    $: {
        if (nextContent) {

            w.set(
                nextContent === 'chest'
                    ? (contentRefs.chest?.scrollWidth || 312)
                    : nextContentWidth
            );

            h.set(
                nextContent === 'alert'
                    ? 50
                    : nextContent === 'chest'
                        ? (contentRefs.chest?.scrollHeight || 104) + PADDING
                        : 40
            );

        } else {
            const widthMap = {
                alert: 280 + PADDING,
                greeting: (contentRefs.greeting?.scrollWidth || 0) + PADDING,
                status: (contentRefs.status?.scrollWidth || 0) + PADDING,
                chest: chestMeasured ? chestWidth : (contentRefs.chest?.scrollWidth || 0) + PADDING
            };
            const targetWidth = currentContent === 'alert' && currentAlert?.type === 'eating'
                ? widthMap.alert
                : widthMap[currentContent];
            w.set(targetWidth);
            h.set(
                currentContent === 'alert'
                    ? 50
                    : currentContent === 'chest'
                        ? (contentRefs.chest?.scrollHeight || 104) + PADDING
                        : 40
            );
        }
    }
    $: if (currentContent === 'chest' && contentRefs.chest?.scrollWidth) {
        chestWidth = contentRefs.chest.scrollWidth;
        chestMeasured = true;
        w.set(chestWidth);
    }


    $: if (openChest.length > 0 && currentContent !== 'alert' && !initialAnimation && progressEvent?.title === "ChestStealer") {
        switchContent('chest');
    } else if (openChest.length === 0 && currentContent === 'chest' && !initialAnimation) {
        switchContent('status');
    }

    onMount(() => {
        isMounted = true;
        (async () => {
            await tick();
            await initialWidth.set((wrapper?.scrollWidth || 312) + PADDING_SIDES);
            await initialOpacity.set(1);
            await updateAllData().catch(console.error);
            await handleInitialAnimationEnd();
        })();

        const interval = setInterval(() => {
            if (isMounted) updateAllData().catch(console.error);
        }, UPDATE_INTERVAL_MS);

        return () => {
            isMounted = false;
            clearInterval(interval);
        };
    });

    onDestroy(() => {
        timeoutManager.clearAll();
        if (eatingAnimationFrame) cancelAnimationFrame(eatingAnimationFrame);
    });
    listen("progress", (e: ProgressEvent) => {
        progressEvent = e;

        if (e.progress >= e.maxProgress) {
            setTimeout(() => {
                progressEvent = null;
            }, 100);
        }
    });
    listen("clientPlayerData", async (event: ClientPlayerDataEvent) => {
        const newData = event.playerData;
        if (newData.armorItems) newData.armor = calcArmorValue(newData.armorItems);

        handleEatingState(newData);
        checkHealthAlert(newData.actualHealth);
        checkAirAlert(newData.air);
        checkFoodAlert(newData.food);
        checkArmorDurability();
        checkTotemAlert($totemCount);

        if ($armorValue !== undefined && typeof $targetId === 'string') {
            checkArmorAlert($targetId, $armorValue, newData.armor);
        }
        if ($blockCount !== undefined) checkBlockAlert($blockCount);

        playerData = newData;
    });
    listen("clientPlayerInventory", (data: ClientPlayerInventoryEvent) => {
        openChest = data.inventory.openChest ?? [];
    });
    listen("nameProtectValueChange", async () => {
        const settings = await getModuleSettings("NameProtect");
        NameProtectSetting(settings);
    });
</script>
{#if loaded  && playerData}
    <div class="dynamic-island-container">
        <div class="dynamic-island hud-container {alertState}"
             class:notification-active={currentAlert !== null}
             class:contract={animationPhase === 'contract'}
             class:expand={animationPhase === 'expand'}
             class:initial={initialAnimation}
             style="width: {initialAnimation ? initialWidth.current : w.current}px;
                height: {initialAnimation ? 40 : (h.current + (currentAlert ? 10 : 0))}px;
                opacity: {initialOpacity.current};
                transform-origin: top center;">
            <div class="content-wrapper" bind:this={wrapper}>
                {#if currentAlert && playerData.gameMode !== "creative"}
                    <div class="notification-content {currentAlert.type}"
                         in:fade={{ duration: 150 }}
                         bind:this={contentRefs.alert}>
                        <div class="icon">
                            <img src={`img/hud/island/${currentAlert.type || 'default'}.svg`} alt="icon"/>
                        </div>
                        <div class="text">
                            <div class="title">{currentAlert.title}</div>
                            <div class="description">{currentAlert.message}</div>
                        </div>
                        {#if currentAlert.type === 'eating'}
                            <div class="eating-progress-bar">
                                <div class="eating-progress-fill"
                                     style="width: {eatingProgress * 100}%;">
                                </div>
                            </div>
                        {:else}
                            <div class="progress-bar-container">
                                <div class="progress-bar {currentAlert.type}"></div>
                            </div>
                        {/if}
                    </div>
                {:else if currentContent === 'greeting'}
                    <div class="greeting-content"
                         in:fade={{ duration: 150 }}
                         bind:this={contentRefs.greeting}>
                        <span class="greeting">{timeGreeting}</span>
                        <span class="username">&nbsp;{userData.username}~</span>
                    </div>
                {:else if currentContent === 'chest' && playerData.gameMode !== "spectator"}
                    <div class="chest-content"
                         in:fade={{ duration: 150 }}
                         bind:this={contentRefs.chest}>
                        <div class="chest-container">
                            {#each openChest as stack (stack)}
                                <ItemStackView {stack}/>
                            {/each}
                        </div>
                    </div>
                {:else}
                    <div class="status-content"
                         in:fade={{ duration: 150 }}
                         bind:this={contentRefs.status}>
                        {#if timeLoaded && clientInfo}
                            <span class="client"> { $clientName || clientInfo.clientName }</span>
                            <div class="separator"></div>
                            <span class="time">{time}</span>
                            <div class="separator"></div>
                            <span class="fps">{clientInfo.fps}fps</span>
                            <div class="separator"></div>
                            <span class="username">
                              {#if $showUsername && session}
                                  {session.username}
                              {:else if $useGarbled}
                                  {$randomCode}
                              {:else}
                                  {$nameProtect}
                              {/if}
                            </span>
                        {/if}
                    </div>
                {/if}
            </div>
        </div>
    </div>
{/if}

<style lang="scss">
  @use "../../../../colors" as *;
  @use "sass:list";

  @mixin text-ellipsis {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  :root {

    --text-primary: rgba(255, 255, 255, 0.9);
  }

  .dynamic-island-container {
    display: flex;
    position: absolute;
    top: 5px;
    left: 50%;
    transform: translateX(-50%);
    perspective: 1000px;
    filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.3)) drop-shadow(0 8px 24px rgba(0, 0, 0, 0.2)) drop-shadow(0 16px 48px rgba(0, 0, 0, 0.15));
  }


  .dynamic-island {
    overflow: hidden;
    color: var(--text-primary);
    padding: 0 16px;
    border-radius:24px;
    display: flex;
    align-items: center;
    transition: width 0.3s cubic-bezier(0.25, 1, 0.5, 1),
    height 0.3s cubic-bezier(0.25, 1, 0.5, 1);
    transform-style: preserve-3d;

    transform-origin: top center;

    &.expand {
      border-radius: 16px;
    }

    &.showing {
      transition-timing-function: cubic-bezier(0.5, 0, 0.75, 0);
    }

    &.hiding {
      transition-timing-function: cubic-bezier(0.25, 1, 0.5, 1);
    }

    &.initial {
      transform-origin: top center;
      animation: initialExpand 0.5s cubic-bezier(0.2, 0, 0.1, 1) forwards;

      &:not(.showing):not(.hiding) {
        transform-origin: top center;
        animation: initialExpand 0.5s cubic-bezier(0.2, 0, 0.1, 1) forwards;
      }

      .greeting-content {
        justify-content: center;
        opacity: 0;
        animation: fadeIn 0.4s 0.3s forwards;
      }
    }
  }

  .content-wrapper {
    width: 100%;
    display: flex;
    align-items: center;
    overflow: hidden;
    position: relative;
  }

  .greeting-content,
  .status-content {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
    font-size: 14px;
    white-space: nowrap;

    .client, .username, .greeting, .time, .fps {
      font-size: 20px;
      letter-spacing: -0.25px;
      flex-shrink: 0;
      color: hsl(0, 0%, 90%);
      text-shadow: 0 0 3px rgba(255, 255, 255, 0.9);
      font-feature-settings: "tnum";
      font-variant-numeric: tabular-nums;
    }
    .username::after {
      content: "";
      display: inline-block;
      width: 32px;
    }

    .client {
      font-weight: bold;
      background-clip: text;
      background: linear-gradient(to right in oklch, var(--primary-color) 0%, var(--secondary-color) 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      text-shadow: 0 0 1px color-mix(in srgb, var(--primary-color) 30%, transparent);
    }

    .separator {
      width: 1px;
      height: 20px;
      background: linear-gradient(to bottom, transparent, rgba($text, 0.7), transparent);
      flex-shrink: 0;
      position: relative;
      opacity: 0.7;
      animation: fadeBreath 2s infinite ease-in-out;
    }
  }

  .chest-content {
    width: 100%;
    padding: 8px;
    display: flex;
    justify-content: center;
  }

  .chest-container {
    display: grid;
    grid-template-columns: repeat(9, 32px);
    gap: 4px;
    min-height: 104px;
    width: 100%;
    justify-content: center;
  }

  .notification-content {
    display: flex;
    align-items: center;
    gap: 12px;
    width: 100%;
    padding: 4px 0;
    position: relative;

    .icon {
      width: 24px;
      height: 24px;
      flex-shrink: 0;

      img {
        width: 100%;
        height: 100%;
        object-fit: contain;
      }
    }

    // 定义不同类型通知的颜色变量
    $notification-types: (
            health: (#ff453a, #ff8a80, 340deg, #ff453a),
            totem:(#ff453a, #ff8a80, 340deg, #ff453a),
            air: (#2e90bd, #7fd1ff, 180deg, #2e90bd),
            hunger: (#ff9f0a, #ffd60a, 30deg, #ff9f0a),
            durability: (#ff9f0a, #ffd60a, 30deg, #ff9f0a),
            inventory: (#ff9f0a, #ffd60a, 30deg, #ff9f0a),
            eating: (#ff9f0a, #ffd60a, 30deg, #ff9f0a),
            blocks: (#ff9f0a, #ffd60a, 30deg, #ff9f0a),
            uniform: (#ff9f0a, #ffd60a, 30deg, #ff9f0a),
            saturation: (#ff640a, #ffab5e, 10deg, #ff640a),
            unbeatable: (#ff640a, #ffab5e, 10deg, #ff640a)
    );

    // 循环生成不同类型通知的样式
    @each $type, $colors in $notification-types {
      $primary: list.nth($colors, 1);
      $secondary: list.nth($colors, 2);
      $hue-rotate: list.nth($colors, 3);
      $text-color: list.nth($colors, 4);

      &.#{$type} {
        .icon img {
          filter: brightness(0.8) saturate(200%) invert(25%) sepia(90%) saturate(2000%) hue-rotate($hue-rotate) drop-shadow(0 0 5px rgba($primary, 0.7));
        }

        .title {
          background: linear-gradient(90deg, $primary, $secondary);
          -webkit-background-clip: text;
          background-clip: text;
          color: transparent;
        }

        .progress-bar {
          background: linear-gradient(90deg, $primary, $secondary);
        }

        .eating-progress-bar {
          position: absolute;
          bottom: 0;
          left: 0;
          width: 100%;
          height: 3px;
          background: rgba(0, 0, 0, 0.2);
          border-radius: 0 0 16px 16px;
          overflow: hidden;

          .eating-progress-fill {
            height: 100%;
            transition: width 0.1s linear;
            background: linear-gradient(90deg, $primary, $secondary);
          }
        }
      }
    }


    .progress-bar-container {
      position: absolute;
      bottom: 0;
      left: 0;
      width: 100%;
      height: 4px;
      background: rgba(0, 0, 0, 0.3);
      border-radius: 0 0 16px 16px;


      .progress-bar {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        transform-origin: left center;
        animation: progress 3s linear forwards;
        box-shadow: 0 0 10px currentColor;
      }
    }

    .text {
      flex: 1;
      min-width: 0;

      .title {
        font-weight: 700;
        font-size: 16px;
        line-height: 1.2;
        @include text-ellipsis;
        text-shadow: 0 0 5px currentColor;
        animation: textGlow 2s infinite alternate;
      }

      .description {
        font-size: 14px;
        color: rgba($text, 0.9);
        @include text-ellipsis;
        margin-top: 2px;
        text-shadow: 0 0 3px rgba(255, 255, 255, 0.5);
      }
    }
  }

  @keyframes progress {
    from {
      transform: scaleX(1);
    }
    to {
      transform: scaleX(0);
    }
  }

  @keyframes initialExpand {
    0% {
      width: 0;
      opacity: 0;
      transform: scaleX(0.1);
    }
    70% {
      opacity: 1;
      transform: scaleX(1.1);
    }
    100% {
      transform: scaleX(1);
      opacity: 1;
    }
  }

  @keyframes fadeBreath {
    0%, 100% {
      opacity: 0.5;
    }
    50% {
      opacity: 0.9;
    }
  }

  @keyframes fadeIn {
    from {
      opacity: 0;
    }
    to {
      opacity: 1;
    }
  }

  @keyframes textGlow {
    0% {
      text-shadow: 0 0 5px currentColor;
    }
    100% {
      text-shadow: 0 0 10px currentColor;
    }
  }
</style>
