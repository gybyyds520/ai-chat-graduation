<template>
  <div class="friend-container">
    <div class="sidebar">
      <div class="sidebar-header">
        <h3>好友</h3>
      </div>
      
      <div class="nav-tabs">
        <div 
          :class="['tab', { active: activeTab === 'friends' }]"
          @click="activeTab = 'friends'"
        >
          我的好友
        </div>
        <div 
          :class="['tab', { active: activeTab === 'groups' }]"
          @click="activeTab = 'groups'; loadGroups()"
        >
          好友分组
        </div>
        <div 
          :class="['tab', { active: activeTab === 'requests' }]"
          @click="activeTab = 'requests'; loadRequests()"
        >
          好友请求
          <span v-if="pendingRequests.length > 0" class="badge">{{ pendingRequests.length }}</span>
        </div>
        <div 
          :class="['tab', { active: activeTab === 'search' }]"
          @click="activeTab = 'search'"
        >
          添加好友
        </div>
      </div>

      <div class="content-area">
        <!-- 我的好友 -->
        <div v-if="activeTab === 'friends'" class="friend-list">
          <div v-if="friends.length === 0" class="empty-state">
            暂无好友
          </div>
          <div 
            v-for="friend in friends" 
            :key="getFriendId(friend)"
            class="friend-item"
            @click="startChatWithFriend(friend)"
          >
            <div class="friend-avatar" :class="{ online: isOnline(friend.lastActiveTime) }">
              <img :src="getFriendAvatar(friend)" alt="">
              <span class="online-dot" v-if="isOnline(friend.lastActiveTime)"></span>
            </div>
            <div class="friend-info">
              <div class="friend-name">{{ displayFriendName(friend) }}</div>
              <div v-if="getFriendRemark(friend)" class="friend-remark">备注：{{ getFriendRemark(friend) }}</div>
              <div class="friend-status-text">{{ isOnline(friend.lastActiveTime) ? '在线' : '离线' }}</div>
            </div>
            <el-button
              size="small"
              @click.stop="editRemark(friend)"
            >
              备注
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click.stop="removeFriend(friend)"
            >
              删除
            </el-button>
          </div>
        </div>

        <!-- 好友分组 -->
        <div v-if="activeTab === 'groups'" class="group-list">
          <div class="group-actions">
            <el-button type="primary" size="small" @click="showCreateGroupModal = true">
              创建分组
            </el-button>
          </div>
          <div v-if="groups.length === 0" class="empty-state">
            暂无分组
          </div>
          <div 
            v-for="group in groups" 
            :key="group.id"
            class="group-item"
          >
            <div class="group-header" @click="toggleGroupMembers(group.id)">
              <span class="group-name">{{ group.name }}</span>
              <span class="group-count">{{ getGroupMemberCount(group.id) }} 人</span>
              <span class="group-toggle">{{ expandedGroups.includes(group.id) ? '▼' : '▶' }}</span>
            </div>
            <div v-if="expandedGroups.includes(group.id)" class="group-members">
              <div 
                v-for="member in getGroupMembers(group.id)" 
                :key="member.friendId"
                class="group-member"
              >
                <img :src="getUserAvatar(member.friendId)" class="member-avatar">
                <div class="member-info">
                  <span class="member-name">{{ member.remark || '好友' }}</span>
                </div>
                <el-button size="small" @click="removeMember(group.id, member.friendId)">移除</el-button>
              </div>
              <div v-if="getGroupMembers(group.id).length === 0" class="empty-members">
                暂无成员
              </div>
            </div>
            <div class="group-actions-row">
              <el-button size="small" @click="editGroupName(group)">编辑</el-button>
              <el-button size="small" type="danger" @click="deleteGroup(group.id)">删除</el-button>
              <el-button size="small" @click="addMemberToGroup(group)">添加成员</el-button>
            </div>
          </div>
        </div>

        <!-- 好友请求 -->
        <div v-if="activeTab === 'requests'" class="request-list">
          <div v-if="pendingRequests.length === 0" class="empty-state">
            暂无好友请求
          </div>
          <div 
            v-for="request in pendingRequests" 
            :key="request.id"
            class="request-item"
          >
            <div class="request-avatar">
              <img :src="getUserAvatar(request.userId)" alt="">
            </div>
            <div class="request-info">
              <div class="request-name">{{ request.username }}</div>
              <div class="request-time">请求时间: {{ formatTime(request.createTime) }}</div>
            </div>
            <div class="request-actions">
              <el-button type="primary" size="small" @click="acceptRequest(request)">
                接受
              </el-button>
              <el-button size="small" @click="rejectRequest(request)">
                拒绝
              </el-button>
            </div>
          </div>
        </div>

        <!-- 添加好友 -->
        <div v-if="activeTab === 'search'" class="search-area">
          <div class="search-box">
            <el-input 
              v-model="searchUsername" 
              placeholder="输入用户名搜索"
              @keyup.enter="searchUser"
            >
              <template #append>
                <el-button @click="searchUser">搜索</el-button>
              </template>
            </el-input>
          </div>

          <div v-if="searchResult" class="search-result">
            <div class="result-item">
              <div class="result-avatar">
                <img :src="getUserAvatar(searchResult.id)" alt="">
              </div>
              <div class="result-info">
                <div class="result-name">{{ searchResult.username }}</div>
              </div>
              <div class="result-action">
                <el-button 
                  v-if="searchResult.status === 'none'" 
                  type="primary" 
                  size="small"
                  @click="sendFriendRequest(searchResult.id)"
                >
                  添加好友
                </el-button>
                <span v-else-if="searchResult.status === 'pending'" class="status-text">
                  等待对方接受
                </span>
                <span v-else-if="searchResult.status === 'requested'" class="status-text">
                  对方已发送请求
                </span>
                <span v-else-if="searchResult.status === 'friends'" class="status-text">
                  已是好友
                </span>
                <span v-else-if="searchResult.status === 'self'" class="status-text">
                  就是你自己
                </span>
              </div>
            </div>
          </div>

          <div v-if="searchError" class="search-error">
            {{ searchError }}
          </div>
        </div>
      </div>

      <div class="nav-buttons">
        <el-button type="primary" @click="goToChat" class="nav-btn">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
          AI聊天
        </el-button>
        <el-button @click="goToSocialFeed" class="nav-btn">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M18 2h-3a5 5 0 0 0-5 5v3H7v4h3v8h4v-8h3l1-4h-4V7a1 1 0 0 1 1-1h3z"/>
          </svg>
          社交动态
        </el-button>
        <el-button @click="goToNotifications" class="nav-btn">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
            <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
          </svg>
          通知
        </el-button>
        <el-button @click="handleLogout" class="nav-btn logout">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
            <polyline points="16 17 21 12 16 7"/>
            <line x1="21" y1="12" x2="9" y2="12"/>
          </svg>
          退出登录
        </el-button>
      </div>
    </div>

    <!-- 创建分组模态框 -->
    <div v-if="showCreateGroupModal" class="modal-overlay" @click="showCreateGroupModal = false">
      <div class="modal-content" @click.stop>
        <h2>创建分组</h2>
        <el-input v-model="newGroupName" placeholder="分组名称" @keyup.enter="createGroup"></el-input>
        <div class="modal-actions">
          <el-button @click="showCreateGroupModal = false">取消</el-button>
          <el-button type="primary" @click="createGroup">创建</el-button>
        </div>
      </div>
    </div>

    <!-- 添加成员模态框 -->
    <div v-if="showAddMemberModal" class="modal-overlay" @click="showAddMemberModal = false">
      <div class="modal-content" @click.stop>
        <h2>添加成员到 {{ currentGroup?.name }}</h2>
        <el-input v-model="searchMemberName" placeholder="搜索好友" @keyup.enter="searchMembers"></el-input>
        <div v-if="availableMembers.length > 0" class="member-select-list">
          <div 
            v-for="member in availableMembers" 
            :key="member.id"
            class="member-select-item"
          >
            <img :src="getUserAvatar(member.id)" class="select-avatar">
            <span class="select-name">{{ member.username }}</span>
            <el-button size="small" @click="confirmAddMember(member.id)">添加</el-button>
          </div>
        </div>
        <div class="modal-actions">
          <el-button @click="showAddMemberModal = false">取消</el-button>
        </div>
      </div>
    </div>

    <div class="main-content">
      <div class="welcome-section">
        <h2>好友系统</h2>
        <p>搜索用户、添加好友、管理好友请求</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { generateAvatarUrl } from '@/utils/localAvatar'
import { isOnline, displayFriendName, recommendReason } from '@/utils/social'

const router = useRouter()

// 修复：登录后 token 已改为 JWT 字符串，parseInt 会得到 NaN，导致
// /api/friend/list?userId=NaN 等接口报 400。应从 user 对象读取 id。
const userId = ref(JSON.parse(localStorage.getItem('user') || '{}').id)
const activeTab = ref('friends')
const friends = ref([])
const pendingRequests = ref([])
const searchUsername = ref('')
const searchResult = ref(null)
const searchError = ref('')

const groups = ref([])
const groupMembers = ref([])
const expandedGroups = ref([])
const showCreateGroupModal = ref(false)
const showAddMemberModal = ref(false)
const newGroupName = ref('')
const currentGroup = ref(null)
const searchMemberName = ref('')
const availableMembers = ref([])

const loadFriends = async () => {
  try {
    const response = await request.get(`/api/friend/list?userId=${userId.value}`)
    if (response.code === 200) {
      friends.value = response.data
    }
  } catch (error) {
    console.error('加载好友列表失败:', error)
  }
}

const loadRequests = async () => {
  try {
    const response = await request.get(`/api/friend/requests?userId=${userId.value}`)
    if (response.code === 200) {
      pendingRequests.value = response.data
    }
  } catch (error) {
    console.error('加载好友请求失败:', error)
  }
}

const searchUser = async () => {
  if (!searchUsername.value.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }

  searchError.value = ''
  searchResult.value = null

  try {
    const response = await request.get(`/api/friend/search?username=${encodeURIComponent(searchUsername.value)}`)
    if (response.code === 200) {
      searchResult.value = response.data
      
      if (searchResult.value.id === userId.value) {
        searchResult.value.status = 'self'
      } else {
        const statusResponse = await request.get(`/api/friend/status?userId=${userId.value}&targetId=${searchResult.value.id}`)
        if (statusResponse.code === 200) {
          const status = statusResponse.data
          if (status === 'friend') {
            searchResult.value.status = 'friends'
          } else if (status === 'pending_received') {
            searchResult.value.status = 'requested'
          } else if (status === 'pending_sent') {
            searchResult.value.status = 'pending'
          } else {
            searchResult.value.status = 'none'
          }
        }
      }
    }
  } catch (error) {
    searchError.value = error.message || '用户不存在'
  }
}

const sendFriendRequest = async (friendId) => {
  try {
    const response = await request.post('/api/friend/request', {
      userId: userId.value,
      friendId: friendId
    })
    if (response.code === 200) {
      ElMessage.success('好友请求已发送')
      if (searchResult.value) {
        searchResult.value.status = 'pending'
      }
    } else {
      ElMessage.error(response.message || '发送失败')
    }
  } catch (error) {
    ElMessage.error('发送失败')
  }
}

const acceptRequest = async (requestItem) => {
  try {
    const response = await request.post('/api/friend/accept', {
      userId: userId.value,
      requesterId: requestItem.userId
    })
    if (response.code === 200) {
      ElMessage.success('已添加好友')
      pendingRequests.value = pendingRequests.value.filter(r => r.id !== requestItem.id)
      loadFriends()
    } else {
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const rejectRequest = async (requestItem) => {
  try {
    const response = await request.post('/api/friend/reject', {
      userId: userId.value,
      requesterId: requestItem.userId
    })
    if (response.code === 200) {
      ElMessage.success('已拒绝')
      pendingRequests.value = pendingRequests.value.filter(r => r.id !== requestItem.id)
    } else {
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const removeFriend = async (friendItem) => {
  try {
    await ElMessageBox.confirm('确定要删除该好友吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const friendId = getFriendId(friendItem)
    const response = await request.post('/api/friend/remove', {
      userId: userId.value,
      friendId: friendId
    })
    if (response.code === 200) {
      ElMessage.success('已删除好友')
      loadFriends()
    } else {
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const getFriendId = (friend) => {
  return friend.userId === userId.value ? friend.friendId : friend.userId
}

const getFriendName = (friend) => {
  return friend.username || '好友'
}

const getFriendAvatar = (friend) => {
  const friendId = getFriendId(friend)
  return generateAvatarUrl(friendId.toString())
}

const getUserAvatar = (uid) => {
  return generateAvatarUrl(uid.toString())
}

const getFriendRemark = (friend) => {
  return friend.remark || ''
}

// 编辑好友备注
const editRemark = async (friend) => {
  const friendId = getFriendId(friend)
  try {
    const { value } = await ElMessageBox.prompt('请输入好友备注名', '设置备注', {
      inputValue: friend.remark || ''
    })
    if (value !== null && value !== undefined) {
      await request.post('/api/friend/remark', {
        userId: userId.value,
        friendId: friendId,
        remark: value.trim()
      })
      friend.remark = value.trim()
      ElMessage.success('备注已更新')
    }
  } catch (e) {
    if (e !== 'cancel') {
      // 接口未就绪时仅前端本地更新备注展示
      if (e && e.message) { /* 忽略 */ }
    }
  }
}

const loadGroups = async () => {
  try {
    const response = await request.get(`/api/friend/group/list?userId=${userId.value}`)
    if (response.code === 200) {
      groups.value = response.data
    }
  } catch (error) {
    console.error('加载分组失败:', error)
  }
}

const toggleGroupMembers = (groupId) => {
  const index = expandedGroups.value.indexOf(groupId)
  if (index > -1) {
    expandedGroups.value.splice(index, 1)
  } else {
    expandedGroups.value.push(groupId)
    loadGroupMembers(groupId)
  }
}

const loadGroupMembers = async (groupId) => {
  try {
    const response = await request.get(`/api/friend/group/members/${groupId}`)
    if (response.code === 200) {
      groupMembers.value = [...groupMembers.value.filter(m => m.groupId !== groupId), ...response.data]
    }
  } catch (error) {
    console.error('加载分组成员失败:', error)
  }
}

const getGroupMembers = (groupId) => {
  return groupMembers.value.filter(m => m.groupId === groupId)
}

const getGroupMemberCount = (groupId) => {
  return getGroupMembers(groupId).length
}

const createGroup = async () => {
  if (!newGroupName.value.trim()) {
    ElMessage.warning('请输入分组名称')
    return
  }
  try {
    const response = await request.post('/api/friend/group/create', {
      userId: userId.value,
      name: newGroupName.value
    })
    if (response.code === 200) {
      ElMessage.success('分组创建成功')
      showCreateGroupModal.value = false
      newGroupName.value = ''
      loadGroups()
    }
  } catch (error) {
    ElMessage.error('创建失败')
  }
}

const editGroupName = async (group) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入新的分组名称', '编辑分组', {
      defaultValue: group.name
    })
    if (value && value.trim()) {
      await request.put(`/api/friend/group/${group.id}`, { name: value.trim() })
      ElMessage.success('修改成功')
      loadGroups()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('修改失败')
    }
  }
}

const deleteGroup = async (groupId) => {
  try {
    await ElMessageBox.confirm('确定删除该分组吗？', '提示', {
      type: 'warning'
    })
    await request.delete(`/api/friend/group/${groupId}`)
    ElMessage.success('删除成功')
    loadGroups()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const addMemberToGroup = (group) => {
  currentGroup.value = group
  showAddMemberModal.value = true
  searchMemberName.value = ''
  availableMembers.value = []
}

const searchMembers = async () => {
  if (!searchMemberName.value.trim()) {
    availableMembers.value = friends.value.map(f => ({
      id: getFriendId(f),
      username: getFriendName(f)
    }))
    return
  }
  const keyword = searchMemberName.value.toLowerCase()
  availableMembers.value = friends.value
    .filter(f => getFriendName(f).toLowerCase().includes(keyword))
    .map(f => ({
      id: getFriendId(f),
      username: getFriendName(f)
    }))
}

const confirmAddMember = async (friendId) => {
  try {
    await request.post('/api/friend/group/member/add', {
      groupId: currentGroup.value.id,
      friendId: friendId
    })
    ElMessage.success('添加成功')
    loadGroupMembers(currentGroup.value.id)
  } catch (error) {
    ElMessage.error('添加失败')
  }
}

const removeMember = async (groupId, friendId) => {
  try {
    await request.post('/api/friend/group/member/remove', {
      groupId: groupId,
      friendId: friendId
    })
    ElMessage.success('移除成功')
    groupMembers.value = groupMembers.value.filter(m => !(m.groupId === groupId && m.friendId === friendId))
  } catch (error) {
    ElMessage.error('移除失败')
  }
}

const goToSocialFeed = () => {
  router.push('/social-feed')
}

const goToNotifications = () => {
  router.push('/notifications')
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN')
}

const startChatWithFriend = (friend) => {
  const friendId = getFriendId(friend)
  router.push({
    path: '/friend-chat',
    query: { friendId: friendId }
  })
}

const goToChat = () => {
  router.push('/chat')
}

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}

onMounted(() => {
  loadFriends()
})
</script>

<style scoped>
.friend-container {
  display: flex;
  height: 100vh;
  background: var(--bg-primary);
}

.sidebar {
  width: 380px;
  background: var(--card-bg);
  border-right: 1px solid var(--border-primary);
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid var(--border-primary);
}

.sidebar-header h3 {
  margin: 0;
  font-size: 18px;
  color: var(--text-primary);
}

.nav-tabs {
  display: flex;
  border-bottom: 1px solid var(--border-primary);
}

.tab {
  flex: 1;
  padding: 15px 10px;
  text-align: center;
  cursor: pointer;
  font-size: 14px;
  color: var(--text-secondary);
  position: relative;
  transition: all 0.2s;
}

.tab:hover {
  background: var(--bg-hover);
}

.tab.active {
  color: var(--color-primary);
  border-bottom: 2px solid var(--color-primary);
  font-weight: 500;
}

.badge {
  position: absolute;
  top: 8px;
  right: 20px;
  background: var(--color-error);
  color: var(--text-inverse);
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 18px;
}

.content-area {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: var(--text-muted);
  font-size: 14px;
}

.friend-item,
.request-item,
.result-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 8px;
  transition: background 0.2s;
}

.friend-item:hover {
  background: var(--bg-hover);
  cursor: pointer;
}

.friend-avatar,
.request-avatar,
.result-avatar {
  margin-right: 12px;
}

.friend-avatar img,
.request-avatar img,
.result-avatar img {
  width: 44px;
  height: 44px;
  border-radius: 50%;
}

.friend-info,
.request-info,
.result-info {
  flex: 1;
}

.friend-name,
.request-name,
.result-name {
  font-size: 15px;
  color: var(--text-primary);
  font-weight: 500;
}

.request-time {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}

.request-actions {
  display: flex;
  gap: 8px;
}

.status-text {
  font-size: 13px;
  color: var(--text-muted);
}

.search-area {
  padding: 10px 0;
}

.search-box {
  margin-bottom: 20px;
}

.search-result {
  margin-top: 20px;
}

.search-error {
  margin-top: 20px;
  text-align: center;
  color: var(--color-error);
  font-size: 14px;
}

.nav-buttons {
  padding: 15px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.nav-btn {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding-left: 12px;
  gap: 10px;
  background: rgba(255, 255, 255, 0.05);
  border: none;
  color: var(--text-secondary);
  transition: all 0.2s ease;
  border-radius: var(--radius-md);
  font-size: var(--font-sm);
}

.nav-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: translateX(2px);
}

.nav-btn:active {
  transform: translateX(0);
}

.nav-btn.logout {
  background: rgba(239, 68, 68, 0.1);
  color: #fca5a5;
}

.nav-btn.logout:hover {
  background: rgba(239, 68, 68, 0.2);
}

@media (max-width: 768px) {
  .sidebar {
    width: 100%;
  }
  
  .nav-btn {
    padding: 12px;
    font-size: 13px;
  }
  
  .nav-btn svg {
    width: 16px;
    height: 16px;
  }
}

.main-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.welcome-section {
  text-align: center;
  color: var(--text-muted);
}

.welcome-section h2 {
  font-size: 24px;
  color: var(--text-primary);
  margin-bottom: 10px;
}

.welcome-section p {
  font-size: 14px;
}

.friend-remark {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}

.group-list {
  padding: 10px 0;
}

.group-actions {
  margin-bottom: 16px;
}

.group-item {
  background: var(--bg-secondary);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 10px;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.group-name {
  font-weight: 500;
  color: var(--text-primary);
}

.group-count {
  font-size: 12px;
  color: var(--text-muted);
}

.group-toggle {
  margin-left: auto;
  font-size: 12px;
  color: var(--text-muted);
}

.group-members {
  margin-top: 12px;
  padding-left: 16px;
  border-left: 2px solid var(--border-primary);
}

.group-member {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
}

.member-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
}

.member-info {
  flex: 1;
}

.member-name {
  font-size: 14px;
  color: var(--text-primary);
}

.empty-members {
  font-size: 12px;
  color: var(--text-muted);
  padding: 10px 0;
}

.group-actions-row {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-primary);
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: var(--card-bg);
  border-radius: 12px;
  padding: 24px;
  width: 90%;
  max-width: 400px;
}

.modal-content h2 {
  margin-bottom: 20px;
  font-size: 18px;
  color: var(--text-primary);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.member-select-list {
  max-height: 300px;
  overflow-y: auto;
  margin-top: 16px;
}

.member-select-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border-radius: 8px;
  margin-bottom: 8px;
}

.member-select-item:hover {
  background: var(--bg-hover);
}

.select-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
}

.select-name {
  flex: 1;
  font-size: 14px;
  color: var(--text-primary);
}
.friend-avatar { position: relative; }
.friend-avatar .online-dot { position: absolute; bottom: 0; right: 0; width: 10px; height: 10px; background: #22c55e; border-radius: 50%; border: 2px solid var(--card-bg, #fff); }
.friend-status-text { font-size: 11px; color: var(--text-muted); }
</style>
