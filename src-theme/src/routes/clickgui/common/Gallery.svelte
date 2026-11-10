<script lang="ts">
    import type {MarkdownImage} from "../../../util/markdown";

    /**
     * A strip of images, each opening enlarged with previous and next. An image that fails to load is left out.
     */
    let {images}: {
        images: MarkdownImage[];
    } = $props();

    let failed = $state<string[]>([]);
    let openIndex = $state<number | null>(null);
    let viewer = $state<HTMLDialogElement>();

    const shown = $derived(images.filter(image => !failed.includes(image.src)));
    const opened = $derived(openIndex === null ? undefined : shown[openIndex]);

    $effect(() => {
        if (viewer && !viewer.open) {
            viewer.showModal();
        }
    });

    function step(delta: number) {
        if (openIndex !== null) {
            openIndex = (openIndex + delta + shown.length) % shown.length;
        }
    }

    function handleKeyDown(event: KeyboardEvent) {
        if (event.key === "ArrowLeft") {
            step(-1);
        } else if (event.key === "ArrowRight") {
            step(1);
        }
    }

    function handleError(src: string) {
        failed = [...failed, src];
        openIndex = null;
    }
</script>

{#if shown.length > 0}
    <div class="gallery">
        {#each shown as image, index (image.src)}
            <button type="button" class="thumbnail" aria-label="Enlarge {image.alt || `image ${index + 1}`}"
                    onclick={() => openIndex = index}>
                <img src={image.src} alt="" loading="lazy" decoding="async" referrerpolicy="no-referrer"
                     draggable="false" onerror={() => handleError(image.src)}/>
            </button>
        {/each}
    </div>
{/if}

{#if opened && openIndex !== null}
    <!-- svelte-ignore a11y_click_events_have_key_events, a11y_no_noninteractive_element_interactions -->
    <dialog bind:this={viewer} class="viewer" aria-label={opened.alt || "Image"} {...{closedby: "any"}}
            onclose={() => openIndex = null} onkeydown={handleKeyDown}
            onclick={event => event.target === event.currentTarget && viewer?.close()}>
        <img class="image" src={opened.src} alt={opened.alt} referrerpolicy="no-referrer" draggable="false"
             onerror={() => handleError(opened.src)}/>
        <div class="controls">
            {#if shown.length > 1}
                <button type="button" class="control previous" aria-label="Previous" onclick={() => step(-1)}></button>
                <span class="position">{openIndex + 1} / {shown.length}</span>
                <button type="button" class="control next" aria-label="Next" onclick={() => step(1)}></button>
            {/if}
            {#if opened.alt}
                <span class="caption">{opened.alt}</span>
            {/if}
            <!-- svelte-ignore a11y_autofocus -->
            <button type="button" class="control close" aria-label="Close" autofocus
                    onclick={() => viewer?.close()}></button>
        </div>
    </dialog>
{/if}

<style lang="scss">
  .gallery {
    display: flex;
    column-gap: 8px;
    overflow-x: auto;
    padding-bottom: 4px;

    &::-webkit-scrollbar {
      height: 4px;
    }
  }

  .thumbnail {
    flex: none;
    height: 120px;
    padding: 0;
    border: solid 2px transparent;
    border-radius: 5px;
    overflow: hidden;
    background-color: var(--clickgui-marketplace-entry-background-color);
    cursor: pointer;
    transition: ease border-color 0.2s;

    &:hover, &:focus-visible {
      border-color: var(--clickgui-marketplace-entry-hover-border-color);
    }

    img {
      display: block;
      height: 100%;
      max-width: 320px;
      object-fit: cover;
    }
  }

  .viewer {
    inset: 0;
    width: auto;
    height: auto;
    max-width: none;
    max-height: none;
    margin: 0;
    padding: 24px;
    border: none;
    background: none;
    color: var(--clickgui-text-color);
    overscroll-behavior: contain;

    &[open] {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      row-gap: 12px;
    }

    &::backdrop {
      background-color: var(--clickgui-marketplace-viewer-backdrop-color);
      overflow: hidden;
      overscroll-behavior: contain;
    }
  }

  .image {
    min-height: 0;
    max-width: 100%;
    max-height: calc(100% - 50px);
    object-fit: contain;
    border-radius: 5px;
    box-shadow: 0 0 10px var(--clickgui-window-shadow-color);
  }

  .controls {
    display: flex;
    align-items: center;
    column-gap: 6px;
    padding: 6px;
    border-radius: 999px;
    background-color: var(--clickgui-tabs-background-color);
    box-shadow: 0 0 10px var(--clickgui-tabs-shadow-color);
    font-size: 14px;
    font-weight: 500;
  }

  .control {
    width: 30px;
    height: 30px;
    border: none;
    border-radius: 50%;
    background-color: transparent;
    background-position: center;
    background-repeat: no-repeat;
    background-size: 14px;
    cursor: pointer;
    transition: ease background-color 0.2s;

    &:hover, &:focus-visible {
      background-color: var(--clickgui-tab-hover-background-color);
    }

    &.previous, &.next {
      background-image: url("/img/menu/icon-select-arrow.svg");
    }

    &.previous {
      rotate: 90deg;
    }

    &.next {
      rotate: -90deg;
    }

    &.close {
      background-image: url("/img/menu/icon-close.svg");
      background-size: 12px;
    }
  }

  .position, .caption {
    padding: 0 6px;
    color: var(--clickgui-text-dimmed-color);
    white-space: nowrap;
  }

  .caption {
    max-width: 60ch;
    overflow: hidden;
    text-overflow: ellipsis;
  }
</style>
