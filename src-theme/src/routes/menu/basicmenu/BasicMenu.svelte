<script lang="ts">
    import ButtonContainer from "../common/buttons/ButtonContainer.svelte";
    import IconButton from "../common/buttons/IconButton.svelte";
    import {openScreen, toggleBasicMode} from "../../../integration/rest";
    import {fly} from "svelte/transition";
</script>

<!-- Clicks also reach the vanilla screen below, so this stays in its empty left strip. -->
<div class="basic-menu" transition:fly|global={{duration: 700, x: -100}}>
    <div class="buttons">
        <ButtonContainer vertical>
            <IconButton title="Full Mode" icon="liquidbounce" on:click={toggleBasicMode}/>
            <IconButton title="Click GUI" icon="clickgui" on:click={() => openScreen("clickgui")}/>
            <IconButton title="Alt Manager" icon="user" on:click={() => openScreen("altmanager")}/>
            <IconButton title="Proxy Manager" icon="proxymanager" on:click={() => openScreen("proxymanager")}/>
        </ButtonContainer>
    </div>
</div>

<style lang="scss">
  .basic-menu {
    position: fixed;
    top: 0;
    bottom: 0;
    left: 0;
    display: flex;
    align-items: center;
  }

  .buttons {
    margin-left: 50px;
  }

  @media screen and (max-width: 1366px) {
    .buttons {
      zoom: 0.8;
    }
  }

  @media screen and (max-width: 1200px) {
    .buttons {
      zoom: 0.5;
    }
  }

  @media screen and (max-height: 1100px) {
    .buttons {
      zoom: 0.8;
    }
  }

  @media screen and (max-height: 700px) {
    .buttons {
      zoom: 0.5;
    }
  }

  @media screen and (max-height: 540px) {
    .buttons {
      zoom: 0.4;
    }
  }
</style>
