<script lang="ts">
    import ButtonSetting from "../../../common/setting/ButtonSetting.svelte";
    import Tab from "../../../common/modal/Tab.svelte";
</script>

<Tab>
    <ButtonSetting title="Link Account"/>
    <ButtonSetting title="Copy URL" secondary={true}/>
</Tab>
