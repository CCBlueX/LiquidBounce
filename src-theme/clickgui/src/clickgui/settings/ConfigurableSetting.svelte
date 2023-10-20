<script>
    import GenericSetting from "./GenericSetting.svelte";

    export let instance;

    const hiddenSettings = ["Enabled", "Hidden", "Bind"];

    function toJavaScriptArray(a) {
        const v = [];
        for (let i = 0; i < a.length; i++) {
            if (!hiddenSettings.includes(a[i].getName())) {
                v.push(a[i]);
            }
        }

        return v;
    }

    const settings = toJavaScriptArray(instance.getContainedValues());
    const name = instance.getName();
</script>

<div class="setting">
    <div class="name">{name}</div>

    <div class="settings">
        {#each settings as s}
            <GenericSetting instance={s}/>
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

