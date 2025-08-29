<script lang="ts">
    import Notifications from "./Notifications.svelte";
    import Account from "./account/Account.svelte";
    import Logo from "./Logo.svelte";

    export let showAccount: boolean;
    export let showHeader: boolean;

</script>

{#if showHeader}
    <div class="header">

        <div class="notifications">
            <Notifications/>
        </div>
        <div class="logo">
            <Logo showLogo={true}/>
        </div>
        {#if showAccount}
            <Account/>
        {/if}
    </div>
{/if}
<style lang="scss">
  @use "../../../../colors" as *;

  .logo {
    height: 125px;
  }

  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 60px;
  }

  .notifications {
    position: absolute;
    left: 50%;
    transform: translateX(-50%);
  }
</style>
