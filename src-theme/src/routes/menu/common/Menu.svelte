<script lang="ts">
    import Header from "./header/Header.svelte";
    import {fly} from "svelte/transition";
</script>

<div class="menu">
    <div in:fly|global={{duration: 180, y: -20}}>
        <Header/>
    </div>

    <div class="menu-wrapper">
        <slot/>
    </div>
</div>

<style lang="scss">
  .menu {
    padding: 50px;
    display: flex;
    flex-direction: column;
    // Not sized in vh, which not every browser scales with zoom
    position: fixed;
    inset: 0;
  }

  .menu-wrapper {
    flex: 1;
    display: flex;
    flex-direction: column;
    will-change: transform;
  }

  @media screen and (max-width: 1366px) {
    .menu {
      zoom: 0.8;
    }
  }

  @media screen and (max-width: 1200px) {
    .menu {
      zoom: 0.5;
    }
  }

  @media screen and (max-height: 1100px) {
    .menu {
      zoom: 0.8;
    }
  }

  @media screen and (max-height: 700px) {
    .menu {
      zoom: 0.5;
    }
  }

  @media screen and (max-height: 540px) {
    .menu {
      zoom: 0.4;
    }
  }
</style>
