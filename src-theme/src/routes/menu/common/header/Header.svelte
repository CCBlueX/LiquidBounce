<script lang="ts">
    import Account from "./Account.svelte";
</script>

<div class="header">
    <img class="logo" src="img/lb-logo.svg" alt="logo">

    <Account/>
</div>

<style lang="scss">
  .header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 60px;
  }
</style>