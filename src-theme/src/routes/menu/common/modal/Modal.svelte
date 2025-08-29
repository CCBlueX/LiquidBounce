<script lang="ts">
    import {createEventDispatcher, onMount, onDestroy} from "svelte";
    import {cubicOut} from "svelte/easing";
    import {fade, scale} from "svelte/transition";

    export let title: string;
    export let visible: boolean;

    const dispatch = createEventDispatcher();

    let isDragging = false;
    let offsetX = 0;
    let offsetY = 0;
    let modal: HTMLDivElement | null = null;
    let titleBar: HTMLDivElement | null = null;

    function handleClick() {
        closeModal();
    }

    function closeModal() {
        dispatch("close");
        visible = false;
    }

    function handleKeyDown(event: KeyboardEvent) {
        if (event.key === "Tab") {
            closeModal();
        }
    }


    function startDrag(event: MouseEvent) {
        if (titleBar && modal) {
            isDragging = true;

            const modalRect = modal.getBoundingClientRect();
            offsetX = event.clientX - (modalRect.left + modalRect.width / 2);
            offsetY = event.clientY - (modalRect.top + modalRect.height / 2);

            document.addEventListener("mousemove", onDrag);
            document.addEventListener("mouseup", stopDrag);
        }
    }

    function onDrag(event: MouseEvent) {
        if (isDragging && modal) {
            modal.style.left = `${event.clientX - offsetX}px`;
            modal.style.top = `${event.clientY - offsetY}px`;
        }
    }

    function stopDrag() {
        isDragging = false;
        document.removeEventListener("mousemove", onDrag);
        document.removeEventListener("mouseup", stopDrag);
    }

    onMount(() => {
        window.addEventListener("keydown", handleKeyDown);
    });

    onDestroy(() => {
        window.removeEventListener("keydown", handleKeyDown);
    });
</script>

{#if visible}
    <div class="modal-wrapper" transition:fade={{ duration: 500, easing: cubicOut }}>
        <div class="modal" bind:this={modal} transition:scale={{ duration: 500, easing: cubicOut }}>
            <!-- svelte-ignore a11y-no-static-element-interactions -->
            <div class="titlebar" bind:this={titleBar} on:mousedown={startDrag}>
                <div class="title">{title}</div>
                <button class="button-modal-close" on:click={handleClick}>
                    <img src="img/menu/icon-close.svg" alt="close">
                </button>
            </div>

            <div class="content">
                <slot/>
            </div>
        </div>
    </div>
{/if}

<style lang="scss">
  @use "../../../../colors" as *;

  .modal-wrapper {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(255, 255, 255, 0.02);
    z-index: 99999;

  }

  .modal {
    background: rgba(255, 255, 255, 0.07);
    border-radius: 16px;
    backdrop-filter: blur(8px);
    border: 1.5px solid rgba(255, 255, 255, 0.2);
    box-shadow: 0 10px 16px rgba(0, 0, 0, 0.15);
    min-width: 500px;
    max-width: 90vw;
    max-height: 90vh;
    position: fixed;
    left: 50%;
    top: 50%;
    transform: translate(-50%, -50%) scale(1);
    padding: 20px;
    display: flex;
    flex-direction: column;
    box-sizing: border-box;
    overflow: hidden;
  }

  .titlebar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 14px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    background: transparent;
    backdrop-filter: blur(6px);
    color: #d0d0e0;
    font-size: 16px;
    font-weight: 500;
    border-radius: 12px 12px 0 0;
    cursor: move;
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    box-sizing: border-box;
  }

  .title {
    color: #e0e0f0;
    font-size: 18px;
    font-weight: 600;
  }

  .button-modal-close {
    font-size: 18px;
    color: #bbb;
    background: none;
    border: none;
    cursor: pointer;
    transition: color 0.2s ease-in-out;
    display: flex;
    height: 40px;
    width: 40px;
    align-items: center;
    justify-content: center;

    &:hover {
      color: #8ab4f8;
    }
  }


  .content {
    margin-top: 52px;
    display: flex;
    flex-direction: column;
    gap: 16px;
    color: #ddddf0;
    font-size: 15px;
    padding-right: 8px;
    row-gap: 40px;
  }


</style>
