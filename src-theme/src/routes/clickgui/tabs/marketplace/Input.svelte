<script lang="ts">
    let {value = $bindable(), placeholder = "", prefix, multiline = false, maxlength, mono = false}: {
        value: string;
        placeholder?: string;
        prefix?: string;
        multiline?: boolean;
        maxlength?: number;
        mono?: boolean;
    } = $props();
</script>

<div class="input" class:mono>
    {#if prefix}
        <span class="prefix">{prefix}</span>
    {/if}
    {#if multiline}
        <textarea spellcheck="false" rows="3" {placeholder} bind:value></textarea>
    {:else}
        <input type="text" spellcheck="false" {placeholder} {maxlength} bind:value/>
    {/if}
</div>

<style lang="scss">
  .input {
    display: flex;
    align-items: baseline;
    background-color: var(--clickgui-input-background-color);
    border-bottom: solid 2px var(--clickgui-input-border-color);
    border-radius: 3px;
    padding: 7px 9px;
    font-size: 12px;
  }

  .prefix {
    color: var(--clickgui-text-dimmed-color);
    font-weight: 500;
  }

  input, textarea {
    all: unset;
    flex: 1;
    font-family: "Inter", sans-serif;
    font-size: 12px;
    font-weight: 500;
    color: var(--clickgui-text-color);
    white-space: pre-wrap;

    &::placeholder {
      color: var(--clickgui-text-dimmed-color);
      opacity: .6;
    }
  }

  .mono :is(input, textarea) {
    font-family: monospace;
  }
</style>
