<template>
  <div class="datapool-container">
    <el-container>
      <!-- 侧边栏 -->
      <el-aside width="250px" class="sidebar">
        <div class="sidebar-header">
          <h3>数据池管理</h3>
        </div>
        
        <el-menu :default-active="activeMenu" class="sidebar-menu">
          <el-menu-item index="data">
            <el-icon><Document /></el-icon>
            <span>数据管理</span>
          </el-menu-item>
          <el-menu-item index="userdata">
            <el-icon><User /></el-icon>
            <span>用户数据</span>
          </el-menu-item>
          <el-menu-item index="aisystem">
            <el-icon><Monitor /></el-icon>
            <span>AI系统</span>
          </el-menu-item>
          <el-menu-item index="permission">
            <el-icon><Key /></el-icon>
            <span>权限管理</span>
          </el-menu-item>
          <el-menu-item index="logs">
            <el-icon><Tickets /></el-icon>
            <span>访问日志</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      
      <!-- 主内容区 -->
      <el-main class="main-content">
        <!-- 数据管理 -->
        <div v-if="activeMenu === 'data'" class="content-section">
          <div class="section-header">
            <h2>数据管理</h2>
            <div class="actions">
              <el-button type="primary" @click="showAddDialog">
                <el-icon><Plus /></el-icon>
                添加数据
              </el-button>
              <el-button @click="showImportDialog">
                <el-icon><Upload /></el-icon>
                导入文件
              </el-button>
            </div>
          </div>
          
          <!-- 搜索栏 -->
          <div class="search-bar">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索数据..."
              @keyup.enter="searchData"
              style="width: 300px; margin-right: 10px"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-select v-model="filterDataType" placeholder="数据类型" clearable style="width: 150px; margin-right: 10px">
              <el-option label="全部" value="" />
              <el-option label="文本" value="text" />
              <el-option label="结构化" value="structured" />
              <el-option label="图像" value="image" />
              <el-option label="音频" value="audio" />
              <el-option label="视频" value="video" />
            </el-select>
            <el-select v-model="filterCategory" placeholder="分类" clearable style="width: 150px; margin-right: 10px">
              <el-option label="全部" value="" />
              <el-option label="文本数据" value="text" />
              <el-option label="结构化数据" value="structured" />
              <el-option label="图像数据" value="image" />
              <el-option label="知识库" value="knowledge" />
            </el-select>
            <el-button type="primary" @click="searchData">搜索</el-button>
          </div>
          
          <!-- 数据列表 -->
          <el-table :data="dataList" style="width: 100%" v-loading="loading">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="title" label="标题" min-width="200" />
            <el-table-column prop="dataType" label="类型" width="100" />
            <el-table-column prop="category" label="分类" width="120" />
            <el-table-column prop="format" label="格式" width="80" />
            <el-table-column prop="version" label="版本" width="80" />
            <el-table-column prop="fileSize" label="大小" width="100">
              <template #default="scope">
                {{ formatFileSize(scope.row.fileSize) }}
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="180">
              <template #default="scope">
                {{ formatDate(scope.row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="scope">
                <el-button size="small" @click="viewData(scope.row)">查看</el-button>
                <el-button size="small" type="primary" @click="editData(scope.row)">编辑</el-button>
                <el-button size="small" type="danger" @click="deleteData(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <!-- 分页 -->
          <div class="pagination">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadData"
              @current-change="loadData"
            />
          </div>
        </div>
        
        <!-- AI系统管理 -->
        <div v-if="activeMenu === 'aisystem'" class="content-section">
          <div class="section-header">
            <h2>AI系统管理</h2>
            <el-button type="primary" @click="showRegisterDialog">
              <el-icon><Plus /></el-icon>
              注册系统
            </el-button>
          </div>
          
          <el-table :data="aiSystems" style="width: 100%" v-loading="loading">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="systemName" label="系统名称" min-width="150" />
            <el-table-column prop="systemType" label="类型" width="120" />
            <el-table-column prop="apiKey" label="API Key" min-width="200">
              <template #default="scope">
                <el-text truncated>{{ scope.row.apiKey }}</el-text>
              </template>
            </el-table-column>
            <el-table-column prop="rateLimit" label="速率限制" width="100" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="scope">
                <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
                  {{ scope.row.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="lastAccessAt" label="最后访问" width="180">
              <template #default="scope">
                {{ formatDate(scope.row.lastAccessAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="250" fixed="right">
              <template #default="scope">
                <el-button size="small" @click="viewSystem(scope.row)">详情</el-button>
                <el-button size="small" type="warning" @click="regenerateKey(scope.row)">重置密钥</el-button>
                <el-button size="small" type="danger" @click="deleteSystem(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        
        <!-- 权限管理 -->
        <div v-if="activeMenu === 'permission'" class="content-section">
          <div class="section-header">
            <h2>权限管理</h2>
            <el-button type="primary" @click="showPermissionDialog">
              <el-icon><Plus /></el-icon>
              授权
            </el-button>
          </div>
          
          <el-table :data="permissions" style="width: 100%" v-loading="loading">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="aiSystemId" label="AI系统ID" width="100" />
            <el-table-column prop="dataId" label="数据ID" width="100" />
            <el-table-column prop="dataCategory" label="数据分类" width="120" />
            <el-table-column prop="permissionType" label="权限类型" width="100">
              <template #default="scope">
                <el-tag :type="getPermissionTagType(scope.row.permissionType)">
                  {{ scope.row.permissionType }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdBy" label="授权人" width="100" />
            <el-table-column prop="createdAt" label="授权时间" width="180">
              <template #default="scope">
                {{ formatDate(scope.row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="scope">
                <el-button size="small" type="danger" @click="revokePermission(scope.row)">撤销</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        
        <!-- 访问日志 -->
        <div v-if="activeMenu === 'logs'" class="content-section">
          <div class="section-header">
            <h2>访问日志</h2>
          </div>
          
          <el-table :data="accessLogs" style="width: 100%" v-loading="loading">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="aiSystemId" label="AI系统" width="100" />
            <el-table-column prop="dataId" label="数据ID" width="100" />
            <el-table-column prop="accessType" label="访问类型" width="100">
              <template #default="scope">
                <el-tag :type="getAccessTypeTagType(scope.row.accessType)">
                  {{ scope.row.accessType }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="requestMethod" label="方法" width="80" />
            <el-table-column prop="requestPath" label="路径" min-width="200" />
            <el-table-column prop="responseStatus" label="状态码" width="80">
              <template #default="scope">
                <el-tag :type="getStatusTagType(scope.row.responseStatus)">
                  {{ scope.row.responseStatus }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="responseTime" label="响应时间(ms)" width="120" />
            <el-table-column prop="ipAddress" label="IP地址" width="130" />
            <el-table-column prop="accessedAt" label="访问时间" width="180">
              <template #default="scope">
                {{ formatDate(scope.row.accessedAt) }}
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 用户数据管理（UserDataRecord） -->
        <div v-if="activeMenu === 'userdata'" class="content-section">
          <div class="section-header">
            <h2>用户数据管理</h2>
            <div class="actions">
              <span class="text-muted-small">自动采集</span>
              <el-switch v-model="autoSaveEnabled" @change="onToggleAutoSave" />
              <el-button @click="exportUserDataHandler">
                <el-icon><Download /></el-icon>
                导出
              </el-button>
              <el-button
                type="danger"
                :disabled="userDataSelection.length === 0"
                @click="batchDeleteUserData"
              >
                批量删除{{ userDataSelection.length ? `（${userDataSelection.length}）` : '' }}
              </el-button>
            </div>
          </div>

          <!-- 统计卡片 -->
          <div class="stats-cards">
            <div class="stat-card">
              <div class="stat-value">{{ userDataStats.totalRecords || 0 }}</div>
              <div class="stat-label">总记录数</div>
            </div>
            <div class="stat-card">
              <div class="stat-value">{{ userDataStats.backedUpRecords || 0 }}</div>
              <div class="stat-label">已备份</div>
            </div>
            <div class="stat-card">
              <div class="stat-value">{{ userDataStats.validatedRecords || 0 }}</div>
              <div class="stat-label">已验证</div>
            </div>
            <div class="stat-card">
              <div class="stat-value">{{ userDataStats.userCount || 0 }}</div>
              <div class="stat-label">覆盖用户</div>
            </div>
            <div class="stat-card">
              <div class="stat-value">{{ (userDataStats.byType || []).length }}</div>
              <div class="stat-label">数据类型</div>
            </div>
          </div>

          <!-- 筛选栏 -->
          <div class="search-bar userdata-filter">
            <el-input
              v-model="udKeyword"
              placeholder="搜索内容 / 标签..."
              clearable
              style="width: 220px; margin-right: 10px"
              @keyup.enter="searchUserData"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-select v-model="udDataType" placeholder="数据类型" clearable style="width: 160px; margin-right: 10px">
              <el-option
                v-for="t in udTypeOptions"
                :key="t"
                :label="getDataTypeLabel(t)"
                :value="t"
              />
            </el-select>
            <el-select v-model="udStatus" placeholder="状态" clearable style="width: 130px; margin-right: 10px">
              <el-option label="已验证" value="VALIDATED" />
              <el-option label="活跃" value="ACTIVE" />
              <el-option label="待处理" value="PENDING" />
              <el-option label="已归档" value="ARCHIVED" />
            </el-select>
            <el-input-number
              v-model="udUserId"
              :min="1"
              placeholder="用户ID"
              controls-position="right"
              style="width: 140px; margin-right: 10px"
            />
            <el-date-picker
              v-model="udDateRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              style="margin-right: 10px"
            />
            <el-button type="primary" @click="searchUserData">搜索</el-button>
            <el-button @click="resetUserDataFilters">重置</el-button>
          </div>

          <!-- 数据列表 -->
          <el-table
            :data="userDataList"
            style="width: 100%"
            v-loading="udLoading"
            @selection-change="onUserdataSelectionChange"
          >
            <el-table-column type="selection" width="45" />
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="userId" label="用户ID" width="90" />
            <el-table-column prop="dataType" label="数据类型" width="130">
              <template #default="scope">
                <el-tag size="small">{{ getDataTypeLabel(scope.row.dataType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="content" label="内容摘要" min-width="220">
              <template #default="scope">
                <el-text
                  truncated
                  style="cursor: pointer"
                  @click="viewUserData(scope.row)"
                >{{ truncateContent(scope.row.content, 50) }}</el-text>
              </template>
            </el-table-column>
            <el-table-column prop="integrityScore" label="完整性" width="120">
              <template #default="scope">
                <el-tag size="small" :type="getIntegrityTagType(scope.row.integrityScore)">
                  {{ getIntegrityPercent(scope.row.integrityScore) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="scope">
                <el-tag size="small" :type="getRecordStatusTagType(scope.row.status)">
                  {{ getStatusLabel(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="backupStatus" label="备份" width="100">
              <template #default="scope">
                <el-tag size="small" :type="getBackupStatusTagType(scope.row.backupStatus)">
                  {{ getBackupStatusLabel(scope.row.backupStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="captureTime" label="采集时间" width="170">
              <template #default="scope">
                {{ formatDate(scope.row.captureTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="scope">
                <el-button size="small" @click="viewUserData(scope.row)">查看</el-button>
                <el-popconfirm
                  title="确定删除该记录？"
                  @confirm="deleteUserData(scope.row)"
                >
                  <template #reference>
                    <el-button size="small" type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无用户数据" />
            </template>
          </el-table>

          <!-- 分页 -->
          <div class="pagination">
            <el-pagination
              v-model:current-page="udPage"
              v-model:page-size="udSize"
              :page-sizes="[10, 20, 50]"
              :total="udTotal"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadUserDataList"
              @current-change="loadUserDataList"
            />
          </div>
        </div>
      </el-main>
    </el-container>
    
    <!-- 添加数据对话框 -->
    <el-dialog v-model="addDialogVisible" title="添加数据" width="600px">
      <el-form :model="addForm" label-width="100px">
        <el-form-item label="标题">
          <el-input v-model="addForm.title" placeholder="请输入数据标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="addForm.content"
            type="textarea"
            :rows="10"
            placeholder="请输入数据内容"
          />
        </el-form-item>
        <el-form-item label="数据类型">
          <el-select v-model="addForm.dataType" placeholder="请选择数据类型">
            <el-option label="文本" value="text" />
            <el-option label="结构化" value="structured" />
            <el-option label="图像" value="image" />
            <el-option label="音频" value="audio" />
            <el-option label="视频" value="video" />
            <el-option label="文档" value="document" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="addForm.category" placeholder="请选择分类">
            <el-option label="文本数据" value="text" />
            <el-option label="结构化数据" value="structured" />
            <el-option label="图像数据" value="image" />
            <el-option label="知识库" value="knowledge" />
            <el-option label="模型数据" value="model" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="addForm.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAddData">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 注册AI系统对话框 -->
    <el-dialog v-model="registerDialogVisible" title="注册AI系统" width="500px">
      <el-form :model="registerForm" label-width="100px">
        <el-form-item label="系统名称">
          <el-input v-model="registerForm.systemName" placeholder="请输入系统名称" />
        </el-form-item>
        <el-form-item label="系统类型">
          <el-select v-model="registerForm.systemType" placeholder="请选择系统类型">
            <el-option label="聊天机器人" value="chatbot" />
            <el-option label="助手" value="assistant" />
            <el-option label="分析系统" value="analytics" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="registerForm.description" type="textarea" :rows="3" placeholder="请输入系统描述" />
        </el-form-item>
        <el-form-item label="速率限制">
          <el-input-number v-model="registerForm.rateLimit" :min="1" :max="10000" />
          <span class="text-muted-small">次/分钟</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRegister">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 授权对话框 -->
    <el-dialog v-model="permissionDialogVisible" title="授权管理" width="500px">
      <el-form :model="permissionForm" label-width="100px">
        <el-form-item label="AI系统">
          <el-select v-model="permissionForm.aiSystemId" placeholder="请选择AI系统">
            <el-option
              v-for="sys in aiSystems"
              :key="sys.id"
              :label="sys.systemName"
              :value="sys.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数据ID">
          <el-input v-model="permissionForm.dataId" placeholder="留空表示全局权限" />
        </el-form-item>
        <el-form-item label="权限类型">
          <el-select v-model="permissionForm.permissionType" placeholder="请选择权限类型">
            <el-option label="读取" value="read" />
            <el-option label="写入" value="write" />
            <el-option label="删除" value="delete" />
            <el-option label="管理" value="admin" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPermission">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 查看数据详情对话框 -->
    <el-dialog v-model="viewDialogVisible" title="数据详情" width="800px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="ID">{{ viewDataInfo.id }}</el-descriptions-item>
        <el-descriptions-item label="标题">{{ viewDataInfo.title }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ viewDataInfo.dataType }}</el-descriptions-item>
        <el-descriptions-item label="格式">{{ viewDataInfo.format }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ viewDataInfo.category }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ viewDataInfo.version }}</el-descriptions-item>
        <el-descriptions-item label="大小">{{ formatFileSize(viewDataInfo.fileSize) }}</el-descriptions-item>
        <el-descriptions-item label="创建者">{{ viewDataInfo.createdBy }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(viewDataInfo.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDate(viewDataInfo.updatedAt) }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ viewDataInfo.description }}</el-descriptions-item>
        <el-descriptions-item label="内容" :span="2">
          <el-input
            :model-value="viewDataInfo.content"
            type="textarea"
            :rows="10"
            readonly
          />
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 用户数据详情对话框 -->
    <el-dialog v-model="udDetailVisible" title="用户数据详情" width="800px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="ID">{{ udDetail.id }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ udDetail.userId }}</el-descriptions-item>
        <el-descriptions-item label="数据类型">
          <el-tag size="small">{{ getDataTypeLabel(udDetail.dataType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="分组ID">{{ udDetail.groupId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag size="small" :type="getRecordStatusTagType(udDetail.status)">{{ getStatusLabel(udDetail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备份状态">
          <el-tag size="small" :type="getBackupStatusTagType(udDetail.backupStatus)">{{ getBackupStatusLabel(udDetail.backupStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="完整性评分">
          <el-tag size="small" :type="getIntegrityTagType(udDetail.integrityScore)">{{ getIntegrityPercent(udDetail.integrityScore) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="标签">{{ udDetail.tags || '-' }}</el-descriptions-item>
        <el-descriptions-item label="采集时间">{{ formatDate(udDetail.captureTime) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(udDetail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="内容" :span="2">
          <el-input
            :model-value="maskSensitive(udDetail.content) || ''"
            type="textarea"
            :rows="8"
            readonly
          />
        </el-descriptions-item>
        <el-descriptions-item label="元数据(JSON)" :span="2">
          <pre class="metadata-pre">{{ prettyMetadata(udDetail.metadata) }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document,
  Monitor,
  Key,
  Tickets,
  Plus,
  Upload,
  Search,
  User,
  Download
} from '@element-plus/icons-vue'
import request from '../utils/request'
import {
  getUserDataRecords,
  getUserDataRecordDetail,
  deleteUserDataRecord,
  batchDeleteUserDataRecords,
  getUserDataStats,
  getUserDataTypes,
  getDataPoolConfig,
  toggleAutoSave,
  exportUserData
} from '../api/userData'
import {
  getDataTypeLabel,
  getStatusLabel,
  getBackupStatusLabel,
  getIntegrityPercent,
  getIntegrityTagType,
  getStatusTagType as getRecordStatusTagType,
  getBackupStatusTagType,
  truncateContent,
  maskSensitive,
  buildRecordQueryParams
} from '../utils/userDataManage'

// 状态
const activeMenu = ref('data')
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 数据列表
const dataList = ref([])
const aiSystems = ref([])
const permissions = ref([])
const accessLogs = ref([])

// 搜索和过滤
const searchKeyword = ref('')
const filterDataType = ref('')
const filterCategory = ref('')

// 对话框
const addDialogVisible = ref(false)
const registerDialogVisible = ref(false)
const permissionDialogVisible = ref(false)
const viewDialogVisible = ref(false)

// 表单
const addForm = ref({
  title: '',
  content: '',
  dataType: 'text',
  category: '',
  description: ''
})

const registerForm = ref({
  systemName: '',
  systemType: 'chatbot',
  description: '',
  rateLimit: 100
})

const permissionForm = ref({
  aiSystemId: null,
  dataId: null,
  permissionType: 'read'
})

const viewDataInfo = ref({})

// ===== 用户数据管理（UserDataRecord） =====
const udLoading = ref(false)
const userDataList = ref([])
const udTotal = ref(0)
const udPage = ref(1)
const udSize = ref(10)
const udKeyword = ref('')
const udDataType = ref('')
const udStatus = ref('')
const udUserId = ref(null)
const udDateRange = ref(null)
const udTypeOptions = ref([])
const userDataStats = ref({})
const autoSaveEnabled = ref(false)
const userDataSelection = ref([])
const udDetailVisible = ref(false)
const udDetail = ref({})

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value,
      dataType: filterDataType.value,
      category: filterCategory.value
    }
    
    const response = await request.get('/api/admin/data-pool/records', { params })
    
    if (response.code === 200) {
      dataList.value = response.data.records
      total.value = response.data.total
    }
  } catch (error) {
    ElMessage.error('加载数据失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

// 加载AI系统
const loadAiSystems = async () => {
  loading.value = true
  try {
    const response = await request.get('/api/aisystem/list')
    
    if (response.code === 200) {
      aiSystems.value = response.data
    }
  } catch (error) {
    ElMessage.error('加载AI系统失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

// 搜索数据
const searchData = () => {
  currentPage.value = 1
  loadData()
}

// 显示添加对话框
const showAddDialog = () => {
  addForm.value = {
    title: '',
    content: '',
    dataType: 'text',
    category: '',
    description: ''
  }
  addDialogVisible.value = true
}

// 提交添加数据
const submitAddData = async () => {
  try {
    const response = await request.post('/api/admin/data-pool/records', addForm.value)
    
    if (response.code === 200) {
      ElMessage.success('添加成功')
      addDialogVisible.value = false
      loadData()
    } else {
      ElMessage.error(response.msg || '添加失败')
    }
  } catch (error) {
    ElMessage.error('添加失败: ' + error.message)
  }
}

// 查看数据
const viewData = async (row) => {
  try {
    const response = await request.get(`/api/admin/data-pool/records/${row.id}`)
    
    if (response.code === 200) {
      viewDataInfo.value = response.data
      viewDialogVisible.value = true
    }
  } catch (error) {
    ElMessage.error('加载数据详情失败: ' + error.message)
  }
}

// 编辑数据
const editData = (row) => {
  // TODO: 实现编辑功能
  ElMessage.info('编辑功能开发中')
}

// 删除数据
const deleteData = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这条数据吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const response = await request.delete(`/api/admin/data-pool/records/${row.id}`)
    
    if (response.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    } else {
      ElMessage.error(response.msg || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + error.message)
    }
  }
}

// 显示注册对话框
const showRegisterDialog = () => {
  registerForm.value = {
    systemName: '',
    systemType: 'chatbot',
    description: '',
    rateLimit: 100
  }
  registerDialogVisible.value = true
}

// 提交注册
const submitRegister = async () => {
  try {
    const response = await request.post('/api/aisystem/register', registerForm.value)
    
    if (response.code === 200) {
      ElMessage.success('注册成功')
      registerDialogVisible.value = false
      loadAiSystems()
      
      // 显示API密钥
      ElMessageBox.alert(
        `API Key: ${response.data.apiKey}\nSecret Key: ${response.data.secretKey}`,
        '注册成功 - 请保存密钥',
        {
          confirmButtonText: '确定',
          type: 'success'
        }
      )
    } else {
      ElMessage.error(response.msg || '注册失败')
    }
  } catch (error) {
    ElMessage.error('注册失败: ' + error.message)
  }
}

// 重新生成密钥
const regenerateKey = async (row) => {
  try {
    await ElMessageBox.confirm('确定要重新生成API密钥吗？旧密钥将失效。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const response = await request.post(`/api/aisystem/${row.id}/regenerate-key`)
    
    if (response.code === 200) {
      ElMessage.success('重新生成成功')
      loadAiSystems()
      
      ElMessageBox.alert(
        `新的 API Key: ${response.data.apiKey}\n新的 Secret Key: ${response.data.secretKey}`,
        '密钥已更新 - 请保存新密钥',
        {
          confirmButtonText: '确定',
          type: 'success'
        }
      )
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败: ' + error.message)
    }
  }
}

// 删除系统
const deleteSystem = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这个AI系统吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const response = await request.delete(`/api/aisystem/${row.id}`)
    
    if (response.code === 200) {
      ElMessage.success('删除成功')
      loadAiSystems()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + error.message)
    }
  }
}

// 显示授权对话框
const showPermissionDialog = () => {
  permissionForm.value = {
    aiSystemId: null,
    dataId: null,
    permissionType: 'read'
  }
  permissionDialogVisible.value = true
}

// 提交授权
const submitPermission = async () => {
  try {
    const response = await request.post('/api/aisystem/permission/grant', permissionForm.value)
    
    if (response.code === 200) {
      ElMessage.success('授权成功')
      permissionDialogVisible.value = false
      // TODO: 刷新权限列表
    } else {
      ElMessage.error(response.msg || '授权失败')
    }
  } catch (error) {
    ElMessage.error('授权失败: ' + error.message)
  }
}

// 撤销权限
const revokePermission = async (row) => {
  try {
    await ElMessageBox.confirm('确定要撤销这个权限吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const response = await request.delete(`/api/aisystem/permission/${row.id}`)
    
    if (response.code === 200) {
      ElMessage.success('撤销成功')
      // TODO: 刷新权限列表
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('撤销失败: ' + error.message)
    }
  }
}

// ===== 用户数据管理方法 =====
// 加载统计、类型列表、自动采集开关
const loadUserDataStats = async () => {
  try {
    const [statsRes, typesRes, cfgRes] = await Promise.all([
      getUserDataStats(),
      getUserDataTypes(),
      getDataPoolConfig()
    ])
    if (statsRes.code === 200) userDataStats.value = statsRes.data || {}
    if (typesRes.code === 200) udTypeOptions.value = typesRes.data || []
    if (cfgRes.code === 200) autoSaveEnabled.value = !!(cfgRes.data && cfgRes.data.autoSaveEnabled)
  } catch (e) {
    // 统计加载失败不阻断列表
  }
}

// 加载用户数据列表
const loadUserDataList = async () => {
  udLoading.value = true
  try {
    const params = buildRecordQueryParams({
      page: udPage.value,
      size: udSize.value,
      userId: udUserId.value,
      dataType: udDataType.value,
      keyword: udKeyword.value
    })
    const res = await getUserDataRecords(params)
    if (res.code === 200) {
      userDataList.value = (res.data && res.data.records) || []
      udTotal.value = (res.data && res.data.total) || 0
    } else {
      ElMessage.error(res.msg || '加载用户数据失败')
    }
  } catch (e) {
    ElMessage.error('加载用户数据失败')
  } finally {
    udLoading.value = false
  }
}

// 搜索 / 重置
const searchUserData = () => {
  udPage.value = 1
  loadUserDataList()
}

const resetUserDataFilters = () => {
  udKeyword.value = ''
  udDataType.value = ''
  udStatus.value = ''
  udUserId.value = null
  udDateRange.value = null
  searchUserData()
}

// 多选
const onUserdataSelectionChange = (rows) => {
  userDataSelection.value = rows || []
}

// 查看详情
const viewUserData = async (row) => {
  try {
    const res = await getUserDataRecordDetail(row.id)
    if (res.code === 200) {
      udDetail.value = res.data || {}
      udDetailVisible.value = true
    } else {
      ElMessage.error(res.msg || '加载详情失败')
    }
  } catch (e) {
    ElMessage.error('加载详情失败')
  }
}

// 单条删除
const deleteUserData = async (row) => {
  try {
    const res = await deleteUserDataRecord(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadUserDataList()
      loadUserDataStats()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

// 批量删除
const batchDeleteUserData = async () => {
  const ids = (userDataSelection.value || []).map(r => r.id)
  if (!ids.length) return
  try {
    const res = await batchDeleteUserDataRecords(ids)
    if (res.code === 200) {
      ElMessage.success(`已删除 ${ids.length} 条记录`)
      loadUserDataList()
      loadUserDataStats()
    } else {
      ElMessage.error(res.msg || '批量删除失败')
    }
  } catch (e) {
    ElMessage.error('批量删除失败')
  }
}

// 切换自动采集
const onToggleAutoSave = async (val) => {
  try {
    const res = await toggleAutoSave(val)
    if (res.code === 200) {
      ElMessage.success(val ? '自动采集已开启' : '自动采集已关闭')
      loadUserDataStats()
    } else {
      ElMessage.error(res.msg || '操作失败')
      autoSaveEnabled.value = !val
    }
  } catch (e) {
    autoSaveEnabled.value = !val
    ElMessage.error('操作失败')
  }
}

// 导出（后端返回 JSON，前端触发文件下载）
const exportUserDataHandler = async () => {
  try {
    const res = await exportUserData()
    if (res.code === 200) {
      const payload = res.data || { records: [], total: 0 }
      const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `user-data-export-${Date.now()}.json`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
      ElMessage.success('导出成功')
    } else {
      ElMessage.error(res.msg || '导出失败')
    }
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

// 元数据 JSON 格式化展示
const prettyMetadata = (meta) => {
  if (!meta) return '-'
  try {
    return JSON.stringify(JSON.parse(meta), null, 2)
  } catch (e) {
    return String(meta)
  }
}

// 切换到用户数据标签页时加载
watch(activeMenu, (val) => {
  if (val === 'userdata') {
    loadUserDataStats()
    loadUserDataList()
  }
})

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

// 获取权限标签类型
const getPermissionTagType = (type) => {
  const types = {
    read: 'info',
    write: 'warning',
    delete: 'danger',
    admin: 'success'
  }
  return types[type] || 'info'
}

// 获取访问类型标签类型
const getAccessTypeTagType = (type) => {
  const types = {
    read: 'info',
    write: 'warning',
    delete: 'danger',
    query: 'success',
    upload: 'primary',
    download: ''
  }
  return types[type] || 'info'
}

// 获取状态码标签类型
const getStatusTagType = (status) => {
  if (status >= 200 && status < 300) return 'success'
  if (status >= 400 && status < 500) return 'warning'
  if (status >= 500) return 'danger'
  return 'info'
}

// 显示导入对话框
const showImportDialog = () => {
  ElMessage.info('文件导入功能开发中')
}

// 查看系统详情
const viewSystem = (row) => {
  ElMessage.info('系统详情功能开发中')
}

// 初始化
onMounted(() => {
  loadData()
  loadAiSystems()
})
</script>

<style>
.datapool-container {
  height: 100vh;
  background-color: var(--bg-secondary);
}

.sidebar {
  background-color: var(--card-bg);
  border-right: 1px solid var(--border-primary);
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid var(--border-primary);
}

.sidebar-header h3 {
  margin: 0;
  color: var(--text-primary);
}

.sidebar-menu {
  border: none;
}

.main-content {
  padding: 20px;
  background-color: var(--bg-secondary);
}

.content-section {
  background-color: var(--card-bg);
  border-radius: 8px;
  padding: 20px;
  box-shadow: var(--shadow-md);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid var(--border-primary);
}

.section-header h2 {
  margin: 0;
  color: var(--text-primary);
}

.actions {
  display: flex;
  gap: 10px;
}

.search-bar {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.userdata-filter {
  flex-wrap: wrap;
  gap: 0;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-primary);
  border-radius: 8px;
  padding: 16px;
  text-align: center;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-secondary, #909399);
}

.metadata-pre {
  margin: 0;
  max-height: 240px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
  background-color: var(--bg-secondary);
  padding: 10px;
  border-radius: 4px;
}

.text-muted-small {
  font-size: 13px;
  color: var(--text-secondary, #909399);
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
