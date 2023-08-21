<script>
    export let title;
    export let content;
    export let severity;
</script>

<div class="notification">
    <div class="icon {severity.toString().toLowerCase()}"/>
    <div class="title">{title}</div>
    <div class="content">{content}</div>
</div>

<style lang="scss">
  .notification {
    font-family: "Montserrat", sans-serif;
    display: grid;
    grid-template-areas:
      "a b"
      "a c";
    grid-template-columns: max-content 1fr;
    column-gap: 10px;
    background: rgba(0, 0, 0, 0.68);
    border-radius: 6px;
    width: 300px;
    overflow: hidden;
    padding: 10px;
  }

  .icon {
    height: 40px;
    width: 40px;
    background-position: center;
    background-repeat: no-repeat;
    border-radius: 4px;
    grid-area: a;

    &.success {
      background-color: #4dac68;
      background-image: url("../img/notification/success.svg");
    }

    &.error {
      background-color: #fc4130;
      background-image: url("../img/notification/error.svg");
    }

    &.info {
      background-color: #4677ff;
      background-image: url("../img/notification/info.svg");
    }

    &.enabled {
      background-color: #4dac68;
      background-image: url("../img/notification/enabled.svg");
    }

    &.disabled {
      background-color: #fc4130;
      background-image: url("../img/notification/disabled.svg");
    }
  }

  .title {
    grid-area: b;
    font-size: 14px;
    color: white;
    font-weight: 600;
  }

  .content {
    grid-area: c;
    font-size: 12px;
    color: #cbd1e3;
  }
</style>
