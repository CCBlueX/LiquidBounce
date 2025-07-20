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
    @use "../../../../colors.scss" as *;

    .icon-text-button {
      display: flex;
      border: none;

      border-radius: 5px;
      align-items: center;
      overflow: hidden;
      background: linear-gradient(to left, rgba($menu-base-color, .36) 50%, $accent-color 50%);
      background-size: 200% 100%;
      background-position: right bottom;
      will-change: background-position;
      transition: ease opacity .2s, background-position .2s ease-out;

      &:not([disabled]):hover {
        &:hover {
          background-position: left bottom;
          cursor: pointer;
        }
      }

      &[disabled] {
        opacity: .6;
      }
    }

    .icon {
      height: 58px;
      width: 58px;
      background-color: $accent-color;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .title {
      font-size: 20px;
      font-weight: 500;
      color: $menu-text-color;
      padding: 0 30px;
    }
</style>