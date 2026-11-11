<script lang="ts">
    import type {Client} from "../integration/types";
    import {clientViewUrl} from "../integration/rest";

    let {client}: { client: Client } = $props();

    // A client that joins or leaves a world can have a view again, so a failed stream is retried then
    const streamKey = $derived(`${client.id}:${client.status}`);
    let failedKey = $state<string | null>(null);

    // The active client is the window itself and has no view of its own
    const streaming = $derived(!client.active && failedKey !== streamKey);

    /**
     * The client draws the view as long as the request is open, which removing the image alone may not end.
     */
    function stream(image: HTMLImageElement, key: string) {
        const fail = () => failedKey = key;
        image.addEventListener("error", fail);

        return {
            destroy() {
                image.removeEventListener("error", fail);
                image.removeAttribute("src");
            }
        };
    }
</script>

<div class="client-view">
    {#if streaming}
        {#key streamKey}
            <img
                    class="stream"
                    src={clientViewUrl(client.id)}
                    alt={client.username}
                    use:stream={streamKey}
            >
        {/key}
    {:else}
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
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .avatar {
    height: 50%;
    aspect-ratio: 1;
    border-radius: 5px;
    image-rendering: pixelated;
  }
</style>
