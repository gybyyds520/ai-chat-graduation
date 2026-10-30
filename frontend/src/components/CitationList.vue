<template>
  <div v-if="citations && citations.length" class="citation-list">
    <div class="citation-list-title">参考来源（{{ citations.length }}）</div>
    <ol class="citation-items">
      <li v-for="c in citations" :key="c.index" class="citation-item">
        <span class="citation-index">[{{ c.index }}]</span>
        <a
          v-if="safeUrl(c.url)"
          :href="c.url"
          target="_blank"
          rel="noopener noreferrer"
          class="citation-title"
        >{{ c.title || '无标题' }}</a>
        <span v-else class="citation-title">{{ c.title || '无标题' }}</span>
        <span v-if="c.source" class="citation-source">· {{ c.source }}</span>
        <button class="citation-copy" @click="copyOne(c)" title="复制引用格式">复制引用</button>
        <div v-if="c.snippet" class="citation-snippet" @click="toggle(c.index)">
          {{ expanded.has(c.index) ? c.snippet : truncate(c.snippet) }}
          <span v-if="c.snippet.length > 60" class="citation-toggle">
            {{ expanded.has(c.index) ? '收起' : '展开' }}
          </span>
        </div>
      </li>
    </ol>
  </div>
</template>

<script>
import { isSafeUrl, buildCitationText } from '@/utils/citation.js'

export default {
  name: 'CitationList',
  props: {
    citations: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      expanded: new Set()
    }
  },
  methods: {
    safeUrl(url) {
      return isSafeUrl(url)
    },
    truncate(text) {
      return text && text.length > 60 ? text.slice(0, 60) + '…' : text
    },
    toggle(index) {
      if (this.expanded.has(index)) {
        this.expanded.delete(index)
      } else {
        this.expanded.add(index)
      }
    },
    copyOne(c) {
      const text = buildCitationText(c)
      this.copyToClipboard(text)
    },
    async copyToClipboard(text) {
      try {
        if (navigator.clipboard && window.isSecureContext) {
          await navigator.clipboard.writeText(text)
        } else {
          const ta = document.createElement('textarea')
          ta.value = text
          ta.style.position = 'fixed'
          ta.style.opacity = '0'
          document.body.appendChild(ta)
          ta.select()
          document.execCommand('copy')
          document.body.removeChild(ta)
        }
      } catch (e) {
        /* 静默失败，避免打断用户 */
      }
    }
  }
}
</script>

<style scoped>
.citation-list {
  margin-top: 8px;
  padding: 8px 10px;
  border-top: 1px dashed #cbd5e1;
  font-size: 12px;
  color: #475569;
}
.citation-list-title {
  font-weight: 600;
  margin-bottom: 4px;
}
.citation-items {
  margin: 0;
  padding-left: 18px;
}
.citation-item {
  margin: 3px 0;
}
.citation-index {
  color: #2563eb;
  font-weight: 600;
  margin-right: 2px;
}
.citation-title {
  color: #1d4ed8;
  text-decoration: none;
}
.citation-title:hover {
  text-decoration: underline;
}
.citation-source {
  margin-left: 4px;
  color: #94a3b8;
}
.citation-copy {
  margin-left: 6px;
  border: none;
  background: #eef2ff;
  color: #4f46e5;
  border-radius: 4px;
  padding: 0 6px;
  cursor: pointer;
  font-size: 11px;
}
.citation-copy:hover {
  background: #e0e7ff;
}
.citation-snippet {
  color: #64748b;
  cursor: pointer;
  margin-top: 1px;
}
.citation-toggle {
  color: #2563eb;
  margin-left: 4px;
}
</style>
