<script lang="ts">
    import ButtonSetting from "../../../common/setting/ButtonSetting.svelte";
    import Tab from "../../../common/modal/Tab.svelte";
    import IconTextInput from "../../../common/setting/IconTextInput.svelte";
</script>

<Tab>
    <IconTextInput title="E-Mail" icon="user"></IconTextInput>
    <IconTextInput title="Password" icon="lock" type="password"></IconTextInput>
    <ButtonSetting title="Link Account" inset={true}/>
</Tab>
