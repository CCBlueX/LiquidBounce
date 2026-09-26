<script lang="ts">
    import LiquidProxyLogo from "./LiquidProxyLogo.svelte";

    export let locationCount: number;
    export let loggingIn = false;

    // What liquidproxy.net advertises
    $: features = [
        {
            icon: "globe",
            title: "Global IP Network",
            text: `${locationCount || "Many"} locations, so you can connect from where you need.`
        },
        {icon: "zap", title: "Instant IP Switching", text: "A new IP with a single click, without downtime."},
        {icon: "lock", title: "Enhanced Privacy", text: "Your real IP address stays hidden."},
        {icon: "shield-check", title: "Bypass Restrictions", text: "Get onto servers that block your IP or region."},
    ];
</script>

<div class="welcome">
    <LiquidProxyLogo/>

    <div class="pitch">
        <div class="headline">
            Are you tired of VPNs not working on Minecraft servers? <span class="accent">Not anymore!</span>
        </div>
        <p class="subline">Built right into LiquidBounce, LiquidProxy lets you bypass IP bans within seconds.</p>
    </div>

    <div class="features">
        {#each features as feature (feature.title)}
            <div class="feature">
                <div class="icon">
                    <img src="img/menu/liquidproxy/{feature.icon}.svg" alt={feature.title}>
                </div>
                <div class="title">{feature.title}</div>
                <div class="text">{feature.text}</div>
            </div>
        {/each}
    </div>

    <p class="hint">
        {#if loggingIn}
            Finish logging in in the browser that opened.
        {:else}
            Log in with the LiquidBounce account you got LiquidProxy on.
        {/if}
    </p>
</div>

<style lang="scss">
  .welcome {
    display: flex;
    flex-direction: column;
    justify-content: center;
    row-gap: 32px;
    flex: 1;
    max-width: 760px;
  }

  .pitch {
    display: flex;
    flex-direction: column;
    row-gap: 12px;
  }

  .headline {
    color: var(--menu-text-color);
    font-size: 30px;
    font-weight: 600;
    line-height: 1.25;

    .accent {
      color: var(--accent-color);
    }
  }

  .subline {
    margin: 0;
    color: var(--menu-text-dimmed-color);
    font-size: 19px;
    line-height: 1.4;
  }

  .features {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
  }

  .feature {
    display: grid;
    grid-template-columns: max-content 1fr;
    grid-template-rows: max-content 1fr;
    column-gap: 16px;
    row-gap: 4px;
    padding: 18px;
    border-radius: 8px;
    background-color: rgba(255, 255, 255, 0.04);
    border: 1px solid rgba(255, 255, 255, 0.06);

    .icon {
      grid-row: span 2;
      display: flex;
      align-items: center;
      justify-content: center;
      width: 44px;
      height: 44px;
      border-radius: 10px;
      background-color: var(--accent-color);

      img {
        width: 22px;
        height: 22px;
      }
    }

    .title {
      color: var(--menu-text-color);
      font-size: 18px;
      font-weight: 600;
    }

    .text {
      color: var(--menu-text-dimmed-color);
      font-size: 15px;
      line-height: 1.35;
    }
  }

  .hint {
    margin: 0;
    color: var(--menu-text-dimmed-color);
    font-size: 16px;
  }
</style>
