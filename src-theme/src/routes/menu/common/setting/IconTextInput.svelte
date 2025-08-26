<script lang="ts">
    export let icon: string;
    export let title: string;
    export let type = "text";
    export let value = "";
    export let maxLength: number | null = null;
    export let pattern: string | null = null;
</script>

<div class="icon-text-input">
    <div class="icon">
        <img src="img/menu/icon-{icon}.svg" alt={icon}>
    </div>
    {#if type === "text"}
        <input {pattern} maxlength={maxLength} class="input" spellcheck="false" type="text" placeholder={title} bind:value={value} autocomplete="off">
    {:else if type === "password"}
        <input {pattern} maxlength={maxLength} class="input" type="password" placeholder={title} bind:value={value} autocomplete="off">
    {/if}
    <div class="button-container">
        <slot />
    </div>
</div>

<style lang="scss">
  @use "../../../../colors.scss" as *;

  .icon-text-input {
    display: grid;
    grid-template-columns: max-content 1fr max-content;
  }

  .icon {
    height: 64px;
    width: 64px;
    background-color: $accent-color;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 5px 0 0 5px;
  }

  .input {
    color: $menu-text-color;
    font-family: "Inter", sans-serif;
    font-size: 20px;
    background-color: rgba($menu-base-color, .36);
    border: none;
    padding: 0 20px 0 18px;
    border-radius: 0 5px 5px 0;
    border-left: solid 2px $menu-base-color;
    width: 100%;

    &:invalid {
      border: solid 2px $menu-error-color;
    }
  }

  .button-container {
    display: flex;
    align-items: center;
  }
</style>