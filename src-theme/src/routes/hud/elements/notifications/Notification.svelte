<script lang="ts">
    export let title: string;
    export let message: string;
    export let severity: string;
</script>

<div class="notification">
    <div class="icon {severity.toString().toLowerCase()}"></div>
    <div class="title">{title}</div>
    <div class="message">{message}</div>
</div>

<style lang="scss">
  .notification {
    display: grid;
    grid-template-areas:
            "a b"
            "a c";
    grid-template-columns: max-content 1fr;
    column-gap: 10px;
    background: var(--notification-background-color);
    border-radius: 5px;
    width: 300px;
    overflow: hidden;
    padding: 10px;
    margin-bottom: 10px;
  }

  .icon {
    height: 40px;
    width: 40px;
    background-position: center;
    background-repeat: no-repeat;
    border-radius: 4px;
    grid-area: a;
    transition: background-color 0.2s;
    position: relative;
    background-image: url("/img/hud/notification/icon-toggle.svg");

    &.success {
      background-color: var(--notification-success-color);
      background-image: url("/img/hud/notification/icon-success.svg");
    }

    &.error {
      background-color: var(--notification-error-color);
      background-image: url("/img/hud/notification/icon-error.svg");
    }

    &.info {
      background-color: var(--notification-info-color);
      background-image: url("/img/hud/notification/icon-info.svg");
    }

    &.disabled,
    &.enabled {
      &::after {
        content: "";
        position: absolute;
        height: 10px;
        width: 10px;
        border-radius: 5px;
        top: 50%;
        transform: translate(-50%, -50%);
        background: var(--notification-toggle-knob-color);
        transition: all 0.2s ease-out;
      }
    }

    &.enabled {
      background-color: var(--notification-success-color);

      &::after {
        left: 62%;
      }
    }

    &.disabled {
      background-color: var(--notification-error-color);

      &::after {
        left: 38%;
      }
    }
  }

  .title {
    grid-area: b;
    font-size: 14px;
    color: var(--notification-title-color);
    font-weight: 600;
  }

  .message {
    grid-area: c;
    font-size: 12px;
    color: var(--notification-message-color);
  }
</style>
