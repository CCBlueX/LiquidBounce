<script lang="ts">
    import Modal from "../../common/modal/Modal.svelte";
    import MicrosoftAccountTab from "./MicrosoftAccountTab.svelte";
    import TheAlteningAccountTab from "./TheAlteningAccountTab.svelte";
    import Tabs from "../../common/modal/Tabs.svelte";
    import CrackedAccountTab from "./CrackedAccountTab.svelte";
    import SessionAccountTab from "./SessionAccountTab.svelte";
    import {createEventDispatcher} from "svelte";

    export let visible: boolean;

    const dispatch = createEventDispatcher();

    const tabs = [
        {
            title: "Microsoft",
            icon: "icon-microsoft.svg",
            component: MicrosoftAccountTab
        },
        {
            title: "TheAltening",
            icon: "icon-thealtening.svg",
            component: TheAlteningAccountTab
        },
        {
            title: "Cracked",
            icon: "icon-cracked.png",
            component: CrackedAccountTab
        },
        {
            title: "Session",
            icon: "icon-session.svg",
            component: SessionAccountTab
        }
    ]

    function handleModify() {
        visible = false;
        dispatch("modify");
    }
</script>

<Modal title="Add Account" bind:visible={visible}>
    <Tabs {tabs} on:modify={handleModify}/>
</Modal>