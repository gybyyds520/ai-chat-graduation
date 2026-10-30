<template>
  <div class="admin-container">
    <div :class="['admin-sidebar', { collapsed: sidebarCollapsed }]" :style="{ transform: sidebarCollapsed ? 'translateX(-100%)' : 'translateX(0)' }">
      <div class="admin-header">
        <h2>管理员后台</h2>
        <p>欢迎，{{ adminName }}</p>
        <div class="header-actions">
          <el-button
            size="small"
            @click="logViewerVisible = true"
            type="info"
            plain
          >
            主题日志
          </el-button>
        </div>
      </div>
      <div class="admin-menu">
        <div
          :class="['menu-item', { active: currentTab === 'users' }]"
          @click="handleTabChange('users')"
        >
          用户管理
        </div>
        <div
          :class="['menu-item', { active: currentTab === 'characters' }]"
          @click="handleTabChange('characters')"
        >
          AI角色管理
        </div>
        <div
          :class="['menu-item', { active: currentTab === 'aiChat' }]"
          @click="handleTabChange('aiChat')"
        >
          AI聊天记录
        </div>
        <div
          :class="['menu-item', { active: currentTab === 'friendChat' }]"
          @click="handleTabChange('friendChat')"
        >
          好友聊天记录
        </div>
        <div
          :class="['menu-item', { active: currentTab === 'stats' }]"
          @click="handleTabChange('stats')"
        >
                    数据统计
        </div>
        <div
          :class="['menu-item', { active: currentTab === 'dashboard' }]"
          @click="handleTabChange('dashboard')"
        >
          数据看板
        </div>
        <div
          :class="['menu-item', { active: currentTab === 'datapool' }]"
          @click="handleTabChange('datapool')"
        >
          数据池管理
        </div>
        <div
          :class="['menu-item', { active: currentTab === 'aisystem' }]"
          @click="handleTabChange('aisystem')"
        >
          AI系统管理
        </div>
        <div
          :class="['menu-item', { active: currentTab === 'announcements' }]"
          @click="handleTabChange('announcements')"
        >
          公告管理
        </div>
        <div
          :class="['menu-item', { active: currentTab === 'profile' }]"
          @click="handleTabChange('profile')"
        >
          个人中心
        </div>
        <div
          :class="['menu-item', { active: currentTab === 'userData' }]"
          @click="handleTabChange('userData')"
        >
          用户数据管理
        </div>
      </div>
      <div class="admin-footer">
        <el-button type="danger" @click="handleLogout">退出登录</el-button>
      </div>
    </div>

    <div v-if="!sidebarCollapsed" class="sidebar-overlay" @click="sidebarCollapsed = true"></div>

    <div class="admin-main">
      <div class="mobile-header">
        <button class="menu-toggle" @click="sidebarCollapsed = !sidebarCollapsed">
          <span></span>
          <span></span>
          <span></span>
        </button>
        <h3>管理员后台</h3>
      </div>
      <div class="admin-top-bar">
        <div class="chandelier-toggle" @click="toggleTheme" :title="currentTheme === 'dark' ? '切换到白天模式' : '切换到夜晚模式'">
          <div class="chandelier-base">
            <div class="chandelier-glow" :class="{ lit: currentTheme === 'dark' }"></div>
            <div class="chandelier-light" :class="{ off: currentTheme === 'dark' }">
              <div class="light-bulb"></div>
            </div>
            <div class="chandelier-chain"></div>
          </div>
          <span class="toggle-label">{{ currentTheme === 'dark' ? '☾' : '☀' }}</span>
        </div>
      </div>
      <div class="admin-content">
      <div v-if="currentTab === 'users'" class="tab-content">
        <div class="tab-header">
          <h3>用户管理</h3>
          <div class="header-actions">
            <el-button type="primary" @click="showAddUserDialog">添加用户</el-button>
            <el-button @click="loadUsers">刷新列表</el-button>
          </div>
        </div>

        <div class="user-stats-row">
          <el-card class="stat-card">
            <div class="stat-icon free-icon">📊</div>
            <div class="stat-info">
              <div class="stat-value">{{ userCounts.freeCount || 0 }}</div>
              <div class="stat-label">免费版用户</div>
            </div>
          </el-card>
          <el-card class="stat-card">
            <div class="stat-icon pro-icon">✨</div>
            <div class="stat-info">
              <div class="stat-value">{{ userCounts.proCount || 0 }}</div>
              <div class="stat-label">专业版用户</div>
            </div>
          </el-card>
          <el-card class="stat-card">
            <div class="stat-icon enterprise-icon">🏢</div>
            <div class="stat-info">
              <div class="stat-value">{{ userCounts.enterpriseCount || 0 }}</div>
              <div class="stat-label">企业版用户</div>
            </div>
          </el-card>
        </div>

        <div class="version-tabs">
          <el-radio-group v-model="versionTab" @change="loadUsersByVersion">
            <el-radio-button label="FREE">免费版用户</el-radio-button>
            <el-radio-button label="PRO">专业版用户</el-radio-button>
            <el-radio-button label="ENTERPRISE">企业版用户</el-radio-button>
          </el-radio-group>
        </div>

        <el-table :data="filteredUsers" style="width: 100%" v-loading="loadingUsers">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="username" label="用户名" width="150" />
          <el-table-column prop="nickname" label="昵称" width="150" />
          <el-table-column prop="accountStatus" label="账户状态" width="120">
            <template #default="scope">
              <el-tag :type="getAccountStatusType(scope.row.accountStatus)">
                {{ getAccountStatusText(scope.row.accountStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="versionType" label="版本类型" width="120">
            <template #default="scope">
              <el-tag :type="getVersionType(scope.row.versionType)">
                {{ getVersionText(scope.row.versionType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="versionExpireTime" label="有效期至" width="180">
            <template #default="scope">
              {{ formatDate(scope.row.versionExpireTime) || '永久' }}
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="注册时间" width="180">
            <template #default="scope">
              {{ formatDate(scope.row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280">
            <template #default="scope">
              <el-button size="small" type="primary" @click="editUser(scope.row)">编辑</el-button>
              <el-button size="small" type="success" @click="upgradeUser(scope.row)">升级</el-button>
              <el-button size="small" type="danger" @click="deleteUser(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="upgrade-section">
          <el-card title="版本升级充值">
            <div class="upgrade-options">
              <el-card class="upgrade-card pro-card">
                <h4>专业版</h4>
                <div class="price">¥99<span class="period">/月</span></div>
                <ul class="features-list">
                  <li>✅ 高级AI对话功能</li>
                  <li>✅ 无限对话次数</li>
                  <li>✅ 无限自定义角色</li>
                  <li>✅ 高级语音功能</li>
                  <li>✅ 优先技术支持</li>
                  <li>✅ 数据导出功能</li>
                </ul>
                <el-select v-model="proDuration" style="width: 100%; margin-bottom: 10px;">
                  <el-option label="1个月" :value="30" />
                  <el-option label="3个月" :value="90" />
                  <el-option label="6个月" :value="180" />
                  <el-option label="12个月" :value="365" />
                </el-select>
                <el-button type="primary" @click="showUpgradeDialog('PRO')">立即升级</el-button>
              </el-card>
              <el-card class="upgrade-card enterprise-card">
                <h4>企业版</h4>
                <div class="price">¥999<span class="period">/月</span></div>
                <ul class="features-list">
                  <li>✅ 包含全部专业版功能</li>
                  <li>✅ 私有化部署选项</li>
                  <li>✅ 专属客户经理</li>
                  <li>✅ 定制开发支持</li>
                  <li>✅ SLA服务保障</li>
                </ul>
                <el-select v-model="enterpriseDuration" style="width: 100%; margin-bottom: 10px;">
                  <el-option label="1个月" :value="30" />
                  <el-option label="3个月" :value="90" />
                  <el-option label="6个月" :value="180" />
                  <el-option label="12个月" :value="365" />
                </el-select>
                <el-button type="primary" @click="showUpgradeDialog('ENTERPRISE')">立即升级</el-button>
              </el-card>
            </div>
          </el-card>
        </div>

        <div class="payment-review-section">
          <el-card title="充值审核" class="review-card">
            <div class="review-header">
              <span>待审核充值记录: {{ pendingPayments.length }}</span>
              <el-button type="primary" @click="loadPendingPayments">刷新</el-button>
            </div>
            <el-table :data="pendingPayments" style="width: 100%" v-loading="loadingPayments">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="username" label="用户名" width="150" />
              <el-table-column prop="versionType" label="升级版本" width="120">
                <template #default="scope">
                  <el-tag :type="getVersionType(scope.row.versionType)">
                    {{ getVersionText(scope.row.versionType) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="amount" label="金额" width="100" />
              <el-table-column prop="durationDays" label="时长(天)" width="100" />
              <el-table-column prop="paymentMethod" label="支付方式" width="120" />
              <el-table-column prop="paymentTime" label="支付时间" width="180">
                <template #default="scope">
                  {{ formatDate(scope.row.paymentTime) }}
                </template>
              </el-table-column>
              <el-table-column prop="reviewStatus" label="审核状态" width="120">
                <template #default="scope">
                  <el-tag :type="getReviewStatusType(scope.row.reviewStatus)">
                    {{ getReviewStatusText(scope.row.reviewStatus) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200">
                <template #default="scope">
                  <el-button size="small" type="success" @click="approvePayment(scope.row)">通过</el-button>
                  <el-button size="small" type="danger" @click="rejectPayment(scope.row)">拒绝</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>
      </div>

      <div v-if="currentTab === 'characters'" class="tab-content">
        <div class="tab-header">
          <h3>AI角色管理</h3>
          <div class="header-actions">
            <el-button type="primary" @click="showAddCharacterDialog">添加角色</el-button>
            <el-button @click="loadCharacters">刷新列表</el-button>
          </div>
        </div>
        <el-table :data="characters" style="width: 100%" v-loading="loadingCharacters">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column label="头像" width="80">
            <template #default="scope">
              <LocalAvatar :seed="scope.row.name" :size="40" :src="scope.row.avatar" />
            </template>
          </el-table-column>
          <el-table-column prop="name" label="名称" width="150" />
          <el-table-column label="描述" show-overflow-tooltip>
            <template #default="scope">
              {{ scope.row.prompt?.substring(0, 50) }}...
            </template>
          </el-table-column>
          <el-table-column prop="isSystem" label="系统角色" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.isSystem ? 'success' : 'info'">
                {{ scope.row.isSystem ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="scope">
              <el-button size="small" type="primary" @click="editCharacter(scope.row)">编辑</el-button>
              <el-button size="small" type="danger" @click="deleteCharacter(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-if="currentTab === 'aiChat'" class="tab-content">
        <div class="tab-header">
          <h3>AI聊天记录</h3>
          <div class="header-actions">
            <el-button @click="loadAiChatMessages">刷新列表</el-button>
          </div>
        </div>

        <div class="filter-section">
          <el-select v-model="filterUserId" placeholder="选择用户" clearable style="width: 200px; margin-right: 10px;">
            <el-option v-for="user in users" :key="user.id" :label="user.nickname || user.username" :value="user.id" />
          </el-select>
          <el-select v-model="filterCharacterId" placeholder="选择AI角色" clearable style="width: 200px; margin-right: 10px;">
            <el-option v-for="char in characters" :key="char.id" :label="char.name" :value="char.id" />
          </el-select>
          <el-button type="primary" @click="loadAiChatMessages">筛选</el-button>
        </div>

        <div class="session-list" v-if="!selectedSession">
          <div v-for="session in sessions" :key="session.sessionId" class="session-item" @click="selectSession(session)">
            <div class="session-info">
              <div class="session-title">{{ session.userName }} 与 {{ session.characterName }}</div>
              <div class="session-id">会话ID: {{ session.sessionId }}</div>
            </div>
            <el-icon class="session-arrow"><arrow-right /></el-icon>
          </div>
        </div>

        <div v-else class="chat-view">
          <div class="chat-header">
            <el-button @click="backToSessions">
              <el-icon><arrow-left /></el-icon> 返回
            </el-button>
            <span>{{ currentSession?.userName }} 与 {{ currentSession?.characterName }}</span>
          </div>
          <div class="chat-messages" ref="chatMessagesRef">
            <div v-for="(msg, index) in sessionMessages" :key="index" :class="['chat-message', msg.senderType]">
              <div class="message-avatar">
                <LocalAvatar :seed="msg.senderType === 'user' ? 'user_' + currentSession?.userId : 'char_' + currentSession?.characterId" :size="40" />
              </div>
              <div class="message-content">
                <div class="message-text">{{ msg.content }}</div>
                <div class="message-time">{{ formatMessageTime(msg.createTime) }}</div>
              </div>
            </div>
          </div>
        </div>

        <el-table v-if="!selectedSession" :data="aiChatMessages" style="width: 100%" v-loading="loadingAiChat">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="sessionId" label="会话ID" width="120" />
          <el-table-column prop="senderType" label="发送者" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.senderType === 'assistant' ? 'success' : 'primary'">
                {{ scope.row.senderType === 'assistant' ? 'AI' : '用户' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="content" label="内容" show-overflow-tooltip>
            <template #default="scope">
              {{ scope.row.content?.substring(0, 80) }}...
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="时间" width="180" />
          <el-table-column label="操作" width="150">
            <template #default="scope">
              <el-button size="small" type="primary" @click="viewAiChatDetail(scope.row)">查看详情</el-button>
              <el-button size="small" type="danger" @click="deleteAiChatMessage(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination" v-if="!selectedSession && aiChatTotal > 0">
          <el-pagination
            v-model:current-page="aiChatPage"
            :page-size="20"
            :total="aiChatTotal"
            @current-change="loadAiChatMessages"
            layout="prev, pager, next, total"
          />
        </div>
      </div>

      <div v-if="currentTab === 'friendChat'" class="tab-content">
        <div class="tab-header">
          <h3>好友聊天记录</h3>
          <div class="header-actions">
            <el-button @click="loadFriendChatMessages">刷新列表</el-button>
          </div>
        </div>

        <div class="filter-section">
          <el-select v-model="friendChatFilterSender" placeholder="选择发送者" clearable style="width: 200px; margin-right: 10px;">
            <el-option v-for="user in users" :key="user.id" :label="user.nickname || user.username" :value="user.id" />
          </el-select>
          <el-select v-model="friendChatFilterReceiver" placeholder="选择接收者" clearable style="width: 200px; margin-right: 10px;">
            <el-option v-for="user in users" :key="user.id" :label="user.nickname || user.username" :value="user.id" />
          </el-select>
          <el-button type="primary" @click="loadFriendChatMessages">筛选</el-button>
        </div>

        <el-table :data="friendChatMessages" style="width: 100%" v-loading="loadingFriendChat">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="senderId" label="发送者ID" width="100" />
          <el-table-column prop="receiverId" label="接收者ID" width="100" />
          <el-table-column prop="content" label="内容" show-overflow-tooltip>
            <template #default="scope">
              {{ scope.row.content?.substring(0, 80) }}...
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="时间" width="180" />
          <el-table-column label="操作" width="150">
            <template #default="scope">
              <el-button size="small" type="primary" @click="viewFriendChatDetail(scope.row)">查看详情</el-button>
              <el-button size="small" type="danger" @click="deleteFriendChatMessage(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination" v-if="friendChatTotal > 0">
          <el-pagination
            v-model:current-page="friendChatPage"
            :page-size="20"
            :total="friendChatTotal"
            @current-change="loadFriendChatMessages"
            layout="prev, pager, next, total"
          />
        </div>
      </div>

      <div v-if="currentTab === 'stats'" class="tab-content stats-tab">
        <div class="stats-header">
          <div class="header-left">
            <h3>数据统计</h3>
            <p class="subtitle">实时监控系统运行状态</p>
          </div>
          <div class="header-right">
            <el-button type="primary" @click="loadStats" :loading="loadingStats">
              <el-icon><refresh-cw /></el-icon> 刷新数据
            </el-button>
            <div class="live-indicator">
              <span class="live-dot"></span>
              <span>实时更新</span>
            </div>
          </div>
        </div>

        <div class="stats-grid">
          <div class="stat-card primary-card" v-animate-stat>
            <div class="card-glow"></div>
            <div class="stat-icon-wrapper primary">
              <el-icon class="stat-icon"><users /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.userCount }}</div>
              <div class="stat-label">注册用户</div>
              <div class="stat-trend positive">
                <el-icon><trending-up /></el-icon>
                <span>今日新增 +{{ dailyNewUsers }}</span>
              </div>
            </div>
          </div>

          <div class="stat-card success-card">
            <div class="card-glow"></div>
            <div class="stat-icon-wrapper success">
              <el-icon class="stat-icon"><bot /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.characterCount }}</div>
              <div class="stat-label">AI角色</div>
              <div class="stat-trend positive">
                <el-icon><trending-up /></el-icon>
                <span>系统 {{ stats.systemCharacterCount }} / 用户 {{ stats.userCharacterCount }}</span>
              </div>
            </div>
          </div>

          <div class="stat-card warning-card">
            <div class="card-glow"></div>
            <div class="stat-icon-wrapper warning">
              <el-icon class="stat-icon"><message-circle /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.messageCount }}</div>
              <div class="stat-label">聊天消息</div>
              <div class="stat-trend positive">
                <el-icon><trending-up /></el-icon>
                <span>AI {{ stats.aiChatCount }} / 好友 {{ stats.friendChatCount }}</span>
              </div>
            </div>
          </div>

          <div class="stat-card info-card">
            <div class="card-glow"></div>
            <div class="stat-icon-wrapper info">
              <el-icon class="stat-icon"><user-check /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.friendshipCount }}</div>
              <div class="stat-label">好友关系</div>
              <div class="stat-trend positive">
                <el-icon><trending-up /></el-icon>
                <span>环比 +12%</span>
              </div>
            </div>
          </div>
        </div>

        <div class="real-time-grid">
          <div class="real-time-card">
            <div class="real-time-value">{{ activeUsers }}</div>
            <div class="real-time-label">在线用户</div>
          </div>
          <div class="real-time-card">
            <div class="real-time-value">{{ processingRequests }}</div>
            <div class="real-time-label">处理中请求</div>
          </div>
          <div class="real-time-card">
            <div class="real-time-value">{{ avgResponseTime }}ms</div>
            <div class="real-time-label">平均响应时间</div>
          </div>
          <div class="real-time-card">
            <div class="real-time-value">{{ serverLoad }}%</div>
            <div class="real-time-label">服务器负载</div>
          </div>
        </div>

        <div class="charts-row">
          <div class="chart-card large-chart">
            <div class="chart-header">
              <h4>消息趋势分析</h4>
              <div class="chart-tabs">
                <button :class="['tab-btn', { active: chartPeriod === '7d' }]" @click="chartPeriod = '7d'; updateCharts()">7天</button>
                <button :class="['tab-btn', { active: chartPeriod === '30d' }]" @click="chartPeriod = '30d'; updateCharts()">30天</button>
              </div>
            </div>
            <div ref="trendChartRef" class="chart-wrapper"></div>
          </div>
        </div>

        <div class="charts-row">
          <div class="chart-card">
            <div class="chart-header">
              <h4>消息类型分布</h4>
            </div>
            <div ref="messageChartRef" class="small-chart-wrapper"></div>
            <div class="chart-legend">
              <div class="legend-item">
                <span class="legend-color" style="background: #6366f1;"></span>
                <span>AI聊天</span>
                <span class="legend-value">{{ stats.aiChatCount }}</span>
              </div>
              <div class="legend-item">
                <span class="legend-color" style="background: #22c55e;"></span>
                <span>好友聊天</span>
                <span class="legend-value">{{ stats.friendChatCount }}</span>
              </div>
            </div>
          </div>

          <div class="chart-card">
            <div class="chart-header">
              <h4>AI角色分布</h4>
            </div>
            <div ref="characterChartRef" class="small-chart-wrapper"></div>
            <div class="chart-legend">
              <div class="legend-item">
                <span class="legend-color" style="background: #8b5cf6;"></span>
                <span>系统角色</span>
                <span class="legend-value">{{ stats.systemCharacterCount }}</span>
              </div>
              <div class="legend-item">
                <span class="legend-color" style="background: #f59e0b;"></span>
                <span>用户创建</span>
                <span class="legend-value">{{ stats.userCharacterCount }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="today-stats-grid">
          <div class="today-stat-card">
            <div class="today-stat-header">
              <div class="today-stat-icon users">
                <el-icon><user-plus /></el-icon>
              </div>
            </div>
            <div class="today-stat-value">{{ todayRegistrations }}</div>
            <div class="today-stat-label">今日新注册</div>
          </div>
          <div class="today-stat-card">
            <div class="today-stat-header">
              <div class="today-stat-icon messages">
                <el-icon><message-square /></el-icon>
              </div>
            </div>
            <div class="today-stat-value">{{ todayMessages }}</div>
            <div class="today-stat-label">今日消息</div>
          </div>
          <div class="today-stat-card">
            <div class="today-stat-header">
              <div class="today-stat-icon friends">
                <el-icon><user-follow /></el-icon>
              </div>
            </div>
            <div class="today-stat-value">{{ todayFriendRequests }}</div>
            <div class="today-stat-label">今日好友请求</div>
          </div>
          <div class="today-stat-card">
            <div class="today-stat-header">
              <div class="today-stat-icon characters">
                <el-icon><bot /></el-icon>
              </div>
            </div>
            <div class="today-stat-value">{{ todayNewCharacters }}</div>
            <div class="today-stat-label">今日新角色</div>
          </div>
        </div>
      </div>

            <!-- M3 ⑩ 数据看板 -->
      <div v-if="currentTab === 'dashboard'" class="tab-content dashboard-tab">
        <div class="stats-header">
          <div class="header-left">
            <h3>数据看板</h3>
            <p class="subtitle">今日核心指标与近 7 天趋势</p>
          </div>
          <div class="header-right">
            <el-button type="primary" @click="loadDashboard" :loading="loadingDashboard">
              刷新数据
            </el-button>
          </div>
        </div>

        <div class="dashboard-cards">
          <div class="dash-card">
            <div class="dash-label">今日活跃用户</div>
            <div class="dash-value">{{ dashboard.todayActiveUsers }}</div>
          </div>
          <div class="dash-card">
            <div class="dash-label">今日新增用户</div>
            <div class="dash-value">{{ dashboard.todayNewUsers }}</div>
          </div>
          <div class="dash-card">
            <div class="dash-label">今日对话次数</div>
            <div class="dash-value">{{ dashboard.todayChats }}</div>
          </div>
        </div>

        <div class="dashboard-charts">
          <div class="dash-chart-card">
            <div class="chart-header"><h4>近 7 天活跃用户趋势</h4></div>
            <div ref="activeTrendRef" class="dash-chart"></div>
          </div>
          <div class="dash-chart-card">
            <div class="chart-header"><h4>近 7 天新增用户趋势</h4></div>
            <div ref="newUserTrendRef" class="dash-chart"></div>
          </div>
          <div class="dash-chart-card">
            <div class="chart-header"><h4>近 7 天对话次数趋势</h4></div>
            <div ref="chatTrendRef" class="dash-chart"></div>
          </div>
        </div>
      </div>

      <!-- 数据池管理 -->
      <!-- 数据池管理 -->
      <div v-if="currentTab === 'datapool'" class="tab-content">
        <div class="tab-header">
          <h3>数据池管理</h3>
        </div>

        <!-- 统计卡片 -->
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon data-icon">
              <span class="icon-text">📊</span>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ dataPoolTotal }}</div>
              <div class="stat-label">数据总数</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon text-icon">
              <span class="icon-text">📝</span>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ textCount }}</div>
              <div class="stat-label">文本数据</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon struct-icon">
              <span class="icon-text">📋</span>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ structCount }}</div>
              <div class="stat-label">结构化数据</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon media-icon">
              <span class="icon-text">🖼️</span>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ mediaCount }}</div>
              <div class="stat-label">媒体数据</div>
            </div>
          </div>
        </div>

        <!-- 容量监控 -->
        <div class="capacity-monitor">
          <el-progress
            :percentage="dataPoolUsage"
            :status="dataPoolUsage > 90 ? 'exception' : dataPoolUsage > 70 ? 'warning' : ''"
            :format="formatCapacityUsage"
          />
          <div class="capacity-info">
            <span>已使用：{{ dataPoolCount }} / {{ dataPoolMaxCapacity }} 条</span>
          </div>
        </div>

        <div class="filter-section">
          <el-input
            v-model="dataPoolSearch"
            placeholder="搜索数据..."
            @keyup.enter="loadDataPool"
            style="width: 300px; margin-right: 10px"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-select v-model="dataPoolFilterType" placeholder="数据类型" clearable style="width: 150px; margin-right: 10px">
            <el-option label="全部" value="" />
            <el-option label="文本" value="text" />
            <el-option label="结构化" value="structured" />
            <el-option label="图像" value="image" />
            <el-option label="音频" value="audio" />
            <el-option label="视频" value="video" />
          </el-select>
          <el-button type="primary" @click="loadDataPool">搜索</el-button>
        </div>

        <!-- 批量操作栏 -->
        <div class="batch-actions" v-if="selectedDataIds.length > 0">
          <span>已选择 {{ selectedDataIds.length }} 条数据</span>
          <el-button type="danger" size="small" @click="batchDelete">批量删除</el-button>
        </div>

        <el-table 
          :data="dataPoolList" 
          style="width: 100%" 
          v-loading="loadingDataPool"
          @selection-change="handleDataSelectionChange"
        >
          <el-table-column type="selection" width="55" />
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
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="scope">
              <el-button size="small" @click="viewDataDetail(scope.row)">查看</el-button>
              <el-button size="small" type="primary" @click="editData(scope.row)">编辑</el-button>
              <el-button size="small" type="danger" @click="deleteData(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 最底部：分页（左）+ 6 个管理按钮（右） -->
        <div class="datapool-footer">
          <div class="pagination datapool-pagination" v-if="dataPoolTotal > 0">
            <el-pagination
              v-model:current-page="dataPoolPage"
              :page-size="20"
              :total="dataPoolTotal"
              @current-change="loadDataPool"
              layout="prev, pager, next, total"
            />
          </div>
          <div class="footer-actions">
            <el-button type="primary" @click="showAddDataDialog">添加数据</el-button>
            <el-button @click="showDataPoolConfig">配置管理</el-button>
            <el-button @click="showImportDialog">导入数据</el-button>
            <el-button @click="exportDataPool">导出数据</el-button>
            <el-button @click="showSystemLogs">系统日志</el-button>
            <el-button @click="loadDataPool">刷新列表</el-button>
          </div>
        </div>
      </div>

      <!-- AI系统管理 -->
      <div v-if="currentTab === 'aisystem'" class="tab-content">
        <div class="tab-header">
          <h3>AI系统管理</h3>
          <div class="header-actions">
            <el-button type="primary" @click="showRegisterSystemDialog">注册系统</el-button>
            <el-button @click="loadAiSystems">刷新列表</el-button>
          </div>
        </div>

        <!-- 使用统计卡片 -->
        <el-row :gutter="16" v-loading="loadingAiStats" class="ai-stats-row">
          <el-col :span="5">
            <el-card shadow="hover">
              <div class="stat-label">系统总数</div>
              <div class="stat-value">{{ formatThousands(aiStats.totalSystems) }}</div>
            </el-card>
          </el-col>
          <el-col :span="5">
            <el-card shadow="hover">
              <div class="stat-label">活跃系统</div>
              <div class="stat-value">{{ formatThousands(aiStats.activeSystems) }}</div>
            </el-card>
          </el-col>
          <el-col :span="5">
            <el-card shadow="hover">
              <div class="stat-label">总调用次数</div>
              <div class="stat-value">{{ formatThousands(aiStats.totalCalls) }}</div>
            </el-card>
          </el-col>
          <el-col :span="5">
            <el-card shadow="hover">
              <div class="stat-label">今日调用</div>
              <div class="stat-value">{{ formatThousands(aiStats.todayCalls) }}</div>
            </el-card>
          </el-col>
          <el-col :span="4">
            <el-card shadow="hover">
              <div class="stat-label">热门系统 TOP</div>
              <div v-if="!aiStats.topSystems || aiStats.topSystems.length === 0" class="stat-empty">暂无数据</div>
              <div v-else class="stat-top-list">
                <div v-for="(t, idx) in aiStats.topSystems.slice(0, 3)" :key="t.id || idx" class="stat-top-item">
                  <span class="stat-top-name">{{ t.systemName }}</span>
                  <span class="stat-top-calls">{{ formatThousands(t.callCount) }}</span>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- Ollama 状态与模型管理 -->
        <el-card shadow="never" class="ai-section-card">
          <template #header>
            <div class="ai-section-header">
              <span>Ollama 模型管理</span>
              <div>
                <el-button size="small" @click="loadOllamaStatus">刷新</el-button>
                <el-popconfirm
                  title="确定要重启 Ollama 服务吗？"
                  confirm-button-text="确定"
                  cancel-button-text="取消"
                  @confirm="handleRestartOllama"
                >
                  <template #reference>
                    <el-button size="small" type="warning" :loading="restartingOllama">重启 Ollama</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </div>
          </template>

          <el-descriptions :column="3" border class="ollama-status-desc">
            <el-descriptions-item label="服务状态">
              <el-tag :type="ollamaTagType">{{ ollamaTagText }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="版本">{{ ollamaStatus.version || '-' }}</el-descriptions-item>
            <el-descriptions-item label="模型数量">{{ ollamaStatus.modelCount ?? '-' }}</el-descriptions-item>
          </el-descriptions>

          <el-divider />

          <el-form inline :model="pullModelForm" class="pull-model-form">
            <el-form-item label="模型名称">
              <el-input v-model="pullModelForm.name" placeholder="如 llama3.2:3b" style="width: 280px" clearable />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="pullingModel" @click="handlePullModel">拉取模型</el-button>
            </el-form-item>
          </el-form>

          <el-table :data="ollamaModels" v-loading="loadingOllamaModels" style="width: 100%">
            <template #empty>
              <el-empty description="暂无模型" />
            </template>
            <el-table-column prop="name" label="模型名称" min-width="220" />
            <el-table-column label="大小" width="140">
              <template #default="scope">{{ formatModelSize(scope.row.size) }}</template>
            </el-table-column>
            <el-table-column label="修改时间" width="180">
              <template #default="scope">{{ formatDate(scope.row.modifiedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="scope">
                <el-popconfirm
                  :title="`确定删除模型 ${scope.row.name} 吗？`"
                  confirm-button-text="删除"
                  cancel-button-text="取消"
                  @confirm="handleDeleteOllamaModel(scope.row.name)"
                >
                  <template #reference>
                    <el-button size="small" type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- AI 全局配置 -->
        <el-card shadow="never" class="ai-section-card">
          <template #header>
            <div class="ai-section-header">
              <span>AI 全局配置</span>
              <el-button size="small" @click="loadAiConfig">加载配置</el-button>
            </div>
          </template>
          <el-form :model="aiConfigForm" label-width="140px" style="max-width: 720px" v-loading="loadingAiConfig">
            <el-form-item label="默认模型">
              <el-input v-model="aiConfigForm.defaultModel" placeholder="如 llama3.2:3b" />
            </el-form-item>
            <el-form-item label="Temperature">
              <el-slider v-model="aiConfigForm.temperature" :min="0" :max="2" :step="0.1" show-input style="max-width: 480px" />
            </el-form-item>
            <el-form-item label="Max Tokens">
              <el-input-number v-model="aiConfigForm.maxTokens" :min="100" :max="8192" :step="128" />
            </el-form-item>
            <el-form-item label="请求超时(秒)">
              <el-input-number v-model="aiConfigForm.requestTimeoutSec" :min="10" :max="300" :step="10" />
            </el-form-item>
            <el-form-item label="启用 AI 聊天">
              <el-switch v-model="aiConfigForm.aiChatEnabled" />
            </el-form-item>
            <el-form-item label="维护公告">
              <el-input v-model="aiConfigForm.maintenanceNotice" type="textarea" :rows="3" placeholder="留空表示无公告" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingAiConfig" @click="handleSaveAiConfig">保存配置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 系统列表 -->
        <el-card shadow="never" class="ai-section-card">
          <template #header><span>系统列表</span></template>
          <el-table :data="aiSystemList" style="width: 100%" v-loading="loadingAiSystem">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="systemName" label="系统名称" min-width="140" />
            <el-table-column prop="systemType" label="类型" width="110" />
            <el-table-column label="API Key" min-width="180">
              <template #default="scope">
                <el-text truncated>{{ maskApiKey(scope.row.apiKey) }}</el-text>
              </template>
            </el-table-column>
            <el-table-column prop="rateLimit" label="速率限制" width="100">
              <template #default="scope">
                <span>{{ scope.row.rateLimit }} 次/分</span>
              </template>
            </el-table-column>
            <el-table-column label="调用次数" width="100">
              <template #default="scope">{{ formatThousands(scope.row.callCount) }}</template>
            </el-table-column>
            <el-table-column label="最近访问" width="170">
              <template #default="scope">{{ formatDate(scope.row.lastAccessAt) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="scope">
                <el-switch
                  :model-value="scope.row.status === 1"
                  active-text="启用"
                  inactive-text="禁用"
                  @change="(val) => handleToggleSystemStatus(scope.row, val)"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="320" fixed="right">
              <template #default="scope">
                <el-button size="small" @click="showSystemDetail(scope.row)">详情</el-button>
                <el-button size="small" @click="showRatelimitDialog(scope.row)">限流</el-button>
                <el-button size="small" @click="showPermissionDialog(scope.row)">权限</el-button>
                <el-button size="small" type="warning" @click="regenerateSystemKey(scope.row)">重置密钥</el-button>
                <el-button size="small" type="danger" @click="deleteAiSystem(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>

      <el-dialog v-model="userDialogVisible" :title="isEditingUser ? '编辑用户' : '添加用户'" width="500px">
        <el-form :model="userForm" label-width="80px">
          <el-form-item label="用户名">
            <el-input v-model="userForm.username" :disabled="isEditingUser" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="userForm.nickname" placeholder="请输入昵称" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="userForm.password" type="password" :placeholder="isEditingUser ? '留空则不修改密码' : '请输入密码'" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="userDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveUser">保存</el-button>
        </template>
      </el-dialog>

    <el-dialog v-model="characterDialogVisible" :title="isEditingCharacter ? '编辑角色' : '添加角色'" width="600px">
      <el-form :model="characterForm" label-width="80px">
        <el-form-item label="角色名称">
          <el-input v-model="characterForm.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="头像URL">
          <el-input v-model="characterForm.avatar" placeholder="请输入头像URL，留空使用默认头像" />
        </el-form-item>
        <el-form-item label="角色描述">
          <el-input v-model="characterForm.prompt" type="textarea" :rows="5" placeholder="请输入角色描述" />
        </el-form-item>
        <el-form-item label="系统角色">
          <el-switch v-model="characterForm.isSystem" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="characterDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCharacter">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="aiChatDetailVisible" title="AI聊天详情" width="700px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="记录ID">{{ aiChatDetail.id }}</el-descriptions-item>
        <el-descriptions-item label="会话ID">{{ aiChatDetail.sessionId }}</el-descriptions-item>
        <el-descriptions-item label="发送者">{{ aiChatDetail.senderType }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ aiChatDetail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="内容">
          <div style="white-space: pre-wrap; word-break: break-all;">{{ aiChatDetail.content }}</div>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="aiChatDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="friendChatDetailVisible" title="好友聊天详情" width="700px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="记录ID">{{ friendChatDetail.id }}</el-descriptions-item>
        <el-descriptions-item label="发送者ID">{{ friendChatDetail.senderId }}</el-descriptions-item>
        <el-descriptions-item label="接收者ID">{{ friendChatDetail.receiverId }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ friendChatDetail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="内容">
          <div style="white-space: pre-wrap; word-break: break-all;">{{ friendChatDetail.content }}</div>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="friendChatDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

      <el-dialog v-model="dataPoolDetailVisible" title="数据详情" width="700px">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="ID">{{ dataPoolDetail.id }}</el-descriptions-item>
          <el-descriptions-item label="标题">{{ dataPoolDetail.title }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ dataPoolDetail.dataType }}</el-descriptions-item>
          <el-descriptions-item label="格式">{{ dataPoolDetail.format }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ dataPoolDetail.category }}</el-descriptions-item>
          <el-descriptions-item label="版本">v{{ dataPoolDetail.version }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDate(dataPoolDetail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="内容">
            <div style="white-space: pre-wrap; word-break: break-all; max-height: 300px; overflow-y: auto;">
              {{ dataPoolDetail.content }}
            </div>
          </el-descriptions-item>
        </el-descriptions>
        <template #footer>
          <el-button @click="dataPoolDetailVisible = false">关闭</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="registerSystemDialogVisible" title="注册 AI 系统" width="500px">
        <el-form :model="registerSystemForm" label-width="100px">
          <el-form-item label="系统名称" required>
            <el-input v-model="registerSystemForm.systemName" placeholder="请输入系统名称" />
          </el-form-item>
          <el-form-item label="系统类型" required>
            <el-select v-model="registerSystemForm.systemType" style="width: 100%">
              <el-option label="聊天机器人" value="chatbot" />
              <el-option label="助手" value="assistant" />
              <el-option label="数据分析" value="analytics" />
              <el-option label="内容生成" value="content" />
              <el-option label="其他" value="other" />
            </el-select>
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="registerSystemForm.description" type="textarea" :rows="3" placeholder="请输入系统描述" />
          </el-form-item>
          <el-form-item label="速率限制">
            <el-input-number v-model="registerSystemForm.rateLimit" :min="1" :max="10000" />
            <span style="margin-left: 10px">次/分钟</span>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="registerSystemDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitRegisterSystem">注册</el-button>
        </template>
      </el-dialog>

      <!-- 速率限制编辑 -->
      <el-dialog v-model="ratelimitDialogVisible" title="编辑速率限制" width="460px">
        <el-form :model="ratelimitForm" label-width="100px">
          <el-form-item label="系统名称">
            <el-input v-model="ratelimitForm.systemName" disabled />
          </el-form-item>
          <el-form-item label="速率限制">
            <el-input-number v-model="ratelimitForm.rateLimit" :min="1" :max="100000" />
            <span style="margin-left: 10px">次/分钟</span>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="ratelimitDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="savingRatelimit" @click="handleSaveRatelimit">保存</el-button>
        </template>
      </el-dialog>

      <!-- 系统权限 -->
      <el-dialog v-model="permissionDialogVisible" title="系统权限" width="640px">
        <div style="margin-bottom: 10px; color: #666;">
          系统：{{ permissionTarget.systemName || '-' }}
        </div>
        <el-table :data="systemPermissions" v-loading="loadingPermissions" size="small" style="width: 100%">
          <template #empty>
            <el-empty description="暂无权限" :image-size="60" />
          </template>
          <el-table-column prop="permissionName" label="权限名称" min-width="160" />
          <el-table-column prop="category" label="分类" width="140" />
          <el-table-column label="授予时间" width="170">
            <template #default="scope">{{ formatDate(scope.row.grantedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="90">
            <template #default="scope">
              <el-popconfirm
                title="确定撤销该权限？"
                confirm-button-text="撤销"
                cancel-button-text="取消"
                @confirm="handleRevokePermission(scope.row)"
              >
                <template #reference>
                  <el-button size="small" type="danger" link>撤销</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <template #footer>
          <el-button @click="permissionDialogVisible = false">关闭</el-button>
        </template>
      </el-dialog>

      <!-- 系统详情 -->
      <el-dialog v-model="systemDetailVisible" title="系统详情" width="640px">
        <el-descriptions :column="1" border v-if="systemDetail.id">
          <el-descriptions-item label="ID">{{ systemDetail.id }}</el-descriptions-item>
          <el-descriptions-item label="系统名称">{{ systemDetail.systemName }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ systemDetail.systemType }}</el-descriptions-item>
          <el-descriptions-item label="描述">{{ systemDetail.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="API Key">
            <span>{{ maskApiKey(systemDetail.apiKey) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="速率限制">{{ systemDetail.rateLimit }} 次/分</el-descriptions-item>
          <el-descriptions-item label="调用次数">{{ formatThousands(systemDetail.callCount) }}</el-descriptions-item>
          <el-descriptions-item label="最近访问">{{ formatDate(systemDetail.lastAccessAt) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="systemDetail.status === 1 ? 'success' : 'danger'">
              {{ systemDetail.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDate(systemDetail.createdAt) }}</el-descriptions-item>
        </el-descriptions>
        <template #footer>
          <el-button @click="systemDetailVisible = false">关闭</el-button>
        </template>
      </el-dialog>

      <!-- 添加数据对话框 -->
      <el-dialog v-model="addDataDialogVisible" title="添加数据" width="700px">
        <el-form :model="addDataForm" label-width="100px">
          <el-form-item label="标题" required>
            <el-input v-model="addDataForm.title" placeholder="请输入数据标题" />
          </el-form-item>
          <el-form-item label="数据类型" required>
            <el-select v-model="addDataForm.dataType" style="width: 100%">
              <el-option label="文本" value="text" />
              <el-option label="结构化" value="structured" />
              <el-option label="图像" value="image" />
              <el-option label="音频" value="audio" />
              <el-option label="视频" value="video" />
            </el-select>
          </el-form-item>
          <el-form-item label="分类" required>
            <el-input v-model="addDataForm.category" placeholder="请输入数据分类" />
          </el-form-item>
          <el-form-item label="格式" required>
            <el-input v-model="addDataForm.format" placeholder="请输入数据格式，如：json, txt, png" />
          </el-form-item>
          <el-form-item label="版本">
            <el-input-number v-model="addDataForm.version" :min="1" :max="999" />
          </el-form-item>
          <el-form-item label="内容" required>
            <el-input
              v-model="addDataForm.content"
              type="textarea"
              :rows="8"
              placeholder="请输入数据内容"
            />
          </el-form-item>
          <el-form-item label="标签">
            <el-input v-model="addDataForm.tags" placeholder="请输入标签，多个标签用逗号分隔" />
          </el-form-item>
          <el-form-item label="描述">
            <el-input
              v-model="addDataForm.description"
              type="textarea"
              :rows="3"
              placeholder="请输入数据描述"
            />
          </el-form-item>
          <el-form-item label="公开访问">
            <el-switch v-model="addDataForm.isPublic" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="addDataDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitAddData">保存</el-button>
        </template>
      </el-dialog>

      <!-- 编辑数据对话框 -->
      <el-dialog v-model="editDataDialogVisible" title="编辑数据" width="700px">
        <el-form :model="editDataForm" label-width="100px">
          <el-form-item label="ID">
            <el-input v-model="editDataForm.id" disabled />
          </el-form-item>
          <el-form-item label="标题" required>
            <el-input v-model="editDataForm.title" placeholder="请输入数据标题" />
          </el-form-item>
          <el-form-item label="数据类型" required>
            <el-select v-model="editDataForm.dataType" style="width: 100%">
              <el-option label="文本" value="text" />
              <el-option label="结构化" value="structured" />
              <el-option label="图像" value="image" />
              <el-option label="音频" value="audio" />
              <el-option label="视频" value="video" />
            </el-select>
          </el-form-item>
          <el-form-item label="分类" required>
            <el-input v-model="editDataForm.category" placeholder="请输入数据分类" />
          </el-form-item>
          <el-form-item label="格式" required>
            <el-input v-model="editDataForm.format" placeholder="请输入数据格式，如：json, txt, png" />
          </el-form-item>
          <el-form-item label="版本">
            <el-input-number v-model="editDataForm.version" :min="1" :max="999" />
          </el-form-item>
          <el-form-item label="内容" required>
            <el-input
              v-model="editDataForm.content"
              type="textarea"
              :rows="8"
              placeholder="请输入数据内容"
            />
          </el-form-item>
          <el-form-item label="标签">
            <el-input v-model="editDataForm.tags" placeholder="请输入标签，多个标签用逗号分隔" />
          </el-form-item>
          <el-form-item label="描述">
            <el-input
              v-model="editDataForm.description"
              type="textarea"
              :rows="3"
              placeholder="请输入数据描述"
            />
          </el-form-item>
          <el-form-item label="公开访问">
            <el-switch v-model="editDataForm.isPublic" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="editDataDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitEditData">保存</el-button>
        </template>
      </el-dialog>

      <!-- 数据池配置对话框 -->
      <el-dialog v-model="dataPoolConfigVisible" title="数据池配置" width="600px">
        <el-form :model="dataPoolConfig" label-width="120px">
          <el-form-item label="最大容量">
            <el-input-number v-model="dataPoolConfig.maxCapacity" :min="100" :max="1000000" />
            <span style="margin-left: 10px">条</span>
          </el-form-item>
          <el-form-item label="自动清理">
            <el-switch v-model="dataPoolConfig.autoCleanup" />
          </el-form-item>
          <el-form-item label="清理阈值" v-if="dataPoolConfig.autoCleanup">
            <el-input-number v-model="dataPoolConfig.cleanupThreshold" :min="50" :max="95" />
            <span style="margin-left: 10px">%</span>
          </el-form-item>
          <el-form-item label="访问权限">
            <el-select v-model="dataPoolConfig.accessLevel" style="width: 100%">
              <el-option label="公开" value="public" />
              <el-option label="仅管理员" value="admin" />
              <el-option label="指定用户" value="specified" />
            </el-select>
          </el-form-item>
          <el-form-item label="允许的用户" v-if="dataPoolConfig.accessLevel === 'specified'">
            <el-select
              v-model="dataPoolConfig.allowedUsers"
              multiple
              filterable
              allow-create
              default-first-option
              style="width: 100%"
              placeholder="请输入用户名"
            >
            </el-select>
          </el-form-item>
          <el-form-item label="备份策略">
            <el-select v-model="dataPoolConfig.backupStrategy" style="width: 100%">
              <el-option label="不备份" value="none" />
              <el-option label="每天备份" value="daily" />
              <el-option label="每周备份" value="weekly" />
              <el-option label="每月备份" value="monthly" />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dataPoolConfigVisible = false">取消</el-button>
          <el-button type="primary" @click="submitDataPoolConfig">保存配置</el-button>
        </template>
      </el-dialog>

      <!-- 导入数据对话框 -->
      <el-dialog v-model="importDialogVisible" title="导入数据" width="700px">
        <el-form :model="importForm" label-width="100px">
          <el-form-item label="导入方式">
            <el-select v-model="importForm.importType" style="width: 100%" @change="resetUploadFiles">
              <el-option label="批量上传文件" value="file" />
              <el-option label="从URL导入" value="url" />
            </el-select>
          </el-form-item>

          <el-form-item label="数据文件" v-if="importForm.importType === 'file'">
            <div 
              class="drop-zone"
              :class="{ 'drop-zone-active': isDragging }"
              @dragover.prevent="isDragging = true"
              @dragleave="isDragging = false"
              @drop.prevent="handleDrop"
            >
              <div class="drop-zone-content">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                  <polyline points="17 8 12 3 7 8"/>
                  <line x1="12" y1="3" x2="12" y2="15"/>
                </svg>
                <p>拖拽文件到此处或点击上传</p>
                <p class="drop-zone-hint">支持 TXT、PDF、DOCX、XLSX、PPTX、JSON、CSV、图片、音频、视频等格式，单文件不超过 50MB</p>
              </div>
              <input
                type="file"
                multiple
                class="drop-zone-input"
                @change="handleFileSelect"
                accept=".txt,.md,.pdf,.docx,.xlsx,.pptx,.json,.csv,.jpg,.jpeg,.png,.gif,.webp,.mp3,.wav,.mp4,.mov"
              />
            </div>

            <div v-if="uploadFiles.length > 0" class="upload-file-list">
              <div v-for="(file, index) in uploadFiles" :key="index" class="upload-file-item">
                <div class="file-icon" :class="getFileIconClass(file.type)">
                  <svg v-if="file.type.startsWith('image/')" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                    <circle cx="8.5" cy="8.5" r="1.5"/>
                    <polyline points="21 15 16 10 5 21"/>
                  </svg>
                  <svg v-else-if="file.type.startsWith('audio/')" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/>
                    <path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"/>
                  </svg>
                  <svg v-else-if="file.type.startsWith('video/')" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="2" y="2" width="20" height="16" rx="2" ry="2"/>
                    <line x1="8" y1="22" x2="16" y2="22"/>
                    <line x1="12" y1="18" x2="12" y2="22"/>
                  </svg>
                  <svg v-else width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                    <polyline points="14 2 14 8 20 8"/>
                    <line x1="16" y1="13" x2="8" y2="13"/>
                    <line x1="16" y1="17" x2="8" y2="17"/>
                    <polyline points="10 9 9 9 8 9"/>
                  </svg>
                </div>
                <div class="file-info">
                  <div class="file-name">{{ file.name }}</div>
                  <div class="file-meta">{{ formatFileSize(file.size) }} · {{ getFileFormat(file.name) }}</div>
                </div>
                <div class="file-preview" v-if="file.type.startsWith('image/')">
                  <img :src="URL.createObjectURL(file)" alt="预览" />
                </div>
                <button class="file-remove" @click="removeUploadFile(index)">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="18" y1="6" x2="6" y2="18"/>
                    <line x1="6" y1="6" x2="18" y2="18"/>
                  </svg>
                </button>
              </div>
            </div>
          </el-form-item>

          <el-form-item label="数据源URL" v-if="importForm.importType === 'url'">
            <el-input v-model="importForm.sourceUrl" placeholder="请输入数据源URL" />
          </el-form-item>

          <el-form-item label="数据类型">
            <el-select v-model="importForm.dataType" style="width: 100%">
              <el-option label="文本" value="text" />
              <el-option label="结构化" value="structured" />
              <el-option label="图像" value="image" />
              <el-option label="音频" value="audio" />
              <el-option label="视频" value="video" />
            </el-select>
          </el-form-item>

          <el-form-item label="覆盖现有数据">
            <el-switch v-model="importForm.overwrite" />
          </el-form-item>

          <div v-if="uploadProgress > 0 && uploadProgress < 100" class="upload-progress-container">
            <el-progress :percentage="uploadProgress" :stroke-width="10" />
            <div class="upload-progress-text">上传中... {{ uploadProgress }}%</div>
          </div>
        </el-form>
        <template #footer>
          <el-button @click="importDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitImport" :loading="uploading">
            {{ uploading ? '上传中...' : '开始导入' }}
          </el-button>
        </template>
      </el-dialog>

      <!-- 批量删除确认对话框 -->
      <el-dialog v-model="batchDeleteVisible" title="批量删除确认" width="400px">
        <p>确定要删除选中的 <span style="color: #f56c6c; font-weight: bold">{{ selectedDataIds.length }}</span> 条数据吗？</p>
        <p class="text-muted-small">此操作不可撤销，请谨慎操作。</p>
        <template #footer>
          <el-button @click="batchDeleteVisible = false">取消</el-button>
          <el-button type="danger" @click="confirmBatchDelete">确定删除</el-button>
        </template>
      </el-dialog>

      <!-- 系统日志对话框 -->
      <el-dialog v-model="systemLogsVisible" title="系统日志" width="900px" height="600px">
        <div class="logs-container">
          <div class="logs-filter">
            <el-select v-model="logFilterLevel" placeholder="日志级别" clearable style="width: 150px; margin-right: 10px">
              <el-option label="全部" value="" />
              <el-option label="信息" value="info" />
              <el-option label="成功" value="success" />
              <el-option label="警告" value="warning" />
              <el-option label="错误" value="error" />
            </el-select>
            <el-select v-model="logFilterModule" placeholder="模块" clearable style="width: 120px; margin-right: 10px">
              <el-option label="全部" value="" />
              <el-option label="数据池" value="datapool" />
              <el-option label="权限" value="auth" />
            </el-select>
            <el-input v-model="logFilterKeyword" placeholder="搜索日志..." style="width: 200px" />
            <el-button type="primary" @click="filterLogs">筛选</el-button>
            <el-button @click="clearLogFilter">清除筛选</el-button>
          </div>
          <div class="logs-list">
            <div 
              v-for="log in filteredLogs" 
              :key="log.id" 
              class="log-item"
              :class="`log-${log.level}`"
            >
              <div class="log-timestamp">{{ formatLogTime(log.timestamp) }}</div>
              <div class="log-level" :class="`level-${log.level}`">
                {{ log.level === 'success' ? '成功' : log.level === 'warning' ? '警告' : log.level === 'error' ? '错误' : '信息' }}
              </div>
              <div class="log-module">{{ log.module === 'datapool' ? '数据池' : '权限' }}</div>
              <div class="log-message">{{ log.message }}</div>
              <div class="log-user">{{ log.user }}</div>
            </div>
            <div v-if="filteredLogs.length === 0" class="empty-logs">暂无日志记录</div>
          </div>
        </div>
      </el-dialog>
    </div>
  </div>

  <!-- 公告管理 -->
      <div v-if="currentTab === 'announcements'" class="tab-content">
        <div class="tab-header">
          <h3>公告管理</h3>
          <div class="header-actions">
            <el-button type="primary" @click="showAddAnnouncementDialog">发布公告</el-button>
            <el-button @click="loadAnnouncements">刷新列表</el-button>
          </div>
        </div>

        <div class="filter-section">
          <el-select v-model="announcementFilterStatus" placeholder="状态筛选" clearable style="width: 150px; margin-right: 10px;">
            <el-option label="全部" value="" />
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
          </el-select>
          <el-input v-model="announcementSearch" placeholder="搜索标题..." style="width: 250px" @keyup.enter="loadAnnouncements" />
          <el-button type="primary" @click="loadAnnouncements">搜索</el-button>
        </div>

        <el-table :data="announcements" style="width: 100%" v-loading="loadingAnnouncements">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
          <el-table-column prop="author" label="作者" width="120" />
          <el-table-column prop="priority" label="优先级" width="100">
            <template #default="scope">
              <el-tag :type="getPriorityType(scope.row.priority)">
                {{ getPriorityText(scope.row.priority) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.status === 'PUBLISHED' ? 'success' : 'info'">
                {{ scope.row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="displayScope" label="显示范围" width="120">
            <template #default="scope">
              {{ getDisplayScopeText(scope.row.displayScope) }}
            </template>
          </el-table-column>
          <el-table-column prop="publishTime" label="发布时间" width="180">
            <template #default="scope">
              {{ formatDate(scope.row.publishTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="viewCount" label="浏览次数" width="100" />
          <el-table-column label="操作" width="280">
            <template #default="scope">
              <el-button size="small" type="primary" @click="editAnnouncement(scope.row)">编辑</el-button>
              <el-button size="small" v-if="scope.row.status === 'DRAFT'" type="success" @click="publishAnnouncement(scope.row.id)">发布</el-button>
              <el-button size="small" v-if="scope.row.status === 'PUBLISHED'" type="warning" @click="unpublishAnnouncement(scope.row.id)">撤回</el-button>
              <el-button size="small" type="danger" @click="deleteAnnouncement(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination" v-if="announcementTotal > 0">
          <el-pagination
            v-model:current-page="announcementPage"
            :page-size="20"
            :total="announcementTotal"
            @current-change="loadAnnouncements"
            layout="prev, pager, next, total"
          />
        </div>
      </div>

      <!-- 个人中心 -->
      <div v-if="currentTab === 'profile'" class="tab-content">
        <div class="tab-header">
          <h3>个人中心</h3>
        </div>

        <div class="profile-section">
          <div class="profile-card">
            <div class="profile-header">
              <div class="profile-avatar">
                <span class="avatar-icon">👤</span>
              </div>
              <div class="profile-info">
                <h4>{{ adminInfo.username }}</h4>
                <p class="profile-role">管理员</p>
              </div>
            </div>
            <div class="profile-details">
              <div class="detail-row">
                <span class="detail-label">用户ID</span>
                <span class="detail-value">{{ adminInfo.id }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">昵称</span>
                <span class="detail-value">{{ adminInfo.nickname }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">邮箱</span>
                <span class="detail-value">{{ adminInfo.email || '未设置' }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">注册时间</span>
                <span class="detail-value">{{ formatDate(adminInfo.createTime) }}</span>
              </div>
            </div>
          </div>

          <div class="profile-card">
            <div class="card-title">
              <el-icon><User /></el-icon>
              <span>修改个人信息</span>
            </div>
            <el-form :model="profileForm" label-width="120px" class="password-form">
              <el-form-item label="昵称" required>
                <el-input 
                  v-model="profileForm.nickname" 
                  placeholder="请输入昵称"
                />
              </el-form-item>
              <el-form-item label="邮箱">
                <el-input 
                  v-model="profileForm.email" 
                  type="email"
                  placeholder="请输入邮箱地址"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="updateProfile" :loading="updatingProfile">确认修改</el-button>
                <el-button @click="resetProfileForm">重置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <div class="profile-card">
            <div class="card-title">
              <el-icon><Lock /></el-icon>
              <span>修改密码</span>
            </div>
            <el-form :model="passwordForm" label-width="120px" class="password-form">
              <el-form-item label="旧密码" required>
                <el-input 
                  v-model="passwordForm.oldPassword" 
                  type="password" 
                  placeholder="请输入旧密码"
                  show-password
                />
              </el-form-item>
              <el-form-item label="新密码" required>
                <el-input 
                  v-model="passwordForm.newPassword" 
                  type="password" 
                  placeholder="请输入新密码（至少6位）"
                  show-password
                />
              </el-form-item>
              <el-form-item label="确认密码" required>
                <el-input 
                  v-model="passwordForm.confirmPassword" 
                  type="password" 
                  placeholder="请再次输入新密码"
                  show-password
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="changePassword" :loading="changingPassword">确认修改</el-button>
                <el-button @click="resetPasswordForm">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </div>

      <!-- 用户数据管理 -->
      <div v-if="currentTab === 'userData'" class="tab-content">
        <div class="tab-header">
          <h3>用户数据管理</h3>
          <div class="header-actions">
            <div class="auto-save-toggle">
              <span>自动保存</span>
              <el-switch 
                v-model="autoSaveEnabled" 
                active-text="开启" 
                inactive-text="关闭"
                @change="toggleAutoSave"
                :loading="togglingAutoSave"
              />
            </div>
            <el-button @click="loadUserDataRecords">刷新列表</el-button>
            <el-button @click="showUserDataAnalysis">数据分析</el-button>
            <el-button @click="showBackupManager">备份管理</el-button>
            <el-dropdown>
              <el-button>更多操作 ▼</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="checkDuplicates">检查重复</el-dropdown-item>
                  <el-dropdown-item @click="removeDuplicates" divided>去重</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>

        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon data-icon">
              <span class="icon-text">📊</span>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ userDataStats.totalRecords || 0 }}</div>
              <div class="stat-label">数据记录总数</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon text-icon">
              <span class="icon-text">✅</span>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ userDataStats.validatedRecords || 0 }}</div>
              <div class="stat-label">已验证数据</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon struct-icon">
              <span class="icon-text">💾</span>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ userDataStats.backedUpRecords || 0 }}</div>
              <div class="stat-label">已备份数据</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon media-icon">
              <span class="icon-text">👥</span>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ userDataStats.userCount || 0 }}</div>
              <div class="stat-label">数据来源用户</div>
            </div>
          </div>
        </div>

        <div class="filter-section">
          <el-input
            v-model="userDataSearch"
            placeholder="搜索数据内容..."
            @keyup.enter="loadUserDataRecords"
            style="width: 300px; margin-right: 10px"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-select v-model="userDataFilterType" placeholder="数据类型" clearable style="width: 150px; margin-right: 10px">
            <el-option label="全部" value="" />
            <el-option label="表单数据" value="form" />
            <el-option label="聊天消息" value="chat_message" />
            <el-option label="用户操作" value="user_action" />
            <el-option label="编辑器内容" value="editor_content" />
          </el-select>
          <el-select v-model="userDataFilterStatus" placeholder="验证状态" clearable style="width: 150px; margin-right: 10px">
            <el-option label="全部" value="" />
            <el-option label="待验证" value="PENDING_VALIDATION" />
            <el-option label="已验证" value="VALIDATED" />
            <el-option label="无效" value="INVALID" />
          </el-select>
          <el-button type="primary" @click="loadUserDataRecords">搜索</el-button>
        </div>

        <div class="batch-actions" v-if="selectedUserDataIds.length > 0">
          <span>已选择 {{ selectedUserDataIds.length }} 条数据</span>
          <el-button type="danger" size="small" @click="batchDeleteUserData">批量删除</el-button>
        </div>

        <el-table 
          :data="userDataRecords" 
          style="width: 100%" 
          v-loading="loadingUserData"
          @selection-change="handleUserDataSelectionChange"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="userId" label="用户ID" width="100" />
          <el-table-column prop="dataType" label="数据类型" width="130">
            <template #default="scope">
              <el-tag :type="getDataTypeTagType(scope.row.dataType)">
                {{ getDataTypeLabel(scope.row.dataType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="content" label="内容" min-width="200">
            <template #default="scope">
              <el-text :truncate="50">{{ scope.row.content }}</el-text>
            </template>
          </el-table-column>
          <el-table-column prop="tags" label="标签" width="150">
            <template #default="scope">
              <el-tag v-for="tag in (scope.row.tags || '').split(',').slice(0, 3)" :key="tag" size="small" type="info">
                {{ tag }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="integrityScore" label="完整性" width="100">
            <template #default="scope">
              <el-progress :percentage="Math.round((scope.row.integrityScore || 0) * 100)" :stroke-width="8" />
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getStatusTagType(scope.row.status)">
                {{ getStatusLabel(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="captureTime" label="捕获时间" width="180">
            <template #default="scope">
              {{ formatDate(scope.row.captureTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="scope">
              <el-button size="small" @click="viewUserDataDetail(scope.row)">查看</el-button>
              <el-button size="small" type="danger" @click="deleteUserData(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination" v-if="userDataTotal > 0">
          <el-pagination
            v-model:current-page="userDataPage"
            :page-size="20"
            :total="userDataTotal"
            @current-change="loadUserDataRecords"
            layout="prev, pager, next, total"
          />
        </div>
      </div>

      <!-- 用户数据详情对话框 -->
      <el-dialog v-model="userDataDetailVisible" title="用户数据详情" width="800px">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ID">{{ userDataDetail.id }}</el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ userDataDetail.userId }}</el-descriptions-item>
          <el-descriptions-item label="数据类型">
            <el-tag :type="getDataTypeTagType(userDataDetail.dataType)">
              {{ getDataTypeLabel(userDataDetail.dataType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTagType(userDataDetail.status)">
              {{ getStatusLabel(userDataDetail.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="完整性">
            <el-progress :percentage="Math.round((userDataDetail.integrityScore || 0) * 100)" />
          </el-descriptions-item>
          <el-descriptions-item label="分组ID">{{ userDataDetail.groupId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="标签">
            <el-tag v-for="tag in (userDataDetail.tags || '').split(',')" :key="tag" size="small">
              {{ tag }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="捕获时间">{{ formatDate(userDataDetail.captureTime) }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top: 20px;">
          <h4 style="margin-bottom: 10px;">数据内容</h4>
          <div style="white-space: pre-wrap; word-break: break-all; max-height: 300px; overflow-y: auto; padding: 12px; background: var(--bg-tertiary); border-radius: 8px;">
            {{ userDataDetail.content }}
          </div>
        </div>
        <div v-if="userDataDetail.metadata" style="margin-top: 20px;">
          <h4 style="margin-bottom: 10px;">元数据</h4>
          <div style="white-space: pre-wrap; word-break: break-all; max-height: 200px; overflow-y: auto; padding: 12px; background: var(--bg-tertiary); border-radius: 8px;">
            {{ userDataDetail.metadata }}
          </div>
        </div>
        <template #footer>
          <el-button @click="userDataDetailVisible = false">关闭</el-button>
        </template>
      </el-dialog>

      <!-- 数据分析对话框 -->
      <el-dialog v-model="userDataAnalysisVisible" title="数据分析" width="800px">
        <div v-loading="loadingAnalysis">
          <div class="stats-grid" style="margin-bottom: 24px;">
            <div class="stat-card">
              <div class="stat-value">{{ analysisData.totalRecords || 0 }}</div>
              <div class="stat-label">记录总数</div>
            </div>
            <div class="stat-card">
              <div class="stat-value">{{ analysisData.avgContentLength || 0 }}</div>
              <div class="stat-label">平均内容长度</div>
            </div>
            <div class="stat-card">
              <div class="stat-value">{{ analysisData.avgIntegrityScore ? (analysisData.avgIntegrityScore * 100).toFixed(1) + '%' : '0%' }}</div>
              <div class="stat-label">平均完整性</div>
            </div>
          </div>

          <div style="margin-bottom: 20px;">
            <h4>数据类型分布</h4>
            <el-table :data="typeDistribution" style="width: 100%; margin-top: 10px;">
              <el-table-column prop="type" label="类型" />
              <el-table-column prop="count" label="数量" />
              <el-table-column prop="percentage" label="占比">
                <template #default="scope">
                  <el-progress :percentage="scope.row.percentage" :stroke-width="8" />
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div>
            <h4>标签分布（Top 10）</h4>
            <div style="display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px;">
              <el-tag 
                v-for="(count, tag) in tagDistribution" 
                :key="tag" 
                :size="getTagSize(count)"
              >
                {{ tag }} ({{ count }})
              </el-tag>
            </div>
          </div>
        </div>
        <template #footer>
          <el-button @click="userDataAnalysisVisible = false">关闭</el-button>
        </template>
      </el-dialog>

      <!-- 备份管理对话框 -->
      <el-dialog v-model="backupManagerVisible" title="数据备份管理" width="800px">
        <div v-loading="loadingBackup">
          <div class="stats-grid" style="margin-bottom: 24px;">
            <div class="stat-card">
              <div class="stat-value">{{ backupStats.totalBackups || 0 }}</div>
              <div class="stat-label">备份总数</div>
            </div>
            <div class="stat-card">
              <div class="stat-value">{{ backupStats.fullBackups || 0 }}</div>
              <div class="stat-label">全量备份</div>
            </div>
            <div class="stat-card">
              <div class="stat-value">{{ backupStats.incrementalBackups || 0 }}</div>
              <div class="stat-label">增量备份</div>
            </div>
            <div class="stat-card">
              <div class="stat-value">{{ backupStats.totalSize || '0 B' }}</div>
              <div class="stat-label">备份总大小</div>
            </div>
          </div>

          <div style="margin-bottom: 20px;">
            <el-button type="primary" @click="performFullBackup" :loading="performingBackup">执行全量备份</el-button>
            <el-button @click="performIncrementalBackup" :loading="performingBackup" style="margin-left: 10px;">执行增量备份</el-button>
          </div>

          <div>
            <h4>备份文件列表</h4>
            <el-table :data="backupList" style="width: 100%; margin-top: 10px;">
              <el-table-column prop="filename" label="文件名" min-width="300" />
              <el-table-column prop="type" label="类型" width="100">
                <template #default="scope">
                  <el-tag :type="scope.row.type === 'FULL' ? 'warning' : 'info'">
                    {{ scope.row.type === 'FULL' ? '全量' : '增量' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="date" label="创建时间" width="180" />
              <el-table-column label="操作" width="150">
                <template #default="scope">
                  <el-button size="small" type="primary" @click="restoreBackup(scope.row.filename)">恢复</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
        <template #footer>
          <el-button @click="backupManagerVisible = false">关闭</el-button>
        </template>
      </el-dialog>

      <!-- 用户升级对话框 -->
      <el-dialog v-model="upgradeDialogVisible" title="版本升级" width="500px">
        <el-form :model="upgradeForm" label-width="100px">
          <el-form-item label="选择用户">
            <el-select v-model="upgradeForm.userId" placeholder="请选择用户" style="width: 100%" filterable>
              <el-option v-for="user in users" :key="user.id" :label="user.username + ' (' + user.nickname + ')'" :value="user.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="升级版本">
            <el-select v-model="upgradeForm.versionType" style="width: 100%">
              <el-option label="专业版" value="PRO" />
              <el-option label="企业版" value="ENTERPRISE" />
            </el-select>
          </el-form-item>
          <el-form-item label="购买时长">
            <el-select v-model="upgradeForm.durationDays" style="width: 100%">
              <el-option label="1个月" :value="30" />
              <el-option label="3个月" :value="90" />
              <el-option label="6个月" :value="180" />
              <el-option label="12个月" :value="365" />
            </el-select>
          </el-form-item>
          <el-form-item label="支付方式">
            <el-select v-model="upgradeForm.paymentMethod" style="width: 100%">
              <el-option label="支付宝" value="ALIPAY" />
              <el-option label="微信支付" value="WECHAT" />
              <el-option label="银行卡" value="BANK" />
            </el-select>
          </el-form-item>
          <el-form-item label="预估金额">
            <el-input :value="calculateUpgradeAmount()" disabled />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="upgradeDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitUpgrade">确认升级</el-button>
        </template>
      </el-dialog>

      <!-- 添加/编辑公告对话框 -->
      <el-dialog v-model="announcementDialogVisible" :title="isEditingAnnouncement ? '编辑公告' : '发布公告'" width="700px">
        <el-form :model="announcementForm" label-width="100px">
          <el-form-item label="标题" required>
            <el-input v-model="announcementForm.title" placeholder="请输入公告标题" />
          </el-form-item>
          <el-form-item label="内容" required>
            <el-input
              v-model="announcementForm.content"
              type="textarea"
              :rows="8"
              placeholder="请输入公告内容"
            />
          </el-form-item>
          <el-form-item label="优先级">
            <el-select v-model="announcementForm.priority" style="width: 100%">
              <el-option label="低" :value="0" />
              <el-option label="中" :value="1" />
              <el-option label="高" :value="2" />
              <el-option label="紧急" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item label="显示范围">
            <el-select v-model="announcementForm.displayScope" style="width: 100%">
              <el-option label="所有用户" value="ALL" />
              <el-option label="仅登录用户" value="LOGGED_IN" />
              <el-option label="仅管理员" value="ADMIN" />
            </el-select>
          </el-form-item>
          <el-form-item label="发布时间">
            <el-date-picker
              v-model="announcementForm.publishTime"
              type="datetime"
              placeholder="选择发布时间"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="开始时间">
            <el-date-picker
              v-model="announcementForm.startTime"
              type="datetime"
              placeholder="选择开始时间"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="结束时间">
            <el-date-picker
              v-model="announcementForm.endTime"
              type="datetime"
              placeholder="选择结束时间"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="作者">
            <el-input v-model="announcementForm.author" placeholder="请输入作者名称" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="announcementDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveAnnouncement">保存</el-button>
        </template>
      </el-dialog>

      <!-- 主题日志查看器 -->
  <ThemeLogViewer v-model="logViewerVisible" />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  updateAiSystemStatus,
  updateAiSystemRatelimit,
  getAiSystemDetail,
  getAiSystemPermissions,
  revokePermission,
  getOllamaStatus,
  getOllamaModels,
  pullOllamaModel,
  deleteOllamaModel,
  restartOllama,
  getAiConfig,
  updateAiConfig,
  getAiSystemStats
} from '@/api/aiSystem'
import {
  formatModelSize,
  ollamaStatusTag,
  normalizeAiConfig,
  maskApiKey,
  formatThousands,
  validateOllamaModelName
} from '@/utils/aiSystemManage'
import * as echarts from 'echarts'
import LocalAvatar from '@/components/LocalAvatar.vue'
import ThemeLogViewer from '@/components/ThemeLogViewer.vue'
import { useTheme } from '@/composables/useTheme'
import {
  logThemeOnMounted,
  logThemeSwitchTrigger,
  logThemeBefore,
  logThemeAfter,
  logThemeSwitchComplete
} from '@/utils/themeLogger'

const { currentTheme, toggleTheme, initTheme } = useTheme()

const router = useRouter()
const componentName = 'AdminView'

// 主题切换相关
let themeSwitchStartTime = null
let beforeThemeVariables = null

// 监听主题切换事件
const handleThemeSwitchStart = () => {
  themeSwitchStartTime = Date.now()
  beforeThemeVariables = logThemeBefore(componentName)
  logThemeSwitchTrigger(componentName, 'localStorage_change')
}

const handleThemeSwitchEnd = () => {
  if (themeSwitchStartTime && beforeThemeVariables) {
    const duration = Date.now() - themeSwitchStartTime
    logThemeAfter(componentName, beforeThemeVariables)
    logThemeSwitchComplete(componentName, duration)
    themeSwitchStartTime = null
    beforeThemeVariables = null
  }
}

// 监听 localStorage 变化
const storageListener = (event) => {
  if (event.key === 'theme') {
    handleThemeSwitchStart()
    // 延迟记录切换后的状态，确保 DOM 已更新
    setTimeout(handleThemeSwitchEnd, 350)
  }
}

const currentTab = ref('users')

// M3 ⑩ 数据看板
const loadingDashboard = ref(false)
const dashboard = ref({
  todayActiveUsers: 0,
  todayNewUsers: 0,
  todayChats: 0,
  activeTrend: [],
  newUserTrend: [],
  chatTrend: []
})
const activeTrendRef = ref(null)
const newUserTrendRef = ref(null)
const chatTrendRef = ref(null)
let dashboardCharts = []
const adminName = ref('管理员')
const adminInfo = ref({
  id: null,
  username: '',
  nickname: '',
  createTime: ''
})
const sidebarCollapsed = ref(false)
const logViewerVisible = ref(false)

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const changingPassword = ref(false)

const profileForm = ref({
  nickname: '',
  email: ''
})
const updatingProfile = ref(false)

const users = ref([])
const characters = ref([])
const loadingUsers = ref(false)
const loadingCharacters = ref(false)

const aiChatMessages = ref([])
const friendChatMessages = ref([])
const loadingAiChat = ref(false)
const loadingFriendChat = ref(false)
const aiChatPage = ref(1)
const friendChatPage = ref(1)
const aiChatTotal = ref(0)
const friendChatTotal = ref(0)

const messageChartRef = ref(null)
const characterChartRef = ref(null)
let messageChart = null
let characterChart = null

const stats = ref({
  userCount: 0,
  characterCount: 0,
  messageCount: 0,
  friendshipCount: 0,
  aiChatCount: 0,
  friendChatCount: 0,
  systemCharacterCount: 0,
  userCharacterCount: 0
})

const loadingStats = ref(false)
const dailyNewUsers = ref(0)
const chartPeriod = ref('7d')

const trendChartRef = ref(null)
let trendChart = null

const activeUsers = ref(0)

// 数据池相关
const dataPoolList = ref([])
const dataPoolTotal = ref(0)
const dataPoolPage = ref(1)
const dataPoolSearch = ref('')
const dataPoolFilterType = ref('')
const loadingDataPool = ref(false)
const dataPoolDetailVisible = ref(false)
const dataPoolDetail = ref({})
const addDataDialogVisible = ref(false)
const editDataDialogVisible = ref(false)
const dataPoolConfigVisible = ref(false)
const dataPoolCount = ref(0)
const dataPoolMaxCapacity = ref(10000)
const addDataForm = ref({
  title: '',
  dataType: 'text',
  category: '',
  format: '',
  version: 1,
  content: '',
  tags: '',
  description: '',
  isPublic: true
})
const editDataForm = ref({
  id: null,
  title: '',
  dataType: 'text',
  category: '',
  format: '',
  version: 1,
  content: '',
  tags: '',
  description: '',
  isPublic: true
})
const dataPoolConfig = ref({
  maxCapacity: 10000,
  autoCleanup: false,
  cleanupThreshold: 80,
  accessLevel: 'public',
  allowedUsers: [],
  backupStrategy: 'weekly'
})

// 导入相关
const importDialogVisible = ref(false)
const batchDeleteVisible = ref(false)
const systemLogsVisible = ref(false)
const selectedDataIds = ref([])
const isDragging = ref(false)
const uploadFiles = ref([])
const uploadProgress = ref(0)
const uploading = ref(false)
const importForm = ref({
  importType: 'file',
  sourceUrl: '',
  dataType: 'text',
  overwrite: false
})

// 日志筛选相关
const logFilterLevel = ref('')
const logFilterModule = ref('')
const logFilterKeyword = ref('')

// 公告管理相关
const announcements = ref([])
const announcementPage = ref(1)
const announcementTotal = ref(0)
const loadingAnnouncements = ref(false)
const announcementDialogVisible = ref(false)
const isEditingAnnouncement = ref(false)
const announcementFilterStatus = ref('')
const announcementSearch = ref('')
const announcementForm = ref({
  id: null,
  title: '',
  content: '',
  priority: 1,
  displayScope: 'ALL',
  publishTime: '',
  startTime: '',
  endTime: '',
  author: ''
})

// 用户版本管理相关
const userCounts = ref({
  freeCount: 0,
  proCount: 0,
  enterpriseCount: 0
})
const versionTab = ref('FREE')
const filteredUsers = ref([])
const proDuration = ref(30)
const enterpriseDuration = ref(30)
const upgradeDialogVisible = ref(false)
const upgradeForm = ref({
  userId: null,
  versionType: 'PRO',
  durationDays: 30,
  paymentMethod: 'ALIPAY'
})
const pendingPayments = ref([])
const loadingPayments = ref(false)

// 日志相关
const systemLogs = ref([])
const maxLogs = 100

// 生成日志ID
const generateLogId = () => {
  return 'log-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9)
}

// 添加日志
const addLog = (logData) => {
  const log = {
    id: generateLogId(),
    timestamp: new Date().toISOString(),
    ...logData
  }
  systemLogs.value.unshift(log)
  if (systemLogs.value.length > maxLogs) {
    systemLogs.value.pop()
  }
  // 同时输出到控制台
  console.log(`[${log.timestamp}] [${log.level.toUpperCase()}] ${log.message}`, log)
}

// 记录导入开始
const logImportStart = (importType, count) => {
  addLog({
    level: 'info',
    operation: 'import_start',
    module: 'datapool',
    user: adminName.value,
    message: `开始导入数据，导入方式: ${importType === 'json' ? 'JSON文件上传' : 'URL导入'}，预计数据量: ${count || '未知'}`,
    params: { importType, expectedCount: count }
  })
}

// 记录导入进度
const logImportProgress = (current, total) => {
  const progress = total > 0 ? Math.round((current / total) * 100) : 0
  addLog({
    level: 'info',
    operation: 'import_progress',
    module: 'datapool',
    user: adminName.value,
    message: `导入进度: ${current}/${total} (${progress}%)`,
    params: { current, total, progress }
  })
  
  // 容量阈值预警
  if (dataPoolUsage.value > 70 && dataPoolUsage.value <= 90) {
    addLog({
      level: 'warning',
      operation: 'capacity_warning',
      module: 'datapool',
      user: adminName.value,
      message: `容量预警：数据池使用率达到 ${dataPoolUsage.value}%，建议清理或扩容`,
      params: { usage: dataPoolUsage.value, maxCapacity: dataPoolMaxCapacity.value, currentCount: dataPoolCount.value }
    })
  } else if (dataPoolUsage.value > 90) {
    addLog({
      level: 'error',
      operation: 'capacity_critical',
      module: 'datapool',
      user: adminName.value,
      message: `容量严重不足：数据池使用率达到 ${dataPoolUsage.value}%，请立即处理`,
      params: { usage: dataPoolUsage.value, maxCapacity: dataPoolMaxCapacity.value, currentCount: dataPoolCount.value }
    })
  }
}

// 记录导入完成
const logImportComplete = (successCount, failedCount, errorMessage) => {
  const status = failedCount === 0 ? '成功' : '部分成功'
  addLog({
    level: failedCount === 0 ? 'success' : 'warning',
    operation: 'import_complete',
    module: 'datapool',
    user: adminName.value,
    message: `导入${status}，成功: ${successCount}条，失败: ${failedCount}条${errorMessage ? '，错误信息: ' + errorMessage : ''}`,
    params: { successCount, failedCount, errorMessage }
  })
}

// 记录导出开始
const logExportStart = (filters) => {
  addLog({
    level: 'info',
    operation: 'export_start',
    module: 'datapool',
    user: adminName.value,
    message: `开始导出数据，筛选条件: ${filters.keyword || '无'}，类型: ${filters.dataType || '全部'}`,
    params: filters
  })
}

// 记录导出进度
const logExportProgress = (current, total) => {
  const progress = total > 0 ? Math.round((current / total) * 100) : 0
  addLog({
    level: 'info',
    operation: 'export_progress',
    module: 'datapool',
    user: adminName.value,
    message: `导出进度: ${current}/${total} (${progress}%)`,
    params: { current, total, progress }
  })
}

// 记录导出完成
const logExportComplete = (success, errorMessage) => {
  addLog({
    level: success ? 'success' : 'error',
    operation: 'export_complete',
    module: 'datapool',
    user: adminName.value,
    message: success ? '导出成功' : '导出失败，错误信息: ' + errorMessage,
    params: { success, errorMessage }
  })
}

// 记录权限验证
const logPermissionCheck = (permission, resource, allowed, user) => {
  addLog({
    level: allowed ? 'info' : 'warning',
    operation: 'permission_check',
    module: 'auth',
    user: user || adminName.value,
    message: `${allowed ? '权限通过' : '权限拒绝'}：用户尝试访问${resource}，需要权限: ${permission}`,
    params: { permission, resource, allowed, userId: user || 'admin' }
  })
}

// 记录权限变更
const logPermissionChange = (action, userId, permissions, changedBy) => {
  addLog({
    level: 'info',
    operation: 'permission_change',
    module: 'auth',
    user: changedBy || adminName.value,
    message: `${action === 'grant' ? '授予权限' : action === 'revoke' ? '撤销权限' : '修改权限'}：用户 ${userId} 的权限变更为: ${permissions.join(', ')}`,
    params: { action, userId, permissions }
  })
}

// 记录权限异常尝试
const logPermissionViolation = (userId, resource, attemptedAction) => {
  addLog({
    level: 'error',
    operation: 'permission_violation',
    module: 'auth',
    user: userId,
    message: `权限异常：用户 ${userId} 尝试${attemptedAction} ${resource}，但无权限`,
    params: { userId, resource, attemptedAction }
  })
}

// 记录配置变更
const logConfigChange = (configKey, oldValue, newValue) => {
  addLog({
    level: 'info',
    operation: 'config_change',
    module: 'datapool',
    user: adminName.value,
    message: `配置变更：${configKey} 从 ${JSON.stringify(oldValue)} 修改为 ${JSON.stringify(newValue)}`,
    params: { configKey, oldValue, newValue }
  })
}

// 日志查看相关
const showSystemLogs = () => {
  systemLogsVisible.value = true
}

const filteredLogs = computed(() => {
  return systemLogs.value.filter(log => {
    if (logFilterLevel.value && log.level !== logFilterLevel.value) return false
    if (logFilterModule.value && log.module !== logFilterModule.value) return false
    if (logFilterKeyword.value && !log.message.includes(logFilterKeyword.value)) return false
    return true
  })
})

const filterLogs = () => {
  // 计算属性会自动更新
}

const clearLogFilter = () => {
  logFilterLevel.value = ''
  logFilterModule.value = ''
  logFilterKeyword.value = ''
}

const formatLogTime = (timestamp) => {
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 计算数据池使用率
const dataPoolUsage = computed(() => {
  return Math.round((dataPoolCount.value / dataPoolMaxCapacity.value) * 100)
})

// 统计计算属性
const textCount = computed(() => {
  return dataPoolList.value.filter(item => item.dataType === 'text').length
})

const structCount = computed(() => {
  return dataPoolList.value.filter(item => item.dataType === 'structured').length
})

const mediaCount = computed(() => {
  return dataPoolList.value.filter(item => 
    ['image', 'audio', 'video'].includes(item.dataType)
  ).length
})

// AI系统相关
const aiSystemList = ref([])
const loadingAiSystem = ref(false)
const registerSystemDialogVisible = ref(false)
const registerSystemForm = ref({
  systemName: '',
  systemType: 'chatbot',
  description: '',
  rateLimit: 100
})
const processingRequests = ref(0)
const avgResponseTime = ref(0)

// AI 系统管理：使用统计 / Ollama / 全局配置 / 列表增强
const aiStats = ref({
  totalSystems: 0,
  activeSystems: 0,
  totalCalls: 0,
  todayCalls: 0,
  topSystems: []
})
const loadingAiStats = ref(false)

const ollamaStatus = ref({})
const ollamaModels = ref([])
const loadingOllamaModels = ref(false)
const pullingModel = ref(false)
const restartingOllama = ref(false)
const pullModelForm = ref({ name: '' })

const aiConfigForm = ref(normalizeAiConfig(null))
const loadingAiConfig = ref(false)
const savingAiConfig = ref(false)

const ratelimitDialogVisible = ref(false)
const ratelimitForm = ref({ id: null, systemName: '', rateLimit: 100 })
const savingRatelimit = ref(false)

const permissionDialogVisible = ref(false)
const permissionTarget = ref({})
const systemPermissions = ref([])
const loadingPermissions = ref(false)

const systemDetailVisible = ref(false)
const systemDetail = ref({})

const ollamaTag = computed(() => ollamaStatusTag(ollamaStatus.value.available))
const ollamaTagType = computed(() => ollamaTag.value.type)
const ollamaTagText = computed(() => ollamaTag.value.text)

// 数据池相关函数
const loadDataPool = async () => {
  loadingDataPool.value = true
  try {
    const params = {
      page: dataPoolPage.value,
      size: 20,
      keyword: dataPoolSearch.value,
      dataType: dataPoolFilterType.value
    }
    const response = await request.get('/api/admin/data-pool/records', { params })
    if (response.code === 200) {
      dataPoolList.value = response.data.records || []
      dataPoolTotal.value = response.data.total || 0
    }
  } catch (error) {
    ElMessage.error('加载数据池失败: ' + error.message)
  } finally {
    loadingDataPool.value = false
  }
}

const showAddDataDialog = () => {
  addDataForm.value = {
    title: '',
    dataType: 'text',
    category: '',
    format: '',
    version: 1,
    content: '',
    tags: '',
    description: '',
    isPublic: true
  }
  addDataDialogVisible.value = true
}

const submitAddData = async () => {
  if (!addDataForm.value.title || !addDataForm.value.content) {
    ElMessage.warning('请填写必填项')
    return
  }
  try {
    const response = await request.post('/api/admin/data-pool/records', addDataForm.value)
    if (response.code === 200) {
      ElMessage.success('添加成功')
      addDataDialogVisible.value = false
      loadDataPool()
    }
  } catch (error) {
    ElMessage.error('添加失败：' + error.message)
  }
}

const editData = (row) => {
  editDataForm.value = {
    id: row.id,
    title: row.title,
    dataType: row.dataType,
    category: row.category,
    format: row.format,
    version: row.version,
    content: row.content || '',
    tags: row.tags || '',
    description: row.description || '',
    isPublic: row.isPublic !== undefined ? row.isPublic : true
  }
  editDataDialogVisible.value = true
}

const submitEditData = async () => {
  if (!editDataForm.value.title || !editDataForm.value.content) {
    ElMessage.warning('请填写必填项')
    return
  }
  try {
    const response = await request.put(`/api/admin/data-pool/records/${editDataForm.value.id}`, editDataForm.value)
    if (response.code === 200) {
      ElMessage.success('更新成功')
      editDataDialogVisible.value = false
      loadDataPool()
    }
  } catch (error) {
    ElMessage.error('更新失败：' + error.message)
  }
}

const showDataPoolConfig = async () => {
  try {
    // 加载当前配置
    const response = await request.get('/api/admin/data-pool/config')
    if (response.code === 200) {
      const config = response.data.data
      dataPoolConfig.value = {
        maxCapacity: config.maxCapacity || 10000,
        autoCleanup: config.autoCleanup || false,
        cleanupThreshold: config.cleanupThreshold || 80,
        accessLevel: config.accessLevel || 'public',
        allowedUsers: config.allowedUsers || [],
        backupStrategy: config.backupStrategy || 'weekly'
      }
    }
  } catch (error) {
    // 使用默认配置
  }
  dataPoolConfigVisible.value = true
}

const submitDataPoolConfig = async () => {
  // 记录配置变更
  const oldMaxCapacity = dataPoolMaxCapacity.value
  logConfigChange('maxCapacity', oldMaxCapacity, dataPoolConfig.value.maxCapacity)
  logConfigChange('autoCleanup', false, dataPoolConfig.value.autoCleanup)
  logConfigChange('cleanupThreshold', 80, dataPoolConfig.value.cleanupThreshold)
  logConfigChange('accessLevel', 'public', dataPoolConfig.value.accessLevel)
  
  // 记录权限变更
  logPermissionChange('modify', 'datapool', ['read', 'write', 'config'], adminName.value)
  
  try {
    const response = await request.post('/api/admin/data-pool/config', dataPoolConfig.value)
    if (response.code === 200) {
      ElMessage.success('配置保存成功')
      dataPoolConfigVisible.value = false
      // 更新最大容量
      dataPoolMaxCapacity.value = dataPoolConfig.value.maxCapacity
      loadDataPool()
    }
  } catch (error) {
    ElMessage.error('保存配置失败：' + error.message)
  }
}

const formatCapacityUsage = (percentage) => {
  return `${percentage}% (${dataPoolCount.value}/${dataPoolMaxCapacity.value})`
}

// 导入数据相关函数
const showImportDialog = () => {
  importForm.value = {
    importType: 'json',
    sourceUrl: '',
    dataType: 'text',
    overwrite: false
  }
  importDialogVisible.value = true
}

const submitImport = async () => {
  if (importForm.value.importType === 'url' && !importForm.value.sourceUrl) {
    ElMessage.warning('请输入数据源URL')
    return
  }
  
  if (importForm.value.importType === 'file' && uploadFiles.value.length === 0) {
    ElMessage.warning('请选择要上传的文件')
    return
  }
  
  logImportStart(importForm.value.importType, uploadFiles.value.length || '未知')
  
  if (importForm.value.importType === 'file') {
    uploading.value = true
    uploadProgress.value = 0
    
    for (let i = 0; i < uploadFiles.value.length; i++) {
      const file = uploadFiles.value[i]
      const formData = new FormData()
      formData.append('file', file)
      formData.append('dataType', importForm.value.dataType)
      formData.append('overwrite', importForm.value.overwrite.toString())
      
      try {
        const response = await request.post('/api/admin/data-pool/import', formData, {
          onUploadProgress: (progressEvent) => {
            const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total)
            uploadProgress.value = percentCompleted
          }
        })
        
        if (response.code === 200) {
          logImportProgress(response.data.count || 1, uploadFiles.value.length)
        }
        
        uploadProgress.value = Math.round(((i + 1) / uploadFiles.value.length) * 100)
      } catch (error) {
        logImportComplete(0, 0, error.message)
        ElMessage.error(`上传文件 "${file.name}" 失败: ${error.message}`)
      }
    }
    
    uploading.value = false
    uploadProgress.value = 100
    ElMessage.success(`成功导入 ${uploadFiles.value.length} 个文件`)
    importDialogVisible.value = false
    uploadFiles.value = []
    uploadProgress.value = 0
    loadDataPool()
    return
  }
  
  try {
    const response = await request.post('/api/admin/data-pool/import', importForm.value)
    if (response.code === 200) {
      logImportProgress(response.data.count, response.data.count)
      logImportComplete(response.data.count, 0, null)
      ElMessage.success(`成功导入 ${response.data.count} 条数据`)
      importDialogVisible.value = false
      loadDataPool()
    } else {
      logImportComplete(0, 0, response.message)
    }
  } catch (error) {
    logImportComplete(0, 0, error.message)
    ElMessage.error('导入失败：' + error.message)
  }
}

const handleImportSuccess = (response) => {
  if (response.code === 200) {
    ElMessage.success(`成功导入 ${response.data.count} 条数据`)
    importDialogVisible.value = false
    loadDataPool()
  }
}

const handleImportError = (error) => {
  ElMessage.error('导入失败：' + error.message)
}

const beforeImportUpload = (file) => {
  const isJson = file.type === 'application/json' || file.name.endsWith('.json')
  if (!isJson) {
    ElMessage.error('只能上传 JSON 文件')
    return false
  }
  return true
}

const handleDrop = (event) => {
  isDragging.value = false
  const files = Array.from(event.dataTransfer.files)
  validateAndAddFiles(files)
}

const handleFileSelect = (event) => {
  const files = Array.from(event.target.files)
  validateAndAddFiles(files)
  event.target.value = ''
}

const validateAndAddFiles = (files) => {
  const maxSize = 50 * 1024 * 1024
  const allowedExtensions = ['.txt', '.md', '.pdf', '.docx', '.xlsx', '.pptx', '.json', '.csv', '.jpg', '.jpeg', '.png', '.gif', '.webp', '.mp3', '.wav', '.mp4', '.mov']
  
  files.forEach(file => {
    const extension = file.name.toLowerCase().substring(file.name.lastIndexOf('.'))
    if (!allowedExtensions.includes(extension)) {
      ElMessage.warning(`文件 "${file.name}" 格式不支持，请上传支持的格式`)
      return
    }
    
    if (file.size > maxSize) {
      ElMessage.warning(`文件 "${file.name}" 大小超过 50MB 限制`)
      return
    }
    
    uploadFiles.value.push(file)
  })
  
  if (files.length > 0) {
    logImportStart('file', files.length)
  }
}

const removeUploadFile = (index) => {
  uploadFiles.value.splice(index, 1)
}

const resetUploadFiles = () => {
  uploadFiles.value = []
  uploadProgress.value = 0
}

const getFileIconClass = (type) => {
  if (type.startsWith('image/')) return 'icon-image'
  if (type.startsWith('audio/')) return 'icon-audio'
  if (type.startsWith('video/')) return 'icon-video'
  return 'icon-file'
}

const getFileFormat = (fileName) => {
  return fileName.substring(fileName.lastIndexOf('.') + 1).toUpperCase()
}

// 导出数据
const exportDataPool = async () => {
  const filters = {
    keyword: dataPoolSearch.value,
    dataType: dataPoolFilterType.value
  }
  
  // 记录导出开始
  logExportStart(filters)
  
  try {
    const response = await request.get('/api/admin/data-pool/export', { 
      params: filters,
      responseType: 'blob'
    })
    
    // 记录导出进度
    logExportProgress(1, 1)
    
    // 创建下载链接
    const blob = new Blob([response.data], { type: 'application/json' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `datapool_${new Date().toISOString().split('T')[0]}.json`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    
    // 记录导出完成
    logExportComplete(true, null)
    ElMessage.success('导出成功')
  } catch (error) {
    logExportComplete(false, error.message)
    ElMessage.error('导出失败：' + error.message)
  }
}

// 批量操作相关
const handleDataSelectionChange = (val) => {
  selectedDataIds.value = val.map(item => item.id)
}

const batchDelete = () => {
  if (selectedDataIds.value.length === 0) {
    ElMessage.warning('请先选择要删除的数据')
    return
  }
  batchDeleteVisible.value = true
}

const confirmBatchDelete = async () => {
  try {
    const response = await request.delete('/api/admin/data-pool/records/batch', {
      data: selectedDataIds.value
    })
    if (response.code === 200) {
      ElMessage.success('批量删除成功')
      batchDeleteVisible.value = false
      selectedDataIds.value = []
      loadDataPool()
    }
  } catch (error) {
    ElMessage.error('批量删除失败：' + error.message)
  }
}

const viewDataDetail = async (row) => {
  try {
    const response = await request.get(`/api/admin/data-pool/records/${row.id}`)
    if (response.code === 200) {
      dataPoolDetail.value = response.data.data
      dataPoolDetailVisible.value = true
    }
  } catch (error) {
    ElMessage.error('获取数据详情失败: ' + error.message)
  }
}

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
      loadDataPool()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + error.message)
    }
  }
}

// AI系统相关函数
const loadAiSystems = async () => {
  loadingAiSystem.value = true
  try {
    const response = await request.get('/api/aisystem/list')
    if (response.code === 200) {
      aiSystemList.value = response.data.data || []
    }
  } catch (error) {
    ElMessage.error('加载AI系统列表失败: ' + error.message)
  } finally {
    loadingAiSystem.value = false
  }
}

const showRegisterSystemDialog = () => {
  registerSystemForm.value = {
    systemName: '',
    systemType: 'chatbot',
    description: '',
    rateLimit: 100
  }
  registerSystemDialogVisible.value = true
}

const submitRegisterSystem = async () => {
  try {
    const response = await request.post('/api/aisystem/register', registerSystemForm.value)
    if (response.code === 200) {
      ElMessage.success('注册成功')
      registerSystemDialogVisible.value = false
      loadAiSystems()
      ElMessageBox.alert(
        `API Key: ${response.data.apiKey}\nSecret Key: ${response.data.secretKey}`,
        '请保存密钥',
        { confirmButtonText: '确定', type: 'success' }
      )
    }
  } catch (error) {
    ElMessage.error('注册失败: ' + error.message)
  }
}

const regenerateSystemKey = async (row) => {
  try {
    await ElMessageBox.confirm('确定要重新生成API密钥吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const response = await request.post(`/api/aisystem/${row.id}/regenerate-key`)
    if (response.code === 200) {
      ElMessage.success('密钥已重新生成')
      loadAiSystems()
      ElMessageBox.alert(
        `新的 API Key: ${response.data.apiKey}\n新的 Secret Key: ${response.data.secretKey}`,
        '请保存新密钥',
        { confirmButtonText: '确定', type: 'success' }
      )
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败: ' + error.message)
    }
  }
}

const deleteAiSystem = async (row) => {
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

// ===== 使用统计 =====
const loadAiSystemStats = async () => {
  loadingAiStats.value = true
  try {
    const response = await getAiSystemStats()
    if (response.code === 200 && response.data) {
      aiStats.value = {
        totalSystems: response.data.totalSystems || 0,
        activeSystems: response.data.activeSystems || 0,
        totalCalls: response.data.totalCalls || 0,
        todayCalls: response.data.todayCalls || 0,
        topSystems: response.data.topSystems || []
      }
    }
  } catch (error) {
    ElMessage.error('加载使用统计失败: ' + error.message)
  } finally {
    loadingAiStats.value = false
  }
}

// ===== Ollama =====
const loadOllamaStatus = async () => {
  try {
    const response = await getOllamaStatus()
    if (response.code === 200 && response.data) {
      ollamaStatus.value = response.data
      if (Array.isArray(response.data.models)) {
        ollamaModels.value = response.data.models
      }
    }
  } catch (error) {
    ElMessage.error('加载 Ollama 状态失败: ' + error.message)
  }
}

const loadOllamaModels = async () => {
  loadingOllamaModels.value = true
  try {
    const response = await getOllamaModels()
    if (response.code === 200 && Array.isArray(response.data)) {
      ollamaModels.value = response.data
    } else if (response.code === 200 && response.data && Array.isArray(response.data.models)) {
      ollamaModels.value = response.data.models
    }
  } catch (error) {
    ElMessage.error('加载 Ollama 模型列表失败: ' + error.message)
  } finally {
    loadingOllamaModels.value = false
  }
}

const handlePullModel = async () => {
  const name = (pullModelForm.value.name || '').trim()
  const err = validateOllamaModelName(name)
  if (err) {
    ElMessage.warning(err)
    return
  }
  pullingModel.value = true
  try {
    const response = await pullOllamaModel(name)
    if (response.code === 200) {
      ElMessage.success(response.data?.message || `模型 ${name} 拉取完成`)
      pullModelForm.value.name = ''
      loadOllamaModels()
      loadOllamaStatus()
    }
  } catch (error) {
    ElMessage.error('拉取模型失败: ' + error.message)
  } finally {
    pullingModel.value = false
  }
}

const handleDeleteOllamaModel = async (name) => {
  try {
    const response = await deleteOllamaModel(name)
    if (response.code === 200) {
      ElMessage.success(`模型 ${name} 已删除`)
      loadOllamaModels()
      loadOllamaStatus()
    }
  } catch (error) {
    ElMessage.error('删除模型失败: ' + error.message)
  }
}

const handleRestartOllama = async () => {
  restartingOllama.value = true
  try {
    const response = await restartOllama()
    if (response.code === 200) {
      ElMessage.success('Ollama 重启指令已发送')
      setTimeout(() => {
        loadOllamaStatus()
        loadOllamaModels()
      }, 1500)
    }
  } catch (error) {
    ElMessage.error('重启 Ollama 失败: ' + error.message)
  } finally {
    restartingOllama.value = false
  }
}

// ===== AI 全局配置 =====
const loadAiConfig = async () => {
  loadingAiConfig.value = true
  try {
    const response = await getAiConfig()
    if (response.code === 200 && response.data) {
      aiConfigForm.value = normalizeAiConfig(response.data)
    }
  } catch (error) {
    ElMessage.error('加载 AI 配置失败: ' + error.message)
  } finally {
    loadingAiConfig.value = false
  }
}

const handleSaveAiConfig = async () => {
  savingAiConfig.value = true
  try {
    const response = await updateAiConfig({ ...aiConfigForm.value })
    if (response.code === 200) {
      ElMessage.success('配置已保存')
    }
  } catch (error) {
    ElMessage.error('保存配置失败: ' + error.message)
  } finally {
    savingAiConfig.value = false
  }
}

// ===== 系统列表增强 =====
const handleToggleSystemStatus = async (row, val) => {
  const newStatus = val ? 1 : 0
  try {
    const response = await updateAiSystemStatus(row.id, newStatus)
    if (response.code === 200) {
      ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
      row.status = newStatus
      loadAiSystemStats()
    }
  } catch (error) {
    ElMessage.error('状态切换失败: ' + error.message)
  }
}

const showRatelimitDialog = (row) => {
  ratelimitForm.value = {
    id: row.id,
    systemName: row.systemName,
    rateLimit: row.rateLimit ?? 100
  }
  ratelimitDialogVisible.value = true
}

const handleSaveRatelimit = async () => {
  savingRatelimit.value = true
  try {
    const response = await updateAiSystemRatelimit(ratelimitForm.value.id, Number(ratelimitForm.value.rateLimit))
    if (response.code === 200) {
      ElMessage.success('速率限制已更新')
      ratelimitDialogVisible.value = false
      loadAiSystems()
    }
  } catch (error) {
    ElMessage.error('保存失败: ' + error.message)
  } finally {
    savingRatelimit.value = false
  }
}

const showPermissionDialog = async (row) => {
  permissionTarget.value = row
  permissionDialogVisible.value = true
  systemPermissions.value = []
  loadingPermissions.value = true
  try {
    const response = await getAiSystemPermissions(row.id)
    if (response.code === 200 && Array.isArray(response.data)) {
      systemPermissions.value = response.data
    } else if (response.code === 200 && response.data && Array.isArray(response.data.records)) {
      systemPermissions.value = response.data.records
    }
  } catch (error) {
    ElMessage.error('加载权限失败: ' + error.message)
  } finally {
    loadingPermissions.value = false
  }
}

const handleRevokePermission = async (perm) => {
  try {
    const response = await revokePermission(perm.id ?? perm.permissionId)
    if (response.code === 200) {
      ElMessage.success('权限已撤销')
      const id = perm.id ?? perm.permissionId
      systemPermissions.value = systemPermissions.value.filter(p => (p.id ?? p.permissionId) !== id)
    }
  } catch (error) {
    ElMessage.error('撤销权限失败: ' + error.message)
  }
}

const showSystemDetail = async (row) => {
  systemDetail.value = { ...row }
  systemDetailVisible.value = true
  try {
    const response = await getAiSystemDetail(row.id)
    if (response.code === 200 && response.data) {
      systemDetail.value = response.data
    }
  } catch (error) {
    // 详情拉取失败时保留列表行数据
  }
}

// 初始化 AI 系统管理面板所有数据
const loadAiSystemPanel = () => {
  loadAiSystems()
  loadAiSystemStats()
  loadOllamaStatus()
  loadOllamaModels()
  loadAiConfig()
}

// 格式化函数
const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}
const serverLoad = ref(0)

const todayRegistrations = ref(0)
const todayMessages = ref(0)
const todayFriendRequests = ref(0)
const todayNewCharacters = ref(0)

const userDialogVisible = ref(false)
const isEditingUser = ref(false)
const userForm = ref({
  id: null,
  username: '',
  nickname: '',
  password: ''
})

const characterDialogVisible = ref(false)
const isEditingCharacter = ref(false)
const characterForm = ref({
  id: null,
  name: '',
  avatar: '',
  prompt: '',
  isSystem: false
})

const aiChatDetailVisible = ref(false)
const aiChatDetail = ref({
  id: null,
  sessionId: '',
  senderType: '',
  content: '',
  createTime: ''
})

const friendChatDetailVisible = ref(false)
const friendChatDetail = ref({
  id: null,
  senderId: null,
  receiverId: null,
  content: '',
  createTime: ''
})

const sessions = ref([])
const selectedSession = ref(null)
const currentSession = ref(null)
const sessionMessages = ref([])
const chatMessagesRef = ref(null)
const filterUserId = ref(null)
const filterCharacterId = ref(null)
const friendChatFilterSender = ref(null)
const friendChatFilterReceiver = ref(null)

const loadCharacters = async () => {
  loadingCharacters.value = true
  try {
    const response = await request.get('/api/admin/characters')
    if (response.code === 200) {
      characters.value = response.data
    }
  } catch (error) {
    ElMessage.error('加载角色列表失败')
  } finally {
    loadingCharacters.value = false
  }
}

const loadStats = async () => {
  loadingStats.value = true
  try {
    const response = await request.get('/api/admin/stats')
    if (response.code === 200) {
      const data = response.data
      stats.value = {
        userCount: data.userCount || 0,
        characterCount: data.characterCount || 0,
        friendshipCount: data.friendshipCount || 0,
        messageCount: data.messageCount || 0,
        aiChatCount: data.aiChatCount || 0,
        friendChatCount: data.friendChatCount || 0,
        systemCharacterCount: data.systemCharacterCount || 0,
        userCharacterCount: data.userCharacterCount || 0
      }
      dailyNewUsers.value = data.dailyNewUsers || 0
      todayRegistrations.value = data.dailyNewUsers || 0
      todayMessages.value = data.todayMessages || 0
      todayFriendRequests.value = data.todayFriendRequests || 0
      todayNewCharacters.value = data.todayNewCharacters || 0
      // 实时监控指标
      activeUsers.value = data.activeUsers || 0
      processingRequests.value = data.processingRequests || 0
      avgResponseTime.value = data.avgResponseTime || 0
      serverLoad.value = data.serverLoad || 0
      updateCharts()
    }
  } catch (error) {
    console.error('加载统计数据失败', error)
  } finally {
    loadingStats.value = false
  }
}

// M3 ⑩ 加载数据看板
const loadDashboard = async () => {
  loadingDashboard.value = true
  try {
    const response = await request.get('/api/admin/dashboard/stats')
    if (response.code === 200) {
      dashboard.value = {
        todayActiveUsers: response.data.todayActiveUsers || 0,
        todayNewUsers: response.data.todayNewUsers || 0,
        todayChats: response.data.todayChats || 0,
        activeTrend: response.data.activeTrend || [],
        newUserTrend: response.data.newUserTrend || [],
        chatTrend: response.data.chatTrend || []
      }
      await nextTick()
      renderDashboardCharts()
    }
  } catch (error) {
    console.error('加载数据看板失败', error)
  } finally {
    loadingDashboard.value = false
  }
}

const renderDashboardCharts = () => {
  // 清理旧实例
  dashboardCharts.forEach(ch => ch && ch.dispose && ch.dispose())
  dashboardCharts = []
  const renderOne = (elRef, title, data, color) => {
    if (!elRef.value) return
    const chart = echarts.init(elRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '10%', top: '15%', containLabel: true },
      xAxis: {
        type: 'category',
        data: data.map(d => d.date),
        axisLabel: { color: '#6b7280' }
      },
      yAxis: { type: 'value', minInterval: 1, axisLabel: { color: '#6b7280' } },
      series: [{
        name: title,
        type: 'line',
        smooth: true,
        data: data.map(d => d.count),
        itemStyle: { color },
        areaStyle: { color: color + '22' }
      }]
    })
    dashboardCharts.push(chart)
  }
  renderOne(activeTrendRef, '活跃用户', dashboard.value.activeTrend, '#6366f1')
  renderOne(newUserTrendRef, '新增用户', dashboard.value.newUserTrend, '#22c55e')
  renderOne(chatTrendRef, '对话次数', dashboard.value.chatTrend, '#f59e0b')
}

const initCharts = () => {
  if (trendChartRef.value) {
    trendChart = echarts.init(trendChartRef.value)
    const trendOption = {
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderColor: '#e5e7eb',
        borderWidth: 1,
        textStyle: { color: '#374151' },
        formatter: '{b}<br/>{a0}: {c0}<br/>{a1}: {c1}'
      },
      legend: {
        data: ['AI聊天', '好友聊天'],
        bottom: 0,
        textStyle: { color: '#6b7280' }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '15%',
        top: '10%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: getDateLabels(),
        axisLine: { lineStyle: { color: '#e5e7eb' } },
        axisLabel: { color: '#6b7280', fontSize: 12 }
      },
      yAxis: {
        type: 'value',
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } },
        axisLabel: { color: '#6b7280', fontSize: 12 }
      },
      series: [
        {
          name: 'AI聊天',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          lineStyle: { width: 3, color: '#6366f1' },
          itemStyle: { color: '#6366f1' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(99, 102, 241, 0.3)' },
              { offset: 1, color: 'rgba(99, 102, 241, 0.05)' }
            ])
          },
          data: generateTrendData()
        },
        {
          name: '好友聊天',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          lineStyle: { width: 3, color: '#22c55e' },
          itemStyle: { color: '#22c55e' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(34, 197, 94, 0.3)' },
              { offset: 1, color: 'rgba(34, 197, 94, 0.05)' }
            ])
          },
          data: generateTrendData()
        }
      ]
    }
    trendChart.setOption(trendOption)
  }

  if (messageChartRef.value) {
    messageChart = echarts.init(messageChartRef.value)
    const messageOption = {
      tooltip: { 
        trigger: 'item',
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderColor: '#e5e7eb',
        borderWidth: 1,
        textStyle: { color: '#374151' }
      },
      series: [
        {
          name: '消息统计',
          type: 'pie',
          radius: ['50%', '75%'],
          center: ['50%', '50%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 8,
            borderColor: 'var(--text-primary)',
            borderWidth: 2
          },
          label: { show: false },
          emphasis: {
            label: { show: true, fontSize: 16, fontWeight: 'bold', color: '#1f2937' }
          },
          labelLine: { show: false },
          data: [
            { value: 0, name: 'AI聊天', itemStyle: { color: '#6366f1' } },
            { value: 0, name: '好友聊天', itemStyle: { color: '#22c55e' } }
          ]
        }
      ]
    }
    messageChart.setOption(messageOption)
  }

  if (characterChartRef.value) {
    characterChart = echarts.init(characterChartRef.value)
    const characterOption = {
      tooltip: { 
        trigger: 'item',
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderColor: '#e5e7eb',
        borderWidth: 1,
        textStyle: { color: '#374151' }
      },
      series: [
        {
          name: '角色分布',
          type: 'pie',
          radius: ['50%', '75%'],
          center: ['50%', '50%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 8,
            borderColor: 'var(--text-primary)',
            borderWidth: 2
          },
          label: { show: false },
          emphasis: {
            label: { show: true, fontSize: 16, fontWeight: 'bold', color: '#1f2937' }
          },
          labelLine: { show: false },
          data: [
            { value: 0, name: '系统角色', itemStyle: { color: '#8b5cf6' } },
            { value: 0, name: '用户创建', itemStyle: { color: '#f59e0b' } }
          ]
        }
      ]
    }
    characterChart.setOption(characterOption)
  }
}

const getDateLabels = () => {
  const labels = []
  const days = chartPeriod.value === '7d' ? 7 : 30
  for (let i = days - 1; i >= 0; i--) {
    const date = new Date()
    date.setDate(date.getDate() - i)
    if (days === 7) {
      const weekDays = ['日', '一', '二', '三', '四', '五', '六']
      labels.push(`周${weekDays[date.getDay()]}`)
    } else {
      labels.push(`${date.getMonth() + 1}/${date.getDate()}`)
    }
  }
  return labels
}

const generateTrendData = () => {
  const days = chartPeriod.value === '7d' ? 7 : 30
  // 基于真实统计数据生成趋势线，不使用随机数
  const avg = Math.round(stats.value.messageCount / days) || 0
  return new Array(days).fill(avg)
}

const updateCharts = () => {
  if (trendChart) {
    trendChart.setOption({
      xAxis: { data: getDateLabels() },
      series: [
        { data: generateTrendData() },
        { data: generateTrendData() }
      ]
    })
  }

  if (messageChart) {
    messageChart.setOption({
      series: [{
        data: [
          { value: stats.value.aiChatCount, name: 'AI聊天', itemStyle: { color: '#6366f1' } },
          { value: stats.value.friendChatCount, name: '好友聊天', itemStyle: { color: '#22c55e' } }
        ]
      }]
    })
  }

  if (characterChart) {
    characterChart.setOption({
      series: [{
        data: [
          { value: stats.value.systemCharacterCount, name: '系统角色', itemStyle: { color: '#8b5cf6' } },
          { value: stats.value.userCharacterCount, name: '用户创建', itemStyle: { color: '#f59e0b' } }
        ]
      }]
    })
  }
}

const loadAiChatMessages = async (page = 1) => {
  if (typeof page === 'object' && page !== null) {
    page = 1
  }
  loadingAiChat.value = true
  aiChatPage.value = page
  try {
    let url = `/api/admin/ai-chat-messages?page=${page}&size=20`
    if (filterUserId.value) {
      url += `&userId=${filterUserId.value}`
    }
    if (filterCharacterId.value) {
      url += `&characterId=${filterCharacterId.value}`
    }
    const response = await request.get(url)
    if (response.code === 200) {
      aiChatMessages.value = response.data.records
      aiChatTotal.value = response.data.total
    }
    await loadSessions()
  } catch (error) {
    ElMessage.error('加载AI聊天记录失败')
  } finally {
    loadingAiChat.value = false
  }
}

const loadSessions = async () => {
  try {
    const response = await request.get('/api/admin/sessions')
    if (response.code === 200) {
      let filteredSessions = response.data
      if (filterUserId.value) {
        filteredSessions = filteredSessions.filter(s => s.userId === filterUserId.value)
      }
      if (filterCharacterId.value) {
        filteredSessions = filteredSessions.filter(s => s.characterId === filterCharacterId.value)
      }
      sessions.value = filteredSessions
    }
  } catch (error) {
    console.error('加载会话列表失败', error)
  }
}

const selectSession = async (session) => {
  selectedSession.value = session
  currentSession.value = session
  try {
    const response = await request.get(`/api/admin/ai-chat-messages/session?sessionId=${encodeURIComponent(session.sessionId)}`)
    if (response.code === 200) {
      sessionMessages.value = response.data
      nextTick(() => {
        if (chatMessagesRef.value) {
          chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight
        }
      })
    } else {
      ElMessage.error('加载会话消息失败')
    }
  } catch (error) {
    console.error('加载会话消息失败:', error)
    ElMessage.error('加载会话消息失败: ' + (error.message || '网络错误'))
  }
}

const backToSessions = () => {
  selectedSession.value = null
  currentSession.value = null
  sessionMessages.value = []
}

const formatMessageTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const loadFriendChatMessages = async (page = 1) => {
  if (typeof page === 'object' && page !== null) {
    page = 1
  }
  loadingFriendChat.value = true
  friendChatPage.value = page
  try {
    let url = `/api/admin/friend-chat-messages?page=${page}&size=20`
    if (friendChatFilterSender.value) {
      url += `&senderId=${friendChatFilterSender.value}`
    }
    if (friendChatFilterReceiver.value) {
      url += `&receiverId=${friendChatFilterReceiver.value}`
    }
    const response = await request.get(url)
    if (response.code === 200) {
      friendChatMessages.value = response.data.records || []
      friendChatTotal.value = response.data.total || 0
    } else {
      ElMessage.error(response.msg || '加载好友聊天记录失败')
      friendChatMessages.value = []
      friendChatTotal.value = 0
    }
  } catch (error) {
    console.error('加载好友聊天记录失败:', error)
    ElMessage.error('加载好友聊天记录失败: ' + (error.message || '网络错误'))
    friendChatMessages.value = []
    friendChatTotal.value = 0
  } finally {
    loadingFriendChat.value = false
  }
}

const showAddUserDialog = () => {
  isEditingUser.value = false
  userForm.value = {
    id: null,
    username: '',
    nickname: '',
    password: ''
  }
  userDialogVisible.value = true
}

const editUser = (user) => {
  isEditingUser.value = true
  userForm.value = {
    id: user.id,
    username: user.username,
    nickname: user.nickname || '',
    password: ''
  }
  userDialogVisible.value = true
}

const saveUser = async () => {
  if (!userForm.value.username) {
    ElMessage.warning('请输入用户名')
    return
  }

  try {
    let response
    if (isEditingUser.value) {
      const data = {
        id: userForm.value.id,
        username: userForm.value.username,
        nickname: userForm.value.nickname
      }
      if (userForm.value.password) {
        data.password = userForm.value.password
      }
      response = await request.put(`/api/admin/users/${userForm.value.id}`, data)
    } else {
      if (!userForm.value.password) {
        ElMessage.warning('请输入密码')
        return
      }
      response = await request.post('/api/admin/users', userForm.value)
    }

    if (response.code === 200) {
      ElMessage.success(isEditingUser.value ? '更新成功' : '添加成功')
      userDialogVisible.value = false
      loadUsers()
    } else {
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const deleteUser = async (userId) => {
  try {
    await ElMessageBox.confirm('确定要删除该用户吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const response = await request.delete(`/api/admin/users/${userId}`)
    if (response.code === 200) {
      ElMessage.success('删除成功')
      loadUsers()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const showAddCharacterDialog = () => {
  isEditingCharacter.value = false
  characterForm.value = {
    id: null,
    name: '',
    avatar: '',
    prompt: '',
    isSystem: false
  }
  characterDialogVisible.value = true
}

const editCharacter = (character) => {
  isEditingCharacter.value = true
  characterForm.value = {
    id: character.id,
    name: character.name,
    avatar: character.avatar || '',
    prompt: character.prompt || '',
    isSystem: character.isSystem || false
  }
  characterDialogVisible.value = true
}

const saveCharacter = async () => {
  if (!characterForm.value.name) {
    ElMessage.warning('请输入角色名称')
    return
  }
  if (!characterForm.value.prompt) {
    ElMessage.warning('请输入角色描述')
    return
  }

  try {
    let response
    if (isEditingCharacter.value) {
      response = await request.put(`/api/character/${characterForm.value.id}`, characterForm.value)
    } else {
      response = await request.post('/api/character/create', characterForm.value)
    }

    if (response.code === 200) {
      ElMessage.success(isEditingCharacter.value ? '更新成功' : '添加成功')
      characterDialogVisible.value = false
      loadCharacters()
    } else {
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const deleteCharacter = async (characterId) => {
  try {
    await ElMessageBox.confirm('确定要删除该角色吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const response = await request.delete(`/api/character/${characterId}`)
    if (response.code === 200) {
      ElMessage.success('删除成功')
      loadCharacters()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const viewAiChatDetail = (row) => {
  aiChatDetail.value = {
    id: row.id,
    sessionId: row.sessionId,
    senderType: row.senderType === 'assistant' ? 'AI' : '用户',
    content: row.content,
    createTime: row.createTime
  }
  aiChatDetailVisible.value = true
}

const viewFriendChatDetail = (row) => {
  friendChatDetail.value = {
    id: row.id,
    senderId: row.senderId,
    receiverId: row.receiverId,
    content: row.content,
    createTime: row.createTime
  }
  friendChatDetailVisible.value = true
}

const deleteAiChatMessage = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这条聊天记录吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const response = await request.delete(`/api/admin/ai-chat-messages/${id}`)
    if (response.code === 200) {
      ElMessage.success('删除成功')
      loadAiChatMessages(aiChatPage.value)
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const deleteFriendChatMessage = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这条聊天记录吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const response = await request.delete(`/api/admin/friend-chat-messages/${id}`)
    if (response.code === 200) {
      ElMessage.success('删除成功')
      loadFriendChatMessages(friendChatPage.value)
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 公告管理相关函数
const loadAnnouncements = async (page = 1) => {
  loadingAnnouncements.value = true
  announcementPage.value = page
  try {
    let url = `/api/admin/announcements?page=${page}&size=20`
    if (announcementFilterStatus.value) {
      url += `&status=${announcementFilterStatus.value}`
    }
    const response = await request.get(url)
    if (response.code === 200) {
      announcements.value = response.data.records || []
      announcementTotal.value = response.data.total || 0
    }
  } catch (error) {
    ElMessage.error('加载公告列表失败: ' + error.message)
  } finally {
    loadingAnnouncements.value = false
  }
}

const showAddAnnouncementDialog = () => {
  isEditingAnnouncement.value = false
  announcementForm.value = {
    id: null,
    title: '',
    content: '',
    priority: 1,
    displayScope: 'ALL',
    publishTime: '',
    startTime: '',
    endTime: '',
    author: adminName.value
  }
  announcementDialogVisible.value = true
}

const editAnnouncement = (row) => {
  isEditingAnnouncement.value = true
  announcementForm.value = {
    id: row.id,
    title: row.title || '',
    content: row.content || '',
    priority: row.priority || 1,
    displayScope: row.displayScope || 'ALL',
    publishTime: row.publishTime || '',
    startTime: row.startTime || '',
    endTime: row.endTime || '',
    author: row.author || ''
  }
  announcementDialogVisible.value = true
}

const saveAnnouncement = async () => {
  if (!announcementForm.value.title || !announcementForm.value.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  try {
    let response
    const formData = {
      title: announcementForm.value.title,
      content: announcementForm.value.content,
      priority: announcementForm.value.priority,
      displayScope: announcementForm.value.displayScope,
      author: announcementForm.value.author || adminName.value
    }
    if (announcementForm.value.publishTime) {
      formData.publishTime = announcementForm.value.publishTime
    }
    if (announcementForm.value.startTime) {
      formData.startTime = announcementForm.value.startTime
    }
    if (announcementForm.value.endTime) {
      formData.endTime = announcementForm.value.endTime
    }
    
    if (isEditingAnnouncement.value) {
      response = await request.put(`/api/admin/announcements/${announcementForm.value.id}`, formData)
    } else {
      response = await request.post('/api/admin/announcements', formData)
    }
    
    if (response.code === 200) {
      ElMessage.success(isEditingAnnouncement.value ? '更新成功' : '发布成功')
      announcementDialogVisible.value = false
      loadAnnouncements()
    } else {
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('操作失败: ' + error.message)
  }
}

const publishAnnouncement = async (id) => {
  try {
    await ElMessageBox.confirm('确定要发布这条公告吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const response = await request.post(`/api/admin/announcements/${id}/publish`)
    if (response.code === 200) {
      ElMessage.success('发布成功')
      loadAnnouncements()
    } else {
      ElMessage.error(response.message || '发布失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败: ' + error.message)
    }
  }
}

const unpublishAnnouncement = async (id) => {
  try {
    await ElMessageBox.confirm('确定要撤回这条公告吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const response = await request.post(`/api/admin/announcements/${id}/unpublish`)
    if (response.code === 200) {
      ElMessage.success('撤回成功')
      loadAnnouncements()
    } else {
      ElMessage.error(response.message || '撤回失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败: ' + error.message)
    }
  }
}

const deleteAnnouncement = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这条公告吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const response = await request.delete(`/api/admin/announcements/${id}`)
    if (response.code === 200) {
      ElMessage.success('删除成功')
      loadAnnouncements()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败: ' + error.message)
    }
  }
}

const getPriorityType = (priority) => {
  const types = ['info', 'success', 'warning', 'danger']
  return types[priority] || 'info'
}

const getPriorityText = (priority) => {
  const texts = ['低', '中', '高', '紧急']
  return texts[priority] || '中'
}

const getDisplayScopeText = (scope) => {
  const map = {
    'ALL': '所有用户',
    'LOGGED_IN': '仅登录用户',
    'ADMIN': '仅管理员'
  }
  return map[scope] || '所有用户'
}

// 用户版本管理相关函数
const loadUsers = async () => {
  loadingUsers.value = true
  try {
    const [usersResponse, countsResponse] = await Promise.all([
      request.get('/api/admin/users'),
      request.get('/api/admin/users/count')
    ])
    if (usersResponse.code === 200) {
      users.value = usersResponse.data
      filteredUsers.value = usersResponse.data.filter(u => u.versionType === 'FREE' || !u.versionType)
    }
    if (countsResponse.code === 200) {
      userCounts.value = countsResponse.data
    }
  } catch (error) {
    ElMessage.error('加载用户列表失败')
  } finally {
    loadingUsers.value = false
  }
}

const loadUsersByVersion = async () => {
  loadingUsers.value = true
  try {
    const response = await request.get(`/api/admin/users/version/${versionTab.value}`)
    if (response.code === 200) {
      filteredUsers.value = response.data
    }
  } catch (error) {
    ElMessage.error('加载用户列表失败')
  } finally {
    loadingUsers.value = false
  }
}

const getVersionType = (version) => {
  const map = {
    'FREE': 'info',
    'PRO': 'success',
    'ENTERPRISE': 'warning'
  }
  return map[version] || 'info'
}

const getVersionText = (version) => {
  const map = {
    'FREE': '免费版',
    'PRO': '专业版',
    'ENTERPRISE': '企业版'
  }
  return map[version] || '免费版'
}

const getAccountStatusType = (status) => {
  const map = {
    'ACTIVE': 'success',
    'INACTIVE': 'info',
    'SUSPENDED': 'danger'
  }
  return map[status] || 'info'
}

const getAccountStatusText = (status) => {
  const map = {
    'ACTIVE': '正常',
    'INACTIVE': '未激活',
    'SUSPENDED': '已冻结'
  }
  return map[status] || '正常'
}

const getReviewStatusType = (status) => {
  const map = {
    'PENDING': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger'
  }
  return map[status] || 'info'
}

const getReviewStatusText = (status) => {
  const map = {
    'PENDING': '待审核',
    'APPROVED': '已通过',
    'REJECTED': '已拒绝'
  }
  return map[status] || '待审核'
}

const showUpgradeDialog = (versionType) => {
  upgradeForm.value = {
    userId: null,
    versionType: versionType,
    durationDays: versionType === 'PRO' ? proDuration.value : enterpriseDuration.value,
    paymentMethod: 'ALIPAY'
  }
  upgradeDialogVisible.value = true
}

const upgradeUser = (row) => {
  upgradeForm.value = {
    userId: row.id,
    versionType: row.versionType === 'FREE' ? 'PRO' : 'ENTERPRISE',
    durationDays: 30,
    paymentMethod: 'ALIPAY'
  }
  upgradeDialogVisible.value = true
}

const calculateUpgradeAmount = () => {
  const basePrice = upgradeForm.value.versionType === 'PRO' ? 99 : 999
  const days = upgradeForm.value.durationDays || 30
  const amount = (basePrice * days) / 30
  return '¥' + amount.toFixed(2)
}

const submitUpgrade = async () => {
  if (!upgradeForm.value.userId) {
    ElMessage.warning('请选择用户')
    return
  }
  try {
    const response = await request.post('/api/payment/create', {
      userId: upgradeForm.value.userId,
      versionType: upgradeForm.value.versionType,
      durationDays: upgradeForm.value.durationDays,
      paymentMethod: upgradeForm.value.paymentMethod
    })
    if (response.code === 200) {
      ElMessage.success('充值申请已提交，请等待管理员审核')
      upgradeDialogVisible.value = false
      loadPendingPayments()
    } else {
      ElMessage.error(response.message || '提交失败')
    }
  } catch (error) {
    ElMessage.error('提交失败: ' + error.message)
  }
}

const loadPendingPayments = async () => {
  loadingPayments.value = true
  try {
    const response = await request.get('/api/payment/pending')
    if (response.code === 200) {
      pendingPayments.value = response.data
    }
  } catch (error) {
    ElMessage.error('加载待审核记录失败')
  } finally {
    loadingPayments.value = false
  }
}

const approvePayment = async (row) => {
  try {
    await ElMessageBox.confirm('确定要通过这条充值申请吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'success'
    })
    const response = await request.post(`/api/payment/${row.id}/review`, {
      reviewStatus: 'APPROVED',
      reviewRemark: '审核通过'
    })
    if (response.code === 200) {
      ElMessage.success('审核通过，用户已升级')
      loadPendingPayments()
      loadUsers()
    } else {
      ElMessage.error(response.message || '审核失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败: ' + error.message)
    }
  }
}

const rejectPayment = async (row) => {
  try {
    const { value: remark } = await ElMessageBox.prompt('请输入拒绝原因', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    const response = await request.post(`/api/payment/${row.id}/review`, {
      reviewStatus: 'REJECTED',
      reviewRemark: remark || '审核未通过'
    })
    if (response.code === 200) {
      ElMessage.success('已拒绝')
      loadPendingPayments()
    } else {
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败: ' + error.message)
    }
  }
}

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  localStorage.removeItem('isAdmin')
  router.push('/login')
}

let statsRefreshTimer = null

onMounted(() => {
  // 初始化主题
  initTheme()
  
  // 记录组件挂载时的主题状态
  logThemeOnMounted(componentName)
  
  // 添加 localStorage 监听器
  window.addEventListener('storage', storageListener)
  
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  adminName.value = user.nickname || user.username || '管理员'

  loadUsers()
  loadCharacters()
  loadStats()
  loadUserDataRecords()

  // 每30秒自动刷新统计与数据池，保持实时状态
  statsRefreshTimer = setInterval(() => {
    loadStats()
    if (currentTab.value === 'userData') {
      loadUserDataRecords()
    }
  }, 30000)

  setTimeout(() => {
    initCharts()
  }, 100)
})

// 清理
onUnmounted(() => {
  // 移除 localStorage 监听器
  window.removeEventListener('storage', storageListener)
  // 清理统计定时刷新
  if (statsRefreshTimer) {
    clearInterval(statsRefreshTimer)
    statsRefreshTimer = null
  }
})

const handleTabChange = (tab) => {
  currentTab.value = tab
  if (tab === 'stats') {
    setTimeout(() => {
      initCharts()
      updateCharts()
    }, 100)
  } else if (tab === 'dashboard') {
    loadDashboard()
  } else if (tab === 'aiChat') {
    loadAiChatMessages()
  } else if (tab === 'friendChat') {
    loadFriendChatMessages()
  } else if (tab === 'datapool') {
    loadDataPool()
  } else if (tab === 'aisystem') {
    loadAiSystemPanel()
  } else if (tab === 'announcements') {
    loadAnnouncements()
  } else if (tab === 'users') {
    loadUsers()
    loadPendingPayments()
  } else if (tab === 'profile') {
    loadAdminInfo()
  } else if (tab === 'userData') {
    loadAutoSaveStatus()
    loadUserDataRecords()
  }
}

const loadAdminInfo = () => {
  const storedAdmin = localStorage.getItem('admin')
  if (storedAdmin) {
    try {
      const admin = JSON.parse(storedAdmin)
      adminInfo.value = admin
      adminName.value = admin.username || '管理员'
      profileForm.value = {
        nickname: admin.nickname || '',
        email: admin.email || ''
      }
    } catch (e) {
      console.error('解析管理员信息失败:', e)
    }
  }
}

const changePassword = async () => {
  if (!passwordForm.value.oldPassword) {
    ElMessage.error('请输入旧密码')
    return
  }
  if (!passwordForm.value.newPassword) {
    ElMessage.error('请输入新密码')
    return
  }
  if (passwordForm.value.newPassword.length < 6) {
    ElMessage.error('新密码长度不能少于6位')
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    ElMessage.error('两次输入的新密码不一致')
    return
  }

  changingPassword.value = true
  try {
    const response = await request.post('/api/admin/change-password', {
      adminId: adminInfo.value.id,
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    if (response.code === 200) {
      ElMessage.success(response.msg || '密码修改成功')
      resetPasswordForm()
    } else {
      ElMessage.error(response.msg || '密码修改失败')
    }
  } catch (error) {
    console.error('修改密码失败:', error)
    ElMessage.error('修改密码失败: ' + (error.message || '网络错误'))
  } finally {
    changingPassword.value = false
  }
}

const resetPasswordForm = () => {
  passwordForm.value = {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  }
}

const updateProfile = async () => {
  if (!profileForm.value.nickname) {
    ElMessage.error('请输入昵称')
    return
  }

  if (profileForm.value.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(profileForm.value.email)) {
    ElMessage.error('请输入正确的邮箱格式')
    return
  }

  updatingProfile.value = true
  try {
    const response = await request.post('/api/admin/update-profile', {
      adminId: adminInfo.value.id,
      nickname: profileForm.value.nickname,
      email: profileForm.value.email
    })
    if (response.code === 200) {
      ElMessage.success(response.msg || '个人信息修改成功')
      adminInfo.value.nickname = profileForm.value.nickname
      adminInfo.value.email = profileForm.value.email
      localStorage.setItem('admin', JSON.stringify(adminInfo.value))
    } else {
      ElMessage.error(response.msg || '个人信息修改失败')
    }
  } catch (error) {
    console.error('修改个人信息失败:', error)
    ElMessage.error('修改个人信息失败: ' + (error.message || '网络错误'))
  } finally {
    updatingProfile.value = false
  }
}

const resetProfileForm = () => {
  profileForm.value = {
    nickname: adminInfo.value.nickname || '',
    email: adminInfo.value.email || ''
  }
}

// 用户数据管理相关
const userDataRecords = ref([])
const userDataTotal = ref(0)
const userDataPage = ref(1)
const userDataSearch = ref('')
const userDataFilterType = ref('')
const userDataFilterStatus = ref('')
const loadingUserData = ref(false)
const selectedUserDataIds = ref([])
const userDataDetailVisible = ref(false)
const userDataDetail = ref({})
const userDataStats = ref({})

const autoSaveEnabled = ref(false)
const togglingAutoSave = ref(false)

const userDataAnalysisVisible = ref(false)
const analysisData = ref({})
const loadingAnalysis = ref(false)
const typeDistribution = ref([])
const tagDistribution = ref({})

const backupManagerVisible = ref(false)
const backupStats = ref({})
const backupList = ref([])
const loadingBackup = ref(false)
const performingBackup = ref(false)

const loadAutoSaveStatus = async () => {
  try {
    const response = await request.get('/api/admin/data-pool/auto-save/status')
    if (response.code === 200) {
      autoSaveEnabled.value = response.data.enabled || false
    }
  } catch (error) {
    console.error('加载自动保存状态失败:', error)
  }
}

const toggleAutoSave = async (value) => {
  togglingAutoSave.value = true
  try {
    const response = await request.post('/api/admin/data-pool/auto-save/toggle', {
      enabled: value
    })
    if (response.code === 200) {
      autoSaveEnabled.value = value
      ElMessage.success(value ? '自动保存已开启' : '自动保存已关闭')
    } else {
      autoSaveEnabled.value = !value
      ElMessage.error(response.msg || '操作失败')
    }
  } catch (error) {
    autoSaveEnabled.value = !value
    ElMessage.error('操作失败: ' + (error.message || '网络错误'))
  } finally {
    togglingAutoSave.value = false
  }
}

const loadUserDataRecords = async () => {
  loadingUserData.value = true
  try {
    const params = {
      page: userDataPage.value,
      size: 20
    }
    if (userDataFilterType.value) params.dataType = userDataFilterType.value
    
    const response = await request.get('/api/admin/data-pool/records', { params })
    if (response.code === 200) {
      userDataRecords.value = response.data.records || []
      userDataTotal.value = response.data.total || 0
    }
    await loadUserDataStats()
  } catch (error) {
    ElMessage.error('加载用户数据记录失败: ' + (error.message || '网络错误'))
  } finally {
    loadingUserData.value = false
  }
}

const loadUserDataStats = async () => {
  try {
    const response = await request.get('/api/admin/data-pool/stats')
    if (response.code === 200) {
      userDataStats.value = response.data
    }
  } catch (error) {
    console.error('加载用户数据统计失败:', error)
  }
}

const handleUserDataSelectionChange = (val) => {
  selectedUserDataIds.value = val.map(item => item.id)
}

const viewUserDataDetail = (row) => {
  userDataDetail.value = row
  userDataDetailVisible.value = true
}

const deleteUserData = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这条数据记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const response = await request.delete(`/api/admin/data-pool/records/${row.id}`)
    if (response.code === 200) {
      ElMessage.success('删除成功')
      loadUserDataRecords()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + (error.message || '网络错误'))
    }
  }
}

const batchDeleteUserData = async () => {
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedUserDataIds.value.length} 条数据记录吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const response = await request.delete('/api/admin/data-pool/records/batch', {
      data: selectedUserDataIds.value
    })
    if (response.code === 200) {
      ElMessage.success('批量删除成功')
      selectedUserDataIds.value = []
      loadUserDataRecords()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败: ' + (error.message || '网络错误'))
    }
  }
}

const getDataTypeLabel = (type) => {
  const labels = {
    form: '表单数据',
    chat_message: '聊天消息',
    user_action: '用户操作',
    editor_content: '编辑器内容',
    unknown: '未知'
  }
  return labels[type] || type
}

const getDataTypeTagType = (type) => {
  const types = {
    form: 'info',
    chat_message: 'success',
    user_action: 'warning',
    editor_content: 'primary'
  }
  return types[type] || 'default'
}

const getStatusLabel = (status) => {
  const labels = {
    PENDING_VALIDATION: '待验证',
    VALIDATED: '已验证',
    INVALID: '无效'
  }
  return labels[status] || status
}

const getStatusTagType = (status) => {
  const types = {
    PENDING_VALIDATION: 'warning',
    VALIDATED: 'success',
    INVALID: 'danger'
  }
  return types[status] || 'default'
}

const showUserDataAnalysis = async () => {
  loadingAnalysis.value = true
  userDataAnalysisVisible.value = true
  try {
    const response = await request.get('/api/admin/data-pool/analysis')
    if (response.code === 200) {
      analysisData.value = response.data
      
      const byType = response.data.byType || {}
      const total = response.data.totalRecords || 1
      typeDistribution.value = Object.entries(byType).map(([type, count]) => ({
        type: getDataTypeLabel(type),
        count,
        percentage: Math.round((count / total) * 100)
      }))
      
      const byTag = response.data.byTag || {}
      tagDistribution.value = Object.entries(byTag)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 10)
        .reduce((acc, [tag, count]) => {
          acc[tag] = count
          return acc
        }, {})
    }
  } catch (error) {
    ElMessage.error('获取数据分析失败: ' + (error.message || '网络错误'))
  } finally {
    loadingAnalysis.value = false
  }
}

const getTagSize = (count) => {
  if (count > 100) return 'large'
  if (count > 10) return 'medium'
  return 'small'
}

const showBackupManager = async () => {
  loadingBackup.value = true
  backupManagerVisible.value = true
  try {
    const [statsResponse, listResponse] = await Promise.all([
      request.get('/api/admin/backup/stats'),
      request.get('/api/admin/backup/list')
    ])
    if (statsResponse.code === 200) {
      backupStats.value = statsResponse.data
    }
    if (listResponse.code === 200) {
      backupList.value = listResponse.data.map(filename => ({
        filename,
        type: filename.startsWith('full_backup') ? 'FULL' : 'INCREMENTAL',
        date: filename.match(/(\d{8}_\d{6})/) ? filename.match(/(\d{8}_\d{6})/)[1].replace('_', ' ') : ''
      }))
    }
  } catch (error) {
    ElMessage.error('获取备份信息失败: ' + (error.message || '网络错误'))
  } finally {
    loadingBackup.value = false
  }
}

const performFullBackup = async () => {
  performingBackup.value = true
  try {
    const response = await request.post('/api/admin/backup/full')
    if (response.code === 200) {
      ElMessage.success('全量备份成功')
      await showBackupManager()
    } else {
      ElMessage.error(response.msg || '备份失败')
    }
  } catch (error) {
    ElMessage.error('备份失败: ' + (error.message || '网络错误'))
  } finally {
    performingBackup.value = false
  }
}

const performIncrementalBackup = async () => {
  performingBackup.value = true
  try {
    const response = await request.post('/api/admin/backup/incremental')
    if (response.code === 200) {
      ElMessage.success('增量备份成功')
      await showBackupManager()
    } else {
      ElMessage.error(response.msg || '备份失败')
    }
  } catch (error) {
    ElMessage.error('备份失败: ' + (error.message || '网络错误'))
  } finally {
    performingBackup.value = false
  }
}

const restoreBackup = async (filename) => {
  try {
    await ElMessageBox.confirm(`确定要从备份文件 "${filename}" 恢复数据吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const response = await request.post(`/api/admin/backup/restore/${filename}`)
    if (response.code === 200) {
      ElMessage.success(response.msg || '恢复成功')
      await showBackupManager()
      loadUserDataRecords()
    } else {
      ElMessage.error(response.msg || '恢复失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('恢复失败: ' + (error.message || '网络错误'))
    }
  }
}

const checkDuplicates = async () => {
  try {
    const response = await request.get('/api/admin/data-pool/duplicates/check')
    if (response.code === 200) {
      const data = response.data
      if (data.totalDuplicateRecords > 0) {
        ElMessage.warning(`发现 ${data.duplicateGroups} 组重复数据，共 ${data.totalDuplicateRecords} 条重复记录`)
      } else {
        ElMessage.success('未发现重复数据')
      }
    }
  } catch (error) {
    ElMessage.error('检查重复数据失败: ' + (error.message || '网络错误'))
  }
}

const removeDuplicates = async () => {
  try {
    await ElMessageBox.confirm('确定要执行数据去重操作吗？去重后将删除所有重复数据，仅保留每组中的第一条。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const response = await request.post('/api/admin/data-pool/duplicates/remove')
    if (response.code === 200) {
      const data = response.data
      ElMessage.success(`去重完成，共删除 ${data.deletedCount} 条重复记录`)
      loadUserDataRecords()
    } else {
      ElMessage.error(response.msg || '去重失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('去重失败: ' + (error.message || '网络错误'))
    }
  }
}
</script>

<style scoped>
.user-stats-row {
  display: flex;
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.08), rgba(124, 58, 237, 0.08));
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.free-icon {
  background: rgba(59, 130, 246, 0.15);
}

.pro-icon {
  background: rgba(34, 197, 94, 0.15);
}

.enterprise-icon {
  background: rgba(245, 158, 11, 0.15);
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: 13px;
  color: var(--text-tertiary);
}

.version-tabs {
  margin-bottom: 20px;
}

.upgrade-section {
  margin-top: 30px;
}

.upgrade-options {
  display: flex;
  gap: 24px;
}

.upgrade-card {
  flex: 1;
  text-align: center;
  border-radius: 16px;
  padding: 24px;
}

.pro-card {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.1), rgba(34, 197, 94, 0.05));
  border: 1px solid rgba(34, 197, 94, 0.2);
}

.pro-card h4 {
  color: #22c55e;
  margin-bottom: 12px;
}

.enterprise-card {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.1), rgba(245, 158, 11, 0.05));
  border: 1px solid rgba(245, 158, 11, 0.2);
}

.enterprise-card h4 {
  color: #f59e0b;
  margin-bottom: 12px;
}

.price {
  font-size: 36px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.price .period {
  font-size: 16px;
  font-weight: 400;
  color: var(--text-tertiary);
  margin-left: 4px;
}

.features-list {
  text-align: left;
  padding: 0;
  margin: 0 0 20px 0;
  list-style: none;
}

.features-list li {
  padding: 8px 0;
  font-size: 14px;
  color: var(--text-secondary);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.features-list li:last-child {
  border-bottom: none;
}

.payment-review-section {
  margin-top: 30px;
}

.review-card {
  border-radius: 12px;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  font-size: 14px;
  color: var(--text-secondary);
}

.admin-container {
  display: flex;
  min-height: 100vh;
  background: var(--bg-primary);
}

.admin-sidebar {
  width: 260px;
  background: var(--sidebar-bg);
  color: var(--text-primary);
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-lg);
  position: relative;
  z-index: 10;
}

.admin-sidebar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background:
    radial-gradient(ellipse 80% 50% at 50% 0%, rgba(79, 70, 229, 0.15), transparent),
    radial-gradient(ellipse 60% 40% at 80% 100%, rgba(124, 58, 237, 0.1), transparent);
  pointer-events: none;
}

.admin-header {
  padding: 24px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  position: relative;
  z-index: 1;
}

.admin-header h2 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 10px;
}

.admin-header h2::before {
  content: '';
  width: 8px;
  height: 8px;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  border-radius: 50%;
}

.admin-header p {
  margin: 0;
  font-size: 13px;
  color: var(--text-tertiary);
  padding-left: 18px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding-left: 18px;
}

.theme-toggle-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 2px solid var(--border-primary);
  background: var(--bg-secondary);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.25s ease;
}

.theme-toggle-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.3);
  transform: scale(1.05);
}

.theme-toggle-btn:active {
  transform: scale(0.95);
}

.admin-top-bar {
  position: fixed;
  top: 0;
  right: 0;
  z-index: 100;
  padding: 16px 24px;
}

.chandelier-toggle {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 12px 20px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(10px);
}

.chandelier-toggle:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
  transform: translateY(-2px);
}

.chandelier-base {
  position: relative;
  width: 40px;
  height: 50px;
}

.chandelier-chain {
  position: absolute;
  top: -16px;
  left: 50%;
  transform: translateX(-50%);
  width: 4px;
  height: 20px;
  background: linear-gradient(180deg, #fbbf24, #d97706);
  border-radius: 2px;
}

.chandelier-light {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 40px;
  height: 35px;
  background: linear-gradient(180deg, #fef3c7, #fbbf24);
  border-radius: 8px 8px 20px 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 20px rgba(251, 191, 36, 0.4),
              0 0 40px rgba(251, 191, 36, 0.2);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.chandelier-light.off {
  background: linear-gradient(180deg, #374151, #1f2937);
  box-shadow: 0 0 10px rgba(100, 116, 139, 0.2);
}

.light-bulb {
  width: 14px;
  height: 14px;
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 0 10px rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
}

.chandelier-light.off .light-bulb {
  background: #4b5563;
  box-shadow: none;
}

.chandelier-glow {
  position: absolute;
  top: -20px;
  left: 50%;
  transform: translateX(-50%);
  width: 80px;
  height: 80px;
  background: radial-gradient(circle, rgba(251, 191, 36, 0.3) 0%, transparent 70%);
  opacity: 0;
  transition: opacity 0.5s ease;
  pointer-events: none;
}

.chandelier-glow.lit {
  opacity: 1;
  animation: glow-pulse 2s ease-in-out infinite;
}

@keyframes glow-pulse {
  0%, 100% {
    opacity: 1;
    transform: translateX(-50%) scale(1);
  }
  50% {
    opacity: 0.7;
    transform: translateX(-50%) scale(1.1);
  }
}

.toggle-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  transition: color 0.3s ease;
}

.chandelier-toggle:hover .toggle-label {
  color: var(--text-primary);
}

.admin-menu {
  flex: 1;
  padding: 16px 12px;
  position: relative;
  z-index: 1;
}

.menu-item {
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.25s ease;
  border-radius: 10px;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  font-weight: 500;
  color: var(--sidebar-text);
  position: relative;
}

.menu-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 0;
  background: linear-gradient(180deg, #4f46e5, #7c3aed);
  border-radius: 0 3px 3px 0;
  transition: height 0.25s ease;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #f1f5f9;
}

.menu-item.active {
  background: linear-gradient(90deg, rgba(79, 70, 229, 0.2), rgba(79, 70, 229, 0.1));
  color: #f1f5f9;
}

.menu-item.active::before {
  height: 24px;
}

.admin-footer {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  position: relative;
  z-index: 1;
}

.admin-footer .el-button {
  width: 100%;
  justify-content: flex-start;
  padding-left: 16px;
  gap: 10px;
  background: rgba(239, 68, 68, 0.1);
  border: none;
  color: #fca5a5;
  transition: all 0.25s ease;
}

.admin-footer .el-button:hover {
  background: rgba(239, 68, 68, 0.2);
  color: #fecaca;
}

.admin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.admin-content {
  flex: 1;
  padding: 28px;
  overflow-y: auto;
}

.tab-content {
  background: var(--card-bg);
  border-radius: 16px;
  padding: 28px;
  box-shadow: var(--shadow-md);
  border: 1px solid var(--border-primary);
  margin-bottom: 24px;
}

.tab-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 28px;
  padding-bottom: 20px;
  border-bottom: 2px solid var(--border-primary);
  flex-wrap: wrap;
  gap: 16px;
}

.tab-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.3px;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.auto-save-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: rgba(79, 70, 229, 0.08);
  border-radius: 8px;
  border: 1px solid rgba(79, 70, 229, 0.2);
}

.auto-save-toggle span {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

.pagination {
  margin-top: 24px;
  display: flex;
  justify-content: center;
  padding-top: 16px;
  border-top: 1px solid var(--border-secondary);
}

.datapool-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--border-secondary);
  gap: 16px;
  flex-wrap: wrap;
}

.datapool-footer .pagination.datapool-pagination {
  margin-top: 0;
  padding-top: 0;
  border-top: none;
  justify-content: flex-start;
}

.footer-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.filter-section {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  padding: 18px 24px;
  background: var(--bg-tertiary);
  border-radius: 12px;
  border: 1px solid var(--border-primary);
  flex-wrap: wrap;
  gap: 12px;
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  background: var(--card-bg);
  border-radius: 12px;
  border: 1px solid var(--border-primary);
  box-shadow: var(--shadow-sm);
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  font-size: 24px;
}

.stat-icon.data-icon {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: white;
}

.stat-icon.text-icon {
  background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%);
  color: white;
}

.stat-icon.struct-icon {
  background: linear-gradient(135deg, #10b981 0%, #34d399 100%);
  color: white;
}

.stat-icon.media-icon {
  background: linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%);
  color: white;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 4px;
}

/* 批量操作栏 */
.batch-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: var(--color-error-light);
  border-radius: 8px;
  border: 1px solid var(--color-error);
  margin-bottom: 16px;
}

.batch-actions span {
  color: var(--color-error);
  font-weight: 500;
}

.capacity-monitor {
  margin-bottom: 20px;
  padding: 20px;
  background: var(--card-bg);
  border-radius: 12px;
  border: 1px solid var(--border-primary);
  box-shadow: var(--shadow-sm);
}

.capacity-info {
  margin-top: 10px;
  text-align: right;
  color: var(--text-secondary);
  font-size: 14px;
}

.session-list {
  margin-bottom: 20px;
}

.session-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: var(--card-bg);
  border: 1px solid var(--border-primary);
  border-radius: 8px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.3s;
}

.session-item:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-md);
}

.session-info {
  flex: 1;
}

.session-title {
  font-weight: bold;
  color: var(--text-primary);
  margin-bottom: 5px;
}

.session-id {
  font-size: 12px;
  color: var(--text-muted);
}

.session-arrow {
  color: var(--text-muted);
  font-size: 20px;
}

.chat-view {
  background: var(--card-bg);
  border-radius: 8px;
  margin-bottom: 20px;
}

.chat-view .chat-header {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px 20px;
  border-bottom: 1px solid var(--border-primary);
  font-weight: bold;
  color: var(--text-primary);
}

.chat-messages {
  max-height: 500px;
  overflow-y: auto;
  padding: 20px;
}

.chat-message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.chat-message.user {
  flex-direction: row-reverse;
}

.chat-message .message-avatar img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
}

.chat-message .message-content {
  max-width: 70%;
}

.chat-message .message-text {
  background: var(--bg-tertiary);
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.5;
  word-break: break-word;
  color: var(--text-primary);
}

.chat-message.user .message-text {
  background: var(--color-primary);
  color: var(--text-inverse);
}

.chat-message .message-time {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 5px;
}

.chat-message.user .message-time {
  text-align: right;
}

.stats-tab {
  padding: 0;
}

.stats-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  background: var(--gradient-primary);
  border-radius: 8px 8px 0 0;
  color: white;
}

.header-left h3 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.header-left .subtitle {
  margin: 6px 0 0 0;
  font-size: 14px;
  opacity: 0.9;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.live-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  font-size: 13px;
}

.live-dot {
  width: 8px;
  height: 8px;
  background: #10b981;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.stats-content {
  padding: 24px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  position: relative;
  background: var(--card-bg);
  border: 1px solid var(--border-primary);
  border-radius: 16px;
  padding: 24px;
  box-shadow: var(--shadow-sm);
  transition: all 0.3s ease;
  overflow: hidden;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
}

.card-glow {
  position: absolute;
  top: -50%;
  right: -50%;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  opacity: 0.1;
}

.primary-card .card-glow {
  background: radial-gradient(circle, #6366f1 0%, transparent 70%);
}

.success-card .card-glow {
  background: radial-gradient(circle, #22c55e 0%, transparent 70%);
}

.warning-card .card-glow {
  background: radial-gradient(circle, #f59e0b 0%, transparent 70%);
}

.info-card .card-glow {
  background: radial-gradient(circle, #3b82f6 0%, transparent 70%);
}

.stat-icon-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.stat-icon-wrapper.primary {
  background: linear-gradient(135deg, #818cf8 0%, #6366f1 100%);
}

.stat-icon-wrapper.success {
  background: linear-gradient(135deg, #86efac 0%, #22c55e 100%);
}

.stat-icon-wrapper.warning {
  background: linear-gradient(135deg, #fcd34d 0%, #f59e0b 100%);
}

.stat-icon-wrapper.info {
  background: linear-gradient(135deg, #93c5fd 0%, #3b82f6 100%);
}

.stat-icon {
  font-size: 24px;
  color: white;
}

.stat-content {
  position: relative;
  z-index: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: var(--text-tertiary);
  margin-bottom: 8px;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}

.stat-trend.positive {
  color: #10b981;
}

.stat-trend.negative {
  color: #ef4444;
}

.real-time-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.real-time-card {
  background: var(--card-bg);
  border-radius: 12px;
  padding: 20px;
  color: var(--text-primary);
  text-align: center;
  position: relative;
  overflow: hidden;
}

.real-time-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #6366f1, #8b5cf6, #6366f1);
  background-size: 200% 100%;
  animation: shimmer 2s infinite;
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.real-time-value {
  font-size: 36px;
  font-weight: 700;
  margin-bottom: 4px;
  font-family: 'Courier New', monospace;
}

.real-time-label {
  font-size: 13px;
  color: var(--text-tertiary);
}

.charts-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

.chart-card {
  background: var(--card-bg);
  border: 1px solid var(--border-primary);
  border-radius: 16px;
  padding: 24px;
  box-shadow: var(--shadow-sm);
}

.chart-card.large-chart {
  grid-column: span 2;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.chart-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.chart-tabs {
  display: flex;
  gap: 8px;
}

.tab-btn {
  padding: 6px 16px;
  border: 1px solid var(--border-primary);
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  transition: all 0.3s;
}

.tab-btn:hover {
  background: var(--bg-hover);
  border-color: var(--color-primary);
}

.tab-btn.active {
  background: var(--color-primary);
  color: white;
  border-color: var(--color-primary);
}

.chart-wrapper {
  height: 300px;
}

.small-chart-wrapper {
  height: 250px;
}

.today-stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.today-stat-card {
  background: var(--card-bg);
  border: 1px solid var(--border-primary);
  border-radius: 12px;
  padding: 20px;
  box-shadow: var(--shadow-sm);
}

.today-stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.today-stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.today-stat-icon.users {
  background: rgba(59, 130, 246, 0.15);
  color: #3b82f6;
}

.today-stat-icon.messages {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

.today-stat-icon.friends {
  background: rgba(245, 158, 11, 0.15);
  color: #f59e0b;
}

.today-stat-icon.characters {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.today-stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.today-stat-label {
  font-size: 13px;
  color: var(--text-tertiary);
}

.profile-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.profile-card {
  background: var(--card-bg);
  border: 1px solid var(--border-primary);
  border-radius: 16px;
  padding: 28px;
  box-shadow: var(--shadow-sm);
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-secondary);
  margin-bottom: 20px;
}

.profile-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--gradient-primary);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-icon {
  font-size: 40px;
}

.profile-info h4 {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

.profile-role {
  margin: 0;
  font-size: 14px;
  color: var(--color-primary);
}

.profile-details {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-secondary);
}

.detail-row:last-child {
  border-bottom: none;
}

.detail-label {
  font-size: 14px;
  color: var(--text-tertiary);
}

.detail-value {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-secondary);
}

.password-form {
  max-width: 500px;
}

@media (max-width: 768px) {
  .profile-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .real-time-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .charts-row {
    grid-template-columns: 1fr;
  }
  
  .chart-card.large-chart {
    grid-column: span 1;
  }
  
  .today-stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .stats-header {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }
}

@media (max-width: 480px) {
  .real-time-grid {
    grid-template-columns: 1fr;
  }

  .today-stats-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1024px) {
  .admin-sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 1000;
    transition: transform 0.3s ease;
  }

  .admin-sidebar.collapsed {
    transform: translateX(-100%);
  }

  .sidebar-overlay {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: 999;
  }

  .admin-main {
    flex: 1;
    margin-left: 0;
  }

  .mobile-header {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 16px 20px;
    background: var(--card-bg);
    border-bottom: 1px solid var(--border-primary);
  }

  .menu-toggle {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    width: 40px;
    height: 40px;
    gap: 5px;
    background: var(--bg-tertiary);
    border: 1px solid var(--border-primary);
    border-radius: 8px;
    cursor: pointer;
    padding: 8px;
  }

  .menu-toggle span {
    display: block;
    width: 20px;
    height: 2px;
    background: var(--text-secondary);
    border-radius: 2px;
    transition: all 0.3s ease;
  }

  .menu-toggle:hover {
    background: var(--bg-hover);
  }

  .mobile-header h1 {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0;
  }

  .admin-content {
    padding: 16px;
  }

  .tab-content {
    padding: 16px;
  }

  .tab-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .real-time-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .charts-row {
    grid-template-columns: 1fr;
  }

  .chart-card.large-chart {
    grid-column: span 1;
  }

  .today-stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .stats-header {
    flex-direction: column;
    gap: 16px;
    text-align: center;
    padding: 16px;
  }

  .filter-section {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .filter-section .el-select {
    width: 100% !important;
    margin-right: 0 !important;
  }

  .session-list {
    padding: 0;
  }

  .session-item {
    padding: 12px 16px;
  }

  .chat-view .chat-header {
    flex-wrap: wrap;
    gap: 10px;
  }

  .chat-messages {
    max-height: 400px;
  }
}

/* 日志相关样式 */
.logs-container {
  height: 500px;
  display: flex;
  flex-direction: column;
}

.logs-filter {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 12px;
}

.logs-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.log-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  margin-bottom: 8px;
  background: var(--card-bg);
  border-radius: 6px;
  border-left: 4px solid var(--border-primary);
  font-size: 13px;
}

.log-item:hover {
  background: var(--bg-hover);
}

.log-info {
  border-left-color: var(--color-primary);
}

.log-success {
  border-left-color: var(--color-success);
}

.log-warning {
  border-left-color: var(--color-warning);
}

.log-error {
  border-left-color: var(--color-error);
}

.log-timestamp {
  color: var(--text-muted);
  font-size: 12px;
  min-width: 160px;
}

.log-level {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  min-width: 50px;
  text-align: center;
}

.level-info {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.level-success {
  background: var(--color-success-light);
  color: var(--color-success);
}

.level-warning {
  background: var(--color-warning-light);
  color: var(--color-warning);
}

.level-error {
  background: var(--color-error-light);
  color: var(--color-error);
}

.log-module {
  min-width: 60px;
  color: var(--text-secondary);
  font-weight: 500;
}

.log-message {
  flex: 1;
  color: var(--text-primary);
}

.log-user {
  min-width: 80px;
  color: var(--text-muted);
  font-size: 12px;
  text-align: right;
}

.empty-logs {
  text-align: center;
  color: var(--text-muted);
  padding: 40px;
}

.drop-zone {
  border: 2px dashed var(--border-color);
  border-radius: 12px;
  padding: 40px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: var(--bg-secondary);
}

.drop-zone:hover {
  border-color: #6366f1;
  background: rgba(99, 102, 241, 0.05);
}

.drop-zone-active {
  border-color: #6366f1;
  background: rgba(99, 102, 241, 0.1);
  transform: scale(1.01);
}

.drop-zone-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: var(--text-secondary);
}

.drop-zone-content svg {
  color: #6366f1;
}

.drop-zone-content p {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

.drop-zone-hint {
  font-size: 13px !important;
  color: var(--text-muted) !important;
  font-weight: 400 !important;
}

.drop-zone-input {
  display: none;
}

.upload-file-list {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.upload-file-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--bg-secondary);
  border-radius: 8px;
  border: 1px solid var(--border-color);
}

.file-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.file-icon.icon-image {
  background: rgba(34, 197, 94, 0.1);
  color: #22c55e;
}

.file-icon.icon-audio {
  background: rgba(139, 92, 246, 0.1);
  color: #8b5cf6;
}

.file-icon.icon-video {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.file-icon.icon-file {
  background: rgba(99, 102, 241, 0.1);
  color: #6366f1;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

.file-preview {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
}

.file-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.file-remove {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.file-remove:hover {
  background: rgba(239, 68, 68, 0.2);
  transform: scale(1.1);
}

.upload-progress-container {
  margin-top: 20px;
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: 8px;
}

.upload-progress-text {
  text-align: center;
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 8px;
}

@media (min-width: 1025px) {
  .mobile-header {
    display: none;
  }
}

/* AI 系统管理面板样式 */
.ai-stats-row {
  margin-bottom: 16px;
}

.ai-stats-row .el-col {
  margin-bottom: 12px;
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary, #909399);
  margin-bottom: 6px;
}

.stat-value {
  font-size: 26px;
  font-weight: 600;
  color: var(--text-primary, #303133);
}

.stat-empty {
  font-size: 13px;
  color: var(--text-secondary, #909399);
}

.stat-top-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-top-item {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.stat-top-name {
  color: var(--text-primary, #303133);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 70%;
}

.stat-top-calls {
  color: var(--text-secondary, #909399);
}

.ai-section-card {
  margin-bottom: 16px;
}

.ai-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ollama-status-desc {
  margin-bottom: 8px;
}

.pull-model-form {
  margin-bottom: 8px;
}
.dashboard-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin: 16px 0;
}
.dash-card {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.dash-card .dash-label {
  color: #6b7280;
  font-size: 13px;
  margin-bottom: 8px;
}
.dash-card .dash-value {
  font-size: 30px;
  font-weight: 700;
  color: #1f2937;
}
.dashboard-charts {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}
.dash-chart-card {
  background: #fff;
  border-radius: 10px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.dash-chart {
  width: 100%;
  height: 280px;
}
@media (max-width: 768px) {
  .dashboard-cards { grid-template-columns: 1fr; }
}
</style>