<template>
  <div class="kb-container">
    <div class="kb-header">
      <button class="back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon><span>返回</span>
      </button>
      <h1>知识库</h1>
    </div>

    <el-row :gutter="16">
      <!-- 左侧：文档列表管理 -->
      <el-col :span="12">
        <el-card shadow="never" class="kb-card">
          <template #header>
            <div class="card-header">
              <span>文档列表</span>
              <el-upload
                :show-file-list="false"
                :auto-upload="false"
                :on-change="onFileChange"
                accept=".txt,.pdf,.docx,.xlsx,.pptx,.md"
              >
                <el-button type="primary" :loading="uploading">上传文档</el-button>
              </el-upload>
            </div>
          </template>

          <!-- 上传进度条 -->
          <div v-if="uploading || uploadPercent > 0" class="upload-progress">
            <el-progress :percentage="uploadPercent" :status="uploadError ? 'exception' : ''" />
          </div>

          <el-table :data="docs" v-loading="loading" stripe size="small" empty-text="暂无文档，点击上传">
            <el-table-column prop="title" label="文件名" min-width="140" show-overflow-tooltip />
            <el-table-column label="大小" width="90">
              <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="docStatusType(row.status)" size="small">{{ docStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="viewSummary(row)">摘要</el-button>
                <el-button link type="primary" size="small" :disabled="!canReparse(row.status)" @click="onReparse(row)">重解析</el-button>
                <el-button link type="danger" size="small" @click="onDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 右侧：多文档问答 -->
      <el-col :span="12">
        <el-card shadow="never" class="kb-card">
          <template #header>
            <div class="card-header">
              <span>多文档问答</span>
            </div>
          </template>

          <div class="ask-range">
            <span class="range-label">问答范围：</span>
            <el-radio-group v-model="rangeMode" size="small">
              <el-radio-button value="all">全部文档</el-radio-button>
              <el-radio-button value="selected">指定文档</el-radio-button>
            </el-radio-group>
          </div>

          <el-select
            v-if="rangeMode === 'selected'"
            v-model="selectedDocIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="选择要问答的文档"
            style="width: 100%; margin-bottom: 12px"
          >
            <el-option v-for="d in docs" :key="d.id" :label="d.title" :value="d.id" />
          </el-select>

          <el-input
            v-model="question"
            type="textarea"
            :rows="3"
            placeholder="输入你的问题，将在所选文档范围内检索回答"
            @keydown.enter.ctrl="onAsk"
          />
          <el-button type="primary" :loading="asking" style="margin-top: 10px" @click="onAsk">
            提问（Ctrl+Enter）
          </el-button>

          <div v-if="answer" class="answer-box">
            <div class="answer-title">回答</div>
            <p>{{ answer }}</p>
            <div v-if="sourceChunks.length" class="source-title">引用片段：</div>
            <ul v-if="sourceChunks.length" class="source-list">
              <li v-for="(s, i) in sourceChunks" :key="i">
                <el-tag size="small" style="margin-right:6px">#{{ s.chunkIndex }}</el-tag>
                {{ s.content }}
              </li>
            </ul>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 摘要弹窗 -->
    <el-dialog v-model="summaryVisible" title="文档摘要" width="560px" append-to-body>
      <p><b>文件名：</b>{{ summary.title }}</p>
      <p><b>类型：</b>{{ summary.fileType }}　<b>分块数：</b>{{ summary.chunkCount }}</p>
      <p><b>状态：</b>{{ docStatusLabel(summary.status) }}</p>
      <p v-if="summary.errorMessage"><b>错误：</b>{{ summary.errorMessage }}</p>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listDocuments,
  deleteDocument,
  reparseDocument,
  uploadDocument,
  askAcrossDocuments
} from '@/api/knowledge'
import { formatFileSize, docStatusLabel, docStatusType, canReparse, buildAskBody } from '@/utils/kbUpload'

const router = useRouter()
const docs = ref([])
const loading = ref(false)
const uploading = ref(false)
const uploadPercent = ref(0)
const uploadError = ref(false)

const rangeMode = ref('all')
const selectedDocIds = ref([])
const question = ref('')
const asking = ref(false)
const answer = ref('')
const sourceChunks = ref([])

const summaryVisible = ref(false)
const summary = ref({})

const goBack = () => router.push('/chat')

async function loadDocs () {
  loading.value = true
  try {
    const res = await listDocuments(1, 50)
    if (res.code === 200) {
      docs.value = res.data || []
    }
  } catch (e) {
    ElMessage.error('加载文档列表失败')
  } finally {
    loading.value = false
  }
}

async function onFileChange (file) {
  const raw = file.raw
  if (!raw) return
  uploading.value = true
  uploadError.value = false
  uploadPercent.value = 0
  try {
    await uploadDocument(raw, raw.name, (pct) => { uploadPercent.value = pct })
    ElMessage.success('上传成功')
    await loadDocs()
  } catch (e) {
    uploadError.value = true
    ElMessage.error(e.message || '上传失败')
  } finally {
    uploading.value = false
    setTimeout(() => { uploadPercent.value = 0 }, 800)
  }
}

function viewSummary (row) {
  summary.value = row
  summaryVisible.value = true
}

async function onReparse (row) {
  try {
    await reparseDocument(row.id)
    ElMessage.success('已触发重新解析')
    await loadDocs()
  } catch (e) {
    ElMessage.error('重新解析失败')
  }
}

async function onDelete (row) {
  try {
    await ElMessageBox.confirm(`确定删除文档「${row.title}」吗？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await deleteDocument(row.id)
    ElMessage.success('已删除')
    docs.value = docs.value.filter(d => d.id !== row.id)
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

async function onAsk () {
  const body = buildAskBody(
    question.value,
    rangeMode.value === 'selected' ? selectedDocIds.value : null,
    3
  )
  if (!body.query) {
    ElMessage.warning('请输入问题')
    return
  }
  asking.value = true
  answer.value = ''
  sourceChunks.value = []
  try {
    const res = await askAcrossDocuments(body.query, body.documentIds, body.topK)
    if (res.code === 200) {
      answer.value = res.data.answer
      sourceChunks.value = res.data.sourceChunks || []
    } else {
      ElMessage.error(res.message || '问答失败')
    }
  } catch (e) {
    ElMessage.error('问答失败')
  } finally {
    asking.value = false
  }
}

onMounted(loadDocs)
</script>

<style scoped>
.kb-container { max-width: 1100px; margin: 0 auto; padding: 20px; }
.kb-header { display: flex; align-items: center; gap: 12px; margin-bottom: 18px; }
.back-btn { display: flex; align-items: center; gap: 6px; padding: 8px 14px; background: rgba(0,0,0,0.05); border: none; border-radius: 8px; cursor: pointer; }
.kb-card { margin-bottom: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.upload-progress { margin-bottom: 12px; }
.ask-range { margin-bottom: 12px; display: flex; align-items: center; gap: 8px; }
.answer-box { margin-top: 16px; padding: 12px; background: rgba(102,126,234,0.06); border-radius: 8px; }
.answer-title { font-weight: bold; margin-bottom: 6px; }
.source-title { margin-top: 10px; font-size: 13px; color: #666; }
.source-list { padding-left: 18px; font-size: 13px; color: #555; }
.source-list li { margin: 4px 0; }
</style>
