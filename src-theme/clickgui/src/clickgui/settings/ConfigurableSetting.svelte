<script>
    import GenericSetting from "./GenericSetting.svelte";
    import SubSettings from "./SubSettings.svelte";

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

    <SubSettings {settings}/>
</div>

<style>

    .setting {
        padding: 7px 0;
    }
    
    .name {
        font-weight: 500;
        color: white;
        font-size: 12px;
        margin-left: 10px;
        margin-bottom: 3px;
    }
</style>

