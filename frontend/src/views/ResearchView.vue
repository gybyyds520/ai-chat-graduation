<template>
  <div class="research-view">
    <header class="research-header">
      <h2>深度研究</h2>
      <p class="research-sub">多轮自主检索 + 结构化整合，离线可测（mock 搜索）</p>
    </header>

    <section class="research-panel">
      <textarea
        v-model="query"
        class="research-input"
        placeholder="输入研究问题，例如：人工智能在医疗领域的应用"
        rows="2"
      ></textarea>
      <div class="research-actions">
        <label class="research-rounds">
          轮次：
          <select v-model.number="maxRounds">
            <option :value="1">1</option>
            <option :value="2">2</option>
            <option :value="3">3</option>
          </select>
        </label>
        <button class="btn-start" :disabled="starting || !query.trim()" @click="startResearch">
          {{ starting ? '研究中…' : '开始研究' }}
        </button>
      </div>

      <div v-if="status" class="research-progress">
        <span class="badge" :class="statusClass">{{ statusText }}</span>
        <span>进度：{{ status.progress }}%</span>
        <span v-if="status.currentRound">当前轮次：{{ status.currentRound }}/{{ status.maxRounds }}</span>
      </div>
    </section>

    <section v-if="report" class="research-report">
      <h3>研究报告</h3>
      <p class="report-summary">{{ report.summary }}</p>

      <h4>关键发现</h4>
      <ul class="findings">
        <li v-for="(f, i) in report.keyFindings" :key="i">{{ f }}</li>
      </ul>

      <h4>来源（{{ report.sources ? report.sources.length : 0 }}）</h4>
      <CitationList :citations="report.sources || []" />

      <h4>检索轮次详情</h4>
      <table class="round-table">
        <thead>
          <tr><th>轮次</th><th>查询词</th><th>结果数</th><th>Top 标题</th></tr>
        </thead>
        <tbody>
          <tr v-for="(r, i) in report.roundDetails" :key="i">
            <td>{{ r.round }}</td>
            <td>{{ r.query }}</td>
            <td>{{ r.resultCount }}</td>
            <td>{{ (r.topTitles || []).join('；') }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section v-if="history.length" class="research-history">
      <h3>研究历史</h3>
      <ul>
        <li v-for="h in history" :key="h.researchId" @click="loadResult(h.researchId)" class="history-item">
          {{ h.query }} <span class="history-status">{{ h.status }}</span>
        </li>
      </ul>
    </section>
  </div>
</template>

<script>
import request from '@/utils/request.js'
import CitationList from '@/components/CitationList.vue'

export default {
  name: 'ResearchView',
  components: { CitationList },
  data() {
    return {
      query: '',
      maxRounds: 3,
      starting: false,
      status: null,
      report: null,
      history: [],
      pollTimer: null
    }
  },
  computed: {
    statusClass() {
      if (!this.status) return ''
      return this.status.status === 'completed' ? 'done'
        : this.status.status === 'failed' ? 'fail' : 'running'
    },
    statusText() {
      if (!this.status) return ''
      const map = {
        pending: '等待中', searching: '检索中', round2: '第二轮检索',
        integrating: '整合中', completed: '已完成', failed: '失败'
      }
      return map[this.status.status] || this.status.status
    }
  },
  mounted() {
    this.loadHistory()
  },
  beforeUnmount() {
    this.stopPolling()
  },
  methods: {
    async startResearch() {
      if (!this.query.trim()) return
      this.starting = true
      this.report = null
      try {
        const res = await request.post('/api/research/start', {
          query: this.query.trim(),
          maxRounds: this.maxRounds
        })
        const researchId = res && res.data && res.data.researchId
        if (researchId) {
          await this.poll(researchId)
        }
      } catch (e) {
        /* 错误已由拦截器处理 */
      } finally {
        this.starting = false
      }
    },
    async poll(researchId) {
      // 简化版：研究为同步执行，这里轮询直到 completed/failed
      for (let i = 0; i < 20; i++) {
        try {
          const sres = await request.get(`/api/research/${researchId}/status`)
          this.status = sres && sres.data
          if (this.status && (this.status.status === 'completed' || this.status.status === 'failed')) {
            await this.loadResult(researchId)
            await this.loadHistory()
            return
          }
        } catch (e) {
          return
        }
        await new Promise(r => setTimeout(r, 300))
      }
    },
    stopPolling() {
      if (this.pollTimer) {
        clearTimeout(this.pollTimer)
        this.pollTimer = null
      }
    },
    async loadResult(researchId) {
      try {
        const res = await request.get(`/api/research/${researchId}/result`)
        this.report = res && res.data
      } catch (e) {
        /* ignore */
      }
    },
    async loadHistory() {
      try {
        const res = await request.get('/api/research/list', { params: { page: 1, size: 10 } })
        this.history = (res && res.data && res.data.items) || []
      } catch (e) {
        /* ignore */
      }
    }
  }
}
</script>

<style scoped>
.research-view {
  max-width: 860px;
  margin: 0 auto;
  padding: 24px;
}
.research-header h2 { margin: 0 0 4px; }
.research-sub { color: #64748b; margin: 0 0 16px; }
.research-panel, .research-report, .research-history {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 16px;
}
.research-input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 8px;
  font-size: 14px;
}
.research-actions { margin-top: 10px; display: flex; align-items: center; gap: 12px; }
.btn-start {
  background: #2563eb; color: #fff; border: none; border-radius: 8px;
  padding: 8px 18px; cursor: pointer;
}
.btn-start:disabled { background: #93c5fd; cursor: not-allowed; }
.research-progress { margin-top: 12px; display: flex; gap: 16px; align-items: center; }
.badge { padding: 2px 10px; border-radius: 999px; font-size: 12px; }
.badge.running { background: #fef3c7; color: #92400e; }
.badge.done { background: #dcfce7; color: #166534; }
.badge.fail { background: #fee2e2; color: #991b1b; }
.report-summary { background: #f8fafc; padding: 10px; border-radius: 8px; }
.findings li { margin: 4px 0; }
.round-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.round-table th, .round-table td { border: 1px solid #e2e8f0; padding: 6px; text-align: left; }
.history-item { cursor: pointer; margin: 4px 0; }
.history-item:hover { color: #2563eb; }
.history-status { color: #166534; margin-left: 8px; font-size: 12px; }
</style>
