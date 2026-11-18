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

  .icon-text-input {
    display: grid;
    grid-template-columns: max-content 1fr max-content;
    border: 1px solid var(--accent-border-color);
    border-radius: var(--radius-sm);
    overflow: hidden;
    transition: border-color var(--duration-fast) var(--ease-standard),
    box-shadow var(--duration-fast) var(--ease-standard);

    &:focus-within {
      border-color: var(--accent-color);
      box-shadow: 0 0 0 3px var(--accent-subtle-background-color);
    }
  }

  .icon {
    height: 60px;
    width: 60px;
    background-color: var(--menu-input-icon-background-color);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .input {
    color: var(--menu-text-color);
    font-family: "Inter", sans-serif;
    font-size: 18px;
    background-color: var(--menu-input-background-color);
    border: none;
    padding: 0 20px 0 18px;
    border-left: solid 1px var(--surface-border-color);
    width: 100%;

    &:invalid {
      box-shadow: inset 0 0 0 1px var(--menu-input-error-border-color);
    }
  }

  .button-container {
    display: flex;
    align-items: center;
  }
</style>
