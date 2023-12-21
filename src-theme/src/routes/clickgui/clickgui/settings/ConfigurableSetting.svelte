<script>
    import GenericSetting from "./GenericSetting.svelte";

    export let instance;
    export let write;

    const hiddenSettings = ["Enabled", "Hidden", "Bind"];
    const settings = instance.value.filter(s => !hiddenSettings.includes(s.name));
    const name = instance.name;
</script>

<div class="setting">
    <div class="name"><b>{name}</b></div>

    <div class="settings">
        {#each settings as s}
            <GenericSetting instance={s} write={write} />
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

