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
    ];

    let activeTab = parseInt(localStorage.getItem("altmanager_active_tab") ?? "0");

    function handleModify() {
        visible = false;
        dispatch("modify");
    }

    function handleChangeTab(e: CustomEvent<{ activeTab: number }>) {
        activeTab = e.detail.activeTab;
        localStorage.setItem("altmanager_active_tab", e.detail.activeTab.toString());
    }
</script>

<Modal title="Add Account" bind:visible={visible}>
    <Tabs {tabs} {activeTab} on:modify={handleModify} on:changeTab={handleChangeTab}/>
</Modal>