<script lang="ts">
    import Tab from "../../common/modal/Tab.svelte";
    import ButtonSetting from "../../common/setting/ButtonSetting.svelte";
    import {addMicrosoftAccount} from "../../../../integration/rest.js";
    import {createEventDispatcher, onDestroy} from "svelte";
    import {listen, deleteListener} from "../../../../integration/ws.js";
    import Message from "../../common/Message.svelte";

    const dispatch = createEventDispatcher();

    let eventMessage = "";

    async function addAccount() {
        await addMicrosoftAccount();
    }

    function handleAltManagerUpdate(event: any) {
        if (event.message) {
            eventMessage = event.message;
        }
    }

    listen("altManagerUpdate", handleAltManagerUpdate);

    onDestroy(() => {
        deleteListener("altManagerUpdate", handleAltManagerUpdate);
    })
</script>

<Tab>
    {#if eventMessage}
        <Message message={eventMessage}/>
    {/if}
    <ButtonSetting title="Link Account" on:click={addAccount}/>
</Tab>