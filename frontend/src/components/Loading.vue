<template>
  <div class="loading-container" :class="{ fullscreen }">
    <div class="loading-content">
      <div class="loading-rings">
        <div class="ring ring-outer"></div>
        <div class="ring ring-middle"></div>
        <div class="ring ring-inner"></div>
      </div>
      <p v-if="text" class="loading-text">{{ text }}</p>
    </div>
  </div>
</template>

<script setup>
defineProps({
  text: {
    type: String,
    default: '加载中...'
  },
  fullscreen: {
    type: Boolean,
    default: false
  }
})
</script>

<style scoped lang="scss">
.loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;

  &.fullscreen {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(245, 240, 232, 0.92);
    z-index: 9999;
  }

  .loading-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 20px;
  }

  .loading-rings {
    position: relative;
    width: 80px;
    height: 80px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .ring {
    position: absolute;
    border-radius: 50%;
    border: 3px solid transparent;
    animation: ringSpin 1.8s cubic-bezier(0.5, 0, 0.5, 1) infinite;

    &-outer {
      width: 80px;
      height: 80px;
      border-top-color: var(--gold);
      border-right-color: var(--gold-light);
      animation-duration: 2s;
    }

    &-middle {
      width: 56px;
      height: 56px;
      border-bottom-color: var(--palace-red);
      border-left-color: var(--palace-red-light);
      animation-duration: 1.5s;
      animation-direction: reverse;
    }

    &-inner {
      width: 32px;
      height: 32px;
      border-top-color: var(--gold);
      border-left-color: var(--gold-light);
      animation-duration: 1.2s;
    }
  }

  .loading-text {
    font-family: var(--font-body);
    font-size: 14px;
    color: var(--gold-dark);
    font-weight: 500;
    letter-spacing: 2px;
  }
}

@keyframes ringSpin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>
