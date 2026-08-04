<script setup lang="ts">
interface Props {
  /** 弹窗标题。 */
  title: string
  /** 弹窗说明。 */
  message: string
  /** 确认按钮文案。 */
  confirmLabel?: string
  /** 取消按钮文案。 */
  cancelLabel?: string
  /** 是否使用危险操作色。 */
  danger?: boolean
}

withDefaults(defineProps<Props>(), {
  confirmLabel: '确认',
  cancelLabel: '取消',
  danger: false,
})

const emit = defineEmits<{
  /** 关闭弹窗。 */
  close: []
  /** 确认当前操作。 */
  confirm: []
}>()
</script>

<template>
  <div class="modal-backdrop" role="presentation" @click.self="emit('close')">
    <section
      class="confirm-modal"
      :class="{ 'confirm-modal--danger': danger }"
      role="dialog"
      aria-modal="true"
      :aria-label="title"
    >
      <div class="confirm-modal__mark" aria-hidden="true">✦</div>
      <h2>{{ title }}</h2>
      <p>{{ message }}</p>
      <div class="confirm-modal__actions">
        <button class="confirm-modal__cancel" type="button" @click="emit('close')">
          {{ cancelLabel }}
        </button>
        <button
          class="confirm-modal__confirm"
          type="button"
          @click="emit('confirm')"
        >
          {{ confirmLabel }}
        </button>
      </div>
    </section>
  </div>
</template>
