import request from '@/utils/request'
import { progressPercent } from '@/utils/kbUpload'

/**
 * 知识库 API 封装（/api/knowledge）。
 * 上传走原生 XMLHttpRequest 以支持 onprogress 进度回调（axios 也可，但 XHR 更直观）。
 */

// 文档列表（分页）
export function listDocuments (page = 1, size = 20) {
  return request.get(`/api/knowledge/list?page=${page}&size=${size}`)
}

// 文档详情（摘要）
export function getDocument (documentId) {
  return request.get(`/api/knowledge/${documentId}`)
}

// 删除文档
export function deleteDocument (documentId) {
  return request.delete(`/api/knowledge/${documentId}`)
}

// 重新解析
export function reparseDocument (documentId) {
  return request.post(`/api/knowledge/${documentId}/reparse`)
}

/**
 * 上传文档，带进度回调。
 * @param {File} file 文件对象
 * @param {string} title 标题（可空）
 * @param {function(number):void} onProgress 进度回调 0-100
 * @returns {Promise<R>}
 */
export function uploadDocument (file, title = '', onProgress = null) {
  return new Promise((resolve, reject) => {
    const form = new FormData()
    form.append('file', file)
    if (title) form.append('title', title)

    const xhr = new XMLHttpRequest()
    xhr.open('POST', '/api/knowledge/upload')
    const token = localStorage.getItem('token')
    if (token) xhr.setRequestHeader('Authorization', 'Bearer ' + token)

    xhr.upload.onprogress = (e) => {
      if (onProgress && e.lengthComputable) {
        onProgress(progressPercent(e.loaded, e.total))
      }
    }
    xhr.onload = () => {
      try {
        const res = JSON.parse(xhr.responseText)
        if (xhr.status >= 200 && xhr.status < 300 && res.code === 200) {
          resolve(res)
        } else {
          reject(new Error(res.message || '上传失败'))
        }
      } catch (e) {
        reject(e)
      }
    }
    xhr.onerror = () => reject(new Error('网络错误，上传失败'))
    xhr.send(form)
  })
}

// 单文档问答
export function askInDocument (documentId, query, topK = 3) {
  return request.post(`/api/knowledge/${documentId}/ask`, { query, topK })
}

// 多文档问答（documentIds 为空 = 全部文档）
export function askAcrossDocuments (query, documentIds = null, topK = 3) {
  return request.post('/api/knowledge/ask', { query, documentIds, topK })
}

export default {
  listDocuments,
  getDocument,
  deleteDocument,
  reparseDocument,
  uploadDocument,
  askInDocument,
  askAcrossDocuments
}
