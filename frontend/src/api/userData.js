import request from '@/utils/request'

/**
 * 管理端「用户数据管理」API 封装（/api/admin/data-pool）。
 * 全部端点后端要求 ADMIN 角色；响应为统一 R{code,msg,data}，
 * request 拦截器已直接返回 R 体，故调用处用 res.code === 200 判断。
 */

// 分页查询用户数据记录：{ page, size, userId?, dataType?, keyword? }
// 返回 data = { records, total, current, size }
export function getUserDataRecords (params) {
  return request.get('/api/admin/data-pool/records', { params })
}

// 记录详情
export function getUserDataRecordDetail (id) {
  return request.get(`/api/admin/data-pool/records/${id}`)
}

// 单条删除
export function deleteUserDataRecord (id) {
  return request.delete(`/api/admin/data-pool/records/${id}`)
}

// 批量删除：后端 @RequestBody List<Long> ids，body 为裸数组
export function batchDeleteUserDataRecords (ids) {
  return request.delete('/api/admin/data-pool/records/batch', { data: ids })
}

// 统计：data = { totalRecords, backedUpRecords, validatedRecords, byType, byUser, userCount }
export function getUserDataStats () {
  return request.get('/api/admin/data-pool/stats')
}

// 数据类型列表：data = string[]
export function getUserDataTypes () {
  return request.get('/api/admin/data-pool/types')
}

// 配置：data = { autoSaveEnabled }
export function getDataPoolConfig () {
  return request.get('/api/admin/data-pool/config')
}

// 切换自动采集：后端为 POST /auto-save/toggle，body { enabled }
export function toggleAutoSave (enabled) {
  return request.post('/api/admin/data-pool/auto-save/toggle', { enabled })
}

// 导出：后端返回 JSON { records, total }，由调用方生成文件下载
export function exportUserData (params) {
  return request.get('/api/admin/data-pool/export', { params })
}

// 重复检查
export function checkDuplicates () {
  return request.get('/api/admin/data-pool/duplicates/check')
}

// 去重
export function removeDuplicates () {
  return request.post('/api/admin/data-pool/duplicates/remove')
}

export default {
  getUserDataRecords,
  getUserDataRecordDetail,
  deleteUserDataRecord,
  batchDeleteUserDataRecords,
  getUserDataStats,
  getUserDataTypes,
  getDataPoolConfig,
  toggleAutoSave,
  exportUserData,
  checkDuplicates,
  removeDuplicates
}
