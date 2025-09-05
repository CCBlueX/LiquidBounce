<script lang="ts">
    import {onMount, afterUpdate} from 'svelte';
    import {listen} from '../../../../integration/ws';
    import TextComponent from '../../../menu/common/TextComponent.svelte';
    import {type KeyEvent, type OverlayChatEvent, ChatType, type EventMap} from '../../../../integration/events';
    import {getMinecraftKeybinds} from '../../../../integration/rest';
    import type {MinecraftKeybind} from '../../../../integration/types';
    import {fly} from 'svelte/transition';
    import {Tween} from 'svelte/motion';
    import {cubicOut, expoInOut} from 'svelte/easing';

    const MAX_MESSAGES = 50;
    const MAX_DISPLAYED = 25;
    const HEIGHT_FOCUSED = 500;
    const HEIGHT_BLUR = 312;
    const FADE_DURATION = 5000;
    const MAX_SIMULTANEOUS_FADES = 3;
    const FADE_DELAY_BETWEEN_BATCHES = 800;

    $: chatHeight.set(focus ? HEIGHT_FOCUSED : HEIGHT_BLUR);
    const chatHeight = new Tween(HEIGHT_BLUR, {
        duration: 300,
        easing: cubicOut
    });
    let fadeQueue: typeof chatMessages[number][] = [];
    let activeFadeCount = 0;
    let focus = false;
    let chatMessages: Array<OverlayChatEvent & {
        id: number;
        visible: boolean;
        fadeTimeout?: number;
    }> = [];
    let container: HTMLDivElement;
    let nextId = 0;
    let isAtBottom = true;
    let keyChat: MinecraftKeybind | undefined;
    let initialized = false;


    let hasVisibleMessages = false;
    $: {
        if (focus) {

            hasVisibleMessages = true;
        } else {

            const lastMessages = chatMessages.slice(-MAX_DISPLAYED);
            hasVisibleMessages = lastMessages.some(msg => msg.visible);
        }
    }

    function clearAllFadeTimeouts() {
        chatMessages.forEach(msg => {
            if (msg.fadeTimeout) {
                clearTimeout(msg.fadeTimeout);
                msg.fadeTimeout = undefined;
            }
        });
        fadeQueue = [];
        activeFadeCount = 0;
    }

    function maybeScrollToBottomImmediately() {
        if (!initialized && container) {
            container.scrollTop = container.scrollHeight;
            isAtBottom = true;
            initialized = true;
        }
    }

    function scheduleFade(msg: typeof chatMessages[number]) {
        if (focus || msg.fadeTimeout || fadeQueue.includes(msg)) return;

        fadeQueue.push(msg);
        processFadeQueue();
    }

    function processFadeQueue() {
        if (activeFadeCount >= MAX_SIMULTANEOUS_FADES || fadeQueue.length === 0) return;

        const msg = fadeQueue.shift();
        if (!msg) return;

        activeFadeCount++;

        msg.fadeTimeout = window.setTimeout(() => {
            msg.visible = false;
            chatMessages = [...chatMessages];
            msg.fadeTimeout = undefined;
            activeFadeCount--;

            setTimeout(() => {
                processFadeQueue();
            }, FADE_DELAY_BETWEEN_BATCHES);
        }, FADE_DURATION);
    }

    $: if (initialized) {
        if (focus) {
            clearAllFadeTimeouts();
            chatMessages.forEach(msg => msg.visible = true);
        } else {

            const windowMsgs = chatMessages
                .slice(-MAX_DISPLAYED)
                .filter(msg => msg.visible);
            windowMsgs.forEach(msg => scheduleFade(msg));
        }
    }

    async function updateKeybinds() {
        const binds = await getMinecraftKeybinds();
        keyChat = binds.find(k => k.bindName === 'key.chat');
    }


    function addMessage(event: OverlayChatEvent) {
        const msg = {...event, id: nextId++, visible: true} as typeof chatMessages[0];
        chatMessages = [...chatMessages, msg].slice(-MAX_MESSAGES);
        maybeScrollToBottomImmediately();

        if (initialized && !focus &&
            chatMessages.slice(-MAX_DISPLAYED).includes(msg)
        ) {
            scheduleFade(msg);
        }
    }

    function handleKeyDown(event: KeyEvent) {
        const k = event.key;
        if (k === keyChat?.key.translationKey) {
            const newFocus = event.action === 1 || event.action === 2;
            if (focus !== newFocus) {
                focus = newFocus;
            }
            return;
        }
        if (k === 'key.keyboard.slash') {
            focus = true;
            return;
        }
        if (k === 'key.keyboard.escape' || k === 'key.keyboard.enter') {
            focus = false;
            return;
        }
    }

    function handleWheel(e: WheelEvent) {
        e.preventDefault();
        container.scrollTop += e.deltaY;
        isAtBottom = container.scrollTop + container.clientHeight >= container.scrollHeight;
    }

    onMount(() => {
        updateKeybinds();
        listen('overlayChatMessage', addMessage);
        listen('key', handleKeyDown);
    });
    listen("keybindChange" as keyof EventMap, updateKeybinds);
    afterUpdate(() => {
        if (isAtBottom) {
            container.scrollTop = container.scrollHeight;
        }
    });
</script>

<div
        class="chat-hud {focus ? 'focused' : ''} {hasVisibleMessages ? 'visible' : 'hidden'}"
        style="
    --chat-width: 700px;
    --chat-height: {chatHeight.current}px;
    --line-height: 20px;
    --chat-scale: 1;
    --bg-opacity: 0.5
  "
>
    <div bind:this={container} class="messages-container "

         on:wheel={handleWheel}>
        {#each (focus ? chatMessages : chatMessages.slice(-MAX_DISPLAYED)) as msg (msg.id)}
            {#if msg.visible}

                <div
                        class="chat-line"
                        transition:fly|global={{duration: 500, x: -150, easing: expoInOut}}>
                    <span class="message-textData">
            <TextComponent
                    textComponent={msg.content}
                    fontSize={16}
                    allowPreformatting
            />
          </span>
                </div>
            {/if}
        {/each}
    </div>
</div>
<style lang="scss">
  @use '../../../../colors' as *;

  .chat-hud {
    position: absolute;
    bottom: 0;
    left: 0;
    width: var(--chat-width);
    max-height: var(--chat-height);
    border-radius: 8px;
    box-shadow: 0 4px 16px rgba($base, 0.6),
    inset 0 0 10px rgba(255, 255, 255, 0.05);
    overflow: hidden;
    transition: background-color 0.3s ease,
    transform 0.3s ease;
    transform-origin: left bottom;
    pointer-events: auto;
    will-change: transform, max-height;
  }

  .chat-hud.hidden {
    pointer-events: none;
    opacity: 0;
    transition: opacity 0.8s ease;
  }

  .chat-hud.visible {
    opacity: 1;
    transition: opacity 0.2s ease;
  }

  .chat-hud.focused {
    transition: box-shadow 0.3s ease;
    box-shadow: var(--box-shadow);
  }

  .messages-container {
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding: 8px;
    transition: background-color 0.3s ease;
    max-height: calc(var(--chat-height) - 16px);
    overflow: hidden;
    background-color: rgba($base, var(--bg-opacity));
    transform: scale(var(--chat-scale));
    scroll-behavior: smooth;
  }

  .chat-line {
    display: flex;
    gap: 6px;
    font-family: 'Alibaba', sans-serif;
    font-size: calc(var(--line-height) * 0.9);
    line-height: 1.4;
    opacity: 1;
    transition: opacity 0.3s ease;
  }

  .message-textData {
    flex: 1;
    white-space: pre-wrap;
    word-break: break-word;
  }
</style>
