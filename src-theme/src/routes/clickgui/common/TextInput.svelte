<script lang="ts">
    let {value = $bindable(), label, placeholder = "", multiline = false, maxlength, autofocus = false, oninput}: {
        value: string;
        label?: string;
        placeholder?: string;
        multiline?: boolean;
        maxlength?: number;
        autofocus?: boolean;
        oninput?: () => void;
    } = $props();
</script>

<!-- svelte-ignore a11y_autofocus -->
<label class="field">
    {#if label}
        <span class="label">{label}</span>
    {/if}
    {#if multiline}
        <textarea class="input" rows="4" spellcheck="false" {placeholder} {maxlength} {autofocus} {oninput}
                  bind:value></textarea>
    {:else}
        <input class="input" type="text" spellcheck="false" {placeholder} {maxlength} {autofocus} {oninput}
               bind:value/>
    {/if}
</label>

<style lang="scss">
  .field {
    display: block;
  }

  .label {
    display: block;
    font-weight: 500;
    color: var(--clickgui-text-color);
    font-size: 12px;
    margin-bottom: 5px;
  }

  .input {
    display: block;
    width: 100%;
    background-color: var(--clickgui-input-background-color);
    font-family: "Inter", sans-serif;
    font-size: 12px;
    color: var(--clickgui-text-color);
    border: none;
    border-bottom: solid 2px var(--clickgui-input-border-color);
    padding: 7px 8px;
    border-radius: 3px;
    user-select: text;

    &::placeholder {
      color: var(--clickgui-search-hint-color);
    }
  }

  textarea.input {
    resize: vertical;
    min-height: 60px;
    line-height: 1.4;
  }
</style>
