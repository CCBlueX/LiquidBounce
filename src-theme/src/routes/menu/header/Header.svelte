<script lang="ts">
    import Account from "./Account.svelte";

    export let username: string;
    export let avatar: string;
    export let premium: boolean;
</script>

<div class="header">
    <img class="logo" src="img/lb-logo.svg" alt="logo">

    <Account {username} {avatar} {premium} />
</div>

<style lang="scss">
    .header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 60px;
    }
</style>