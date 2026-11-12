<script lang="ts">
    import type {Client} from "../integration/types";
    import {getClientView} from "../integration/rest";

    const FRAME_INTERVAL = 100;
    const RETRY_INTERVAL = 1000;

    let {client}: { client: Client } = $props();

    // Primitives, so that the frequent client updates do not start the loop over
    const id = $derived(client.id);
    const live = $derived(!client.active);

    let canvas: HTMLCanvasElement | undefined = $state();
    let hasFrame = $state(false);

    /**
     * The client draws the view as long as it is asked for frames. The active client is the window itself.
     */
    $effect(() => {
        if (!live) {
            hasFrame = false;
            return;
        }

        const viewed = id;
        let stopped = false;
        const sleep = (millis: number) => new Promise(done => setTimeout(done, millis));

        (async () => {
            while (!stopped) {
                const started = performance.now();
                const frame = await getClientView(viewed).catch(() => null);
                if (stopped) {
                    break;
                }

                const bitmap = frame && await createImageBitmap(frame).catch(() => null);
                if (bitmap && canvas && !stopped) {
                    canvas.width = bitmap.width;
                    canvas.height = bitmap.height;
                    canvas.getContext("2d")?.drawImage(bitmap, 0, 0);
                    hasFrame = true;
                }
                bitmap?.close();

                const interval = frame ? FRAME_INTERVAL : RETRY_INTERVAL;
                await sleep(Math.max(0, interval - (performance.now() - started)));
            }
        })();

        return () => {
            stopped = true;
        };
    });
</script>

<div class="client-view">
    <canvas class="stream" class:shown={live && hasFrame} bind:this={canvas}></canvas>
    {#if !live || !hasFrame}
        <img class="avatar" src={client.avatar} alt={client.username}>
    {/if}
</div>

<style lang="scss">
  .client-view {
    position: relative;
    aspect-ratio: 16 / 9;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
  }

  .stream {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    object-fit: cover;
    visibility: hidden;

    &.shown {
      visibility: visible;
    }
  }

  .avatar {
    height: 50%;
    aspect-ratio: 1;
    border-radius: 5px;
    image-rendering: pixelated;
  }
</style>
