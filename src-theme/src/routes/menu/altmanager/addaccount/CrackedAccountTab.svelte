<script lang="ts">
    import Tab from "../../common/modal/Tab.svelte";
    import IconTextInput from "../../common/setting/IconTextInput.svelte";
    import ButtonSetting from "../../common/setting/ButtonSetting.svelte";
    import {addCrackedAccount} from "../../../../integration/rest";
    import {faker} from "@faker-js/faker";
    import IconButton from "../../common/buttons/IconButton.svelte";

    let username = "";
    $: disabled = validateUsername(username);

    async function addAccount() {
        if (disabled) {
            return;
        }
        await addCrackedAccount(username);
    }

    function validateUsername(username: string): boolean {
        return !/^[a-zA-Z0-9_]{1,16}$/.test(username);
    }

    function generateRandomUsername() {
        username = faker.internet.userName().substring(0, 16).replace(/[^a-zA-Z0-9_]+/gi, "");
    }
</script>

<Tab>
    <IconTextInput icon="user" title="Username" pattern={"[a-zA-Z0-9_]{1,16}"} bind:value={username} maxLength={16}>
        <IconButton icon="random" title="Random" on:click={generateRandomUsername} />
    </IconTextInput>
    <ButtonSetting {disabled} title="Add Account" on:click={addAccount} listenForEnter={true} inset={true}/>
</Tab>