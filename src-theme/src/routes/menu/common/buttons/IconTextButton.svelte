<script lang="ts">
    import {createEventDispatcher} from "svelte";

    export let title: string;
    export let icon: string;
    export let disabled = false;

    const dispatch = createEventDispatcher();
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<button class="icon-text-button" on:click={() => dispatch("click")} {disabled}>
    <div class="icon">
        <img src="img/menu/{icon}" alt={title}>
    </div>
    <div class="title">{title}</div>
</button>

<style lang="scss">

    .icon-text-button {
      display: flex;
      border: 1px solid var(--accent-border-color);
      border-radius: var(--radius-sm);
      align-items: center;
      overflow: hidden;
      background: linear-gradient(var(--accent-tint-color), var(--accent-tint-color)) left / 0 100% no-repeat var(--menu-icon-text-button-background-color);
      box-shadow: var(--elevation-sm) var(--menu-base-50-color);
      transition: opacity var(--duration-fast) var(--ease-standard),
      background-size var(--duration-base) var(--ease-emphasized),
      border-color var(--duration-fast) var(--ease-standard);

      &:not([disabled]):hover {
        background-size: 100% 100%;
        border-color: var(--accent-border-strong-color);
        cursor: pointer;
      }

      &:not([disabled]):active {
        transform: translateY(1px);
      }

      &[disabled] {
        cursor: not-allowed;
        opacity: .45;
      }
    }

    .icon {
      height: 56px;
      width: 56px;
      background-color: var(--menu-icon-text-button-icon-background-color);
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .title {
      font-size: 18px;
      font-weight: 500;
      letter-spacing: -0.01em;
      color: var(--menu-text-color);
      padding: 0 24px;
    }
</style>