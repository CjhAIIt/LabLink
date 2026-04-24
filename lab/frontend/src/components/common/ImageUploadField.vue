<template>
  <div class="image-upload-field">
    <div class="field-copy">
      <div class="field-title">{{ title }}</div>
      <div v-if="description" class="field-description">{{ description }}</div>
    </div>

    <FileUpload
      :model-value="fileList"
      action="/api/files/upload"
      :accept="accept"
      :data="{ scene }"
      :limit="1"
      :size-limit="sizeLimit"
      list-type="picture-card"
      :previewable="false"
      :tip="tipText"
      @change="handleChange"
      @remove="handleRemove"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import FileUpload from '@/components/FileUpload.vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  title: { type: String, required: true },
  description: { type: String, default: '' },
  scene: { type: String, default: 'image' },
  accept: { type: String, default: '.jpg,.jpeg,.png,.webp' },
  sizeLimit: { type: Number, default: 5 },
  tip: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue'])

const fileList = computed(() => (props.modelValue ? [props.modelValue] : []))
const tipText = computed(() => props.tip || `支持 ${props.accept}，单张不超过 ${props.sizeLimit}MB`)

const handleChange = (files) => {
  const latest = files?.[files.length - 1]
  emit('update:modelValue', latest?.rawUrl || '')
}

const handleRemove = () => {
  emit('update:modelValue', '')
}
</script>

<style scoped>
.image-upload-field {
  display: grid;
  gap: 12px;
  padding: 16px;
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.96), rgba(255, 255, 255, 0.98));
}

.field-copy {
  display: grid;
  gap: 6px;
}

.field-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.field-description {
  font-size: 13px;
  line-height: 1.6;
  color: #64748b;
}
</style>
