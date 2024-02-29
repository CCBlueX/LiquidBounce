<script lang="ts">
    import Account from "./Account.svelte";
    import type {Session} from "../../../../integration/types";
    import {onMount} from "svelte";
    import {getSession} from "../../../../integration/rest";

    let session: Session | null = null;

    onMount(async () => {
        session = await getSession();
    });
</script>

<div class="header">
    <img class="logo" src="img/lb-logo.svg" alt="logo">

    {#if session}
        <Account username={session.username} avatar={session.avatar} premium={session.premium}/>
    {/if}
</div>

<style lang="scss">
  .header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 60px;
  }
</style>