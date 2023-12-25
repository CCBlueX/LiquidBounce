<script>
    import GenericSetting from "./GenericSetting.svelte";

    /**
     * A reference to the value instance of this setting. It is part of the module configurable and should NOT lose its reference.
     */
    export let reference;
    /**
     * This function is passed from the parent component and is used to write the new configurable to the client.
     * This will result in a request to the server.
     */
    export let write;

    const name = reference.name;
</script>

<div class="setting">
    <div class="name"><b>{name}</b></div>

    <div class="settings">
        {#each reference.value as s}
            <GenericSetting reference={s} write={write} />
        {/each}
    </div>
</div>

<style>
    .name {
        font-weight: 500;
        color: var(--text);;
        font-size: 12px;
        margin-left: 10px;
        margin-bottom: 3px;
    }

    .setting {
        background-color: rgba(0, 0, 0, 0.36);
        border-right: solid 4px var(--accent);
        padding: 7px 0;
    }
</style>

