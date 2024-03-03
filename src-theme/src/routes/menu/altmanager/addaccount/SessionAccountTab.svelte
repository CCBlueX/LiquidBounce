<script lang="ts">
    import Tab from "../../common/modal/Tab.svelte";
    import IconTextInput from "../../common/setting/IconTextInput.svelte";
    import ButtonSetting from "../../common/setting/ButtonSetting.svelte";

    let sessionId = "";
    $: disabled = validateSessionId(sessionId);

    function validateSessionId(sessionId: string): boolean {
        return sessionId.length === 0;
    }
</script>

<Tab>
    <IconTextInput icon="exit" title="Session ID" bind:value={sessionId}/>
    <ButtonSetting title="Add Account" {disabled} />
</Tab>