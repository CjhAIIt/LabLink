<template>
  <Teleport to="body">
    <Transition name="mobile-sheet-fade">
      <div v-if="modelValue" class="mobile-action-sheet" role="dialog" aria-modal="true">
        <button class="mobile-action-sheet__mask" type="button" aria-label="关闭" @click="close" />
        <Transition name="mobile-sheet-slide" appear>
          <section class="mobile-action-sheet__panel">
            <header v-if="title || description" class="mobile-action-sheet__header">
              <div>
                <strong v-if="title">{{ title }}</strong>
                <p v-if="description">{{ description }}</p>
              </div>
              <button class="mobile-action-sheet__close" type="button" aria-label="关闭" @click="close">
                <el-icon :size="18"><Close /></el-icon>
              </button>
            </header>
            <div class="mobile-action-sheet__body">
              <slot />
            </div>
            <footer v-if="$slots.footer" class="mobile-action-sheet__footer">
              <slot name="footer" />
            </footer>
          </section>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { Close } from '@element-plus/icons-vue'

const emit = defineEmits(['update:modelValue', 'close'])

defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: ''
  },
  description: {
    type: String,
    default: ''
  }
})

const close = () => {
  emit('update:modelValue', false)
  emit('close')
}
</script>
