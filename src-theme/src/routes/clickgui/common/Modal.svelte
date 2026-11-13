<script lang="ts">
    import {onMount, type Snippet} from "svelte";
    import {escapeLayer} from "../../../integration/escape";
    import {scaleFactor} from "../clickgui_store";

    /**
     * A modal dialog that is open while it is mounted. [onclose] also runs for Esc and a click beside it.
     */
    let {title, width = 460, onclose, children, footer}: {
        title: string;
        width?: number;
        onclose: () => void;
        children: Snippet;
        footer?: Snippet;
    } = $props();

    const titleId = $props.id();
    let element: HTMLDialogElement;

    const layer = escapeLayer(() => element.close());

    onMount(() => {
        element.showModal();
        layer.setOpen(true);
    });
</script>

<!-- The top layer leaves the scaled ClickGUI, so the dialog takes its scale itself -->
<dialog bind:this={element} class="modal" aria-labelledby={titleId} style:zoom={$scaleFactor / 2}
        style:width="{width}px" {onclose} {...{closedby: "any"}}>
    <h2 class="title" id={titleId}>{title}</h2>
    <div class="content">
        {@render children()}
    </div>
    {#if footer}
        <div class="footer">
            {@render footer()}
        </div>
    {/if}
</dialog>

<style lang="scss">
  .modal {
    max-width: 92vw;
    max-height: 86vh;
    // The page's reset takes the margin that centers a modal dialog
    margin: auto;
    padding: 0;
    border: none;
    border-radius: 5px;
    overflow: hidden;
    color: var(--clickgui-text-color);
    background-color: var(--clickgui-modal-background-color);
    box-shadow: 0 0 10px var(--clickgui-window-shadow-color);
    overscroll-behavior: contain;

    &[open] {
      display: flex;
      flex-direction: column;
      animation: modal-in 0.15s ease-out;
    }

    &::backdrop {
      background-color: var(--clickgui-modal-backdrop-color);
      overflow: hidden;
      overscroll-behavior: contain;
      animation: backdrop-in 0.15s ease-out;
    }
  }

  .title {
    padding: 12px 16px;
    font-size: 14px;
    font-weight: 600;
    background-color: var(--clickgui-window-header-background-color);
    border-bottom: solid 2px var(--clickgui-window-header-border-color);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .content {
    display: flex;
    flex-direction: column;
    row-gap: 12px;
    min-height: 0;
    padding: 14px 16px;
    overflow: auto;
    font-size: 12px;
  }

  .footer {
    display: flex;
    justify-content: flex-end;
    column-gap: 8px;
    padding: 0 16px 14px;
  }

  @keyframes modal-in {
    from {
      opacity: 0;
      transform: translateY(-6px);
    }
  }

  @keyframes backdrop-in {
    from {
      opacity: 0;
    }
  }
</style>
