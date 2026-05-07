<template>
  <div class="info-card" :class="[type, { shadow, hover }]" @click="handleClick">
    <div v-if="image" class="card-image">
      <div class="image-frame">
        <img :src="image" :alt="title" />
        <div class="frame-corner tl"></div>
        <div class="frame-corner tr"></div>
        <div class="frame-corner bl"></div>
        <div class="frame-corner br"></div>
      </div>
      <div v-if="badge" class="card-badge">{{ badge }}</div>
    </div>
    <div class="card-content">
      <div v-if="tags && tags.length" class="card-tags">
        <span
          v-for="(tag, index) in tags"
          :key="index"
          class="tag-seal"
        >
          {{ tag }}
        </span>
      </div>
      <h3 v-if="title" class="card-title">
        <span class="title-accent"></span>
        {{ title }}
      </h3>
      <p v-if="description" class="card-description">{{ description }}</p>
      <div v-if="meta" class="card-meta">
        <span v-for="(item, index) in meta" :key="index" class="meta-item">
          <span class="meta-dot">●</span>
          <span class="meta-text">{{ item.text || item }}</span>
        </span>
      </div>
      <div v-if="$slots.footer" class="card-footer">
        <slot name="footer"></slot>
      </div>
    </div>
    <!-- 拐角装饰 -->
    <div class="corner top-left"></div>
    <div class="corner top-right"></div>
    <div class="corner bottom-left"></div>
    <div class="corner bottom-right"></div>
  </div>
</template>

<script setup>
defineProps({
  type: {
    type: String,
    default: 'default',
    validator: (value) => ['default', 'primary', 'success', 'warning', 'danger', 'info'].includes(value)
  },
  title: String,
  description: String,
  image: String,
  badge: String,
  tags: Array,
  tagType: {
    type: String,
    default: ''
  },
  meta: Array,
  shadow: {
    type: Boolean,
    default: true
  },
  hover: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['click'])

const handleClick = () => {
  emit('click')
}
</script>

<style scoped lang="scss">
.info-card {
  background: var(--ivory);
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.4s ease;
  border: 1px solid var(--gold-light);
  position: relative;

  &.shadow {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  }

  &.hover {
    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 28px rgba(200, 164, 92, 0.25);
      border-color: var(--gold);
    }
  }

  // 四个拐角装饰
  .corner {
    position: absolute;
    width: 16px;
    height: 16px;
    border-color: var(--gold);
    border-style: solid;
    opacity: 0.6;
    transition: opacity 0.3s ease;
    z-index: 2;
    pointer-events: none;

    &-top-left {
      top: 4px;
      left: 4px;
      border-width: 2px 0 0 2px;
      border-radius: 3px 0 0 0;
    }
    &-top-right {
      top: 4px;
      right: 4px;
      border-width: 2px 2px 0 0;
      border-radius: 0 3px 0 0;
    }
    &-bottom-left {
      bottom: 4px;
      left: 4px;
      border-width: 0 0 2px 2px;
      border-radius: 0 0 0 3px;
    }
    &-bottom-right {
      bottom: 4px;
      right: 4px;
      border-width: 0 2px 2px 0;
      border-radius: 0 0 3px 0;
    }
  }

  &:hover .corner {
    opacity: 1;
  }

  .card-image {
    position: relative;
    width: 100%;
    overflow: hidden;

    .image-frame {
      position: relative;
      margin: 8px;
      border-radius: 6px;
      overflow: hidden;
      box-shadow: inset 0 0 20px rgba(0, 0, 0, 0.1);

      img {
        width: 100%;
        height: 180px;
        object-fit: cover;
        display: block;
        transition: transform 0.5s ease;
      }

      // 画框拐角
      .frame-corner {
        position: absolute;
        width: 12px;
        height: 12px;
        border-color: var(--gold);
        border-style: solid;
        z-index: 1;
        opacity: 0.5;

        &.tl { top: 3px; left: 3px; border-width: 2px 0 0 2px; }
        &.tr { top: 3px; right: 3px; border-width: 2px 2px 0 0; }
        &.bl { bottom: 3px; left: 3px; border-width: 0 0 2px 2px; }
        &.br { bottom: 3px; right: 3px; border-width: 0 2px 2px 0; }
      }
    }

    .card-badge {
      position: absolute;
      top: 16px;
      right: 16px;
      background: var(--palace-red);
      color: #fff;
      padding: 4px 12px;
      border-radius: 4px;
      font-size: 12px;
      font-weight: 600;
      font-family: var(--font-body);
      box-shadow: 0 2px 8px rgba(122, 26, 46, 0.3);
    }
  }

  &:hover .image-frame img {
    transform: scale(1.05);
  }

  .card-content {
    padding: 16px 20px 20px;
  }

  .card-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-bottom: 10px;
  }

  .tag-seal {
    display: inline-block;
    padding: 2px 10px;
    border: 1px solid var(--gold);
    color: var(--palace-red);
    border-radius: 3px;
    font-family: var(--font-body);
    font-size: 11px;
    letter-spacing: 1px;
    background: #fff;
  }

  .card-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-family: var(--font-body);
    font-size: 17px;
    font-weight: 600;
    color: var(--ink);
    margin: 0 0 10px;
    line-height: 1.4;

    .title-accent {
      display: inline-block;
      width: 3px;
      height: 18px;
      background: var(--gold);
      border-radius: 2px;
      flex-shrink: 0;
    }
  }

  .card-description {
    font-family: var(--font-body);
    font-size: 14px;
    color: var(--ink-light);
    line-height: 1.8;
    margin: 0 0 14px;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .card-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 14px;
    padding-top: 10px;
    border-top: 1px solid rgba(200, 164, 92, 0.2);

    .meta-item {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 12px;
      color: var(--ink-light);

      .meta-dot {
        font-size: 6px;
        color: var(--gold);
      }

      .meta-text {
        font-weight: 500;
      }
    }
  }

  .card-footer {
    margin-top: 14px;
    padding-top: 12px;
    border-top: 1px solid rgba(200, 164, 92, 0.2);
  }
}

// Type modifiers
.info-card.primary {
  border-color: var(--palace-red);
}

.info-card.success {
  border-color: var(--teal);
}

.info-card.warning {
  border-color: var(--gold);
}
</style>
