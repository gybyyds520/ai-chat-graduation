import { createRouter, createWebHistory } from 'vue-router'
// 首屏必需路由静态加载：落地页、登录页、主聊天页（用户登录后直接进入，不期望白屏等待分包）
import LandingPage from '@/views/LandingPage.vue'
import LoginView from '@/views/LoginView.vue'
import ChatView from '@/views/ChatView.vue'

// 其余路由统一改为动态 import() 懒加载：
// 首屏只下载 Login/Chat 必需 chunk，echarts / three.js / element-plus 重型视图
// （ResearchView/DataPoolView/DesktopElf/AdminView 等）按路由访问时再异步分包下载，
// 显著减小首屏 JS 体积与首屏可交互时间。
const ForgotPasswordView = () => import('@/views/ForgotPasswordView.vue')
const RegisterView = () => import('@/views/RegisterView.vue')
const FriendView = () => import('@/views/FriendView.vue')
const FriendChatView = () => import('@/views/FriendChatView.vue')
const GroupChatView = () => import('@/views/GroupChatView.vue')
const CharacterImportView = () => import('@/views/CharacterImportView.vue')
const CreateCharacterView = () => import('@/views/CreateCharacterView.vue')
const AdminView = () => import('@/views/AdminView.vue')
const DataPoolView = () => import('@/views/DataPoolView.vue')
const FeedView = () => import('@/views/FeedView.vue')
const DesktopElf = () => import('@/views/DesktopElf.vue')
const MiniProgram = () => import('@/miniprogram/MiniProgram.vue')
const ProfileView = () => import('@/views/ProfileView.vue')
const UpgradeView = () => import('@/views/UpgradeView.vue')
const AdminProfileView = () => import('@/views/AdminProfileView.vue')
const AnnouncementView = () => import('@/views/AnnouncementView.vue')
const SocialFeedView = () => import('@/views/SocialFeedView.vue')
const NotificationView = () => import('@/views/NotificationView.vue')
const PrivacyView = () => import('@/views/PrivacyView.vue')
const AiElfView = () => import('@/views/AiElfView.vue')
const CommunityView = () => import('@/views/CommunityView.vue')
const MemoryView = () => import('@/views/MemoryView.vue')
const KnowledgeBaseView = () => import('@/views/KnowledgeBaseView.vue')
const PlazaView = () => import('@/views/PlazaView.vue')
const ResearchView = () => import('@/views/ResearchView.vue')
const UserAgreementView = () => import('@/views/UserAgreement.vue')

const routes = [
  { path: '/', name: 'landing', component: LandingPage },
  { path: '/login', name: 'login', component: LoginView },
  { path: '/forgot-password', name: 'forgotPassword', component: ForgotPasswordView },
  { path: '/register', name: 'register', component: RegisterView },
  { path: '/chat', name: 'chat', component: ChatView, meta: { requiresAuth: true } },
  { path: '/friends', name: 'friends', component: FriendView, meta: { requiresAuth: true } },
  { path: '/friend-chat', name: 'friendChat', component: FriendChatView, meta: { requiresAuth: true } },
  { path: '/group-chat', name: 'groupChat', component: GroupChatView, meta: { requiresAuth: true } },
  { path: '/character-import', name: 'characterImport', component: CharacterImportView, meta: { requiresAuth: true } },
  { path: '/create-character', name: 'createCharacter', component: CreateCharacterView, meta: { requiresAuth: true } },
  { path: '/admin', name: 'admin', component: AdminView, meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/datapool', name: 'datapool', component: DataPoolView, meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/feed', name: 'feed', component: FeedView, meta: { requiresAuth: true } },
  { path: '/desktop-elf', name: 'desktopElf', component: DesktopElf, meta: { requiresAuth: true } },
  { path: '/miniprogram', name: 'miniprogram', component: MiniProgram },
  { path: '/profile', name: 'profile', component: ProfileView, meta: { requiresAuth: true } },
  { path: '/upgrade', name: 'upgrade', component: UpgradeView, meta: { requiresAuth: true } },
  { path: '/admin-profile', name: 'adminProfile', component: AdminProfileView, meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/announcements', name: 'announcements', component: AnnouncementView, meta: { requiresAuth: true } },
  { path: '/social-feed', name: 'socialFeed', component: SocialFeedView, meta: { requiresAuth: true } },
  { path: '/notifications', name: 'notifications', component: NotificationView, meta: { requiresAuth: true } },
  { path: '/privacy', name: 'privacy', component: PrivacyView, meta: { requiresAuth: true } },
  { path: '/elf', name: 'elf', component: AiElfView, meta: { requiresAuth: true } },
  { path: '/community', name: 'community', component: CommunityView, meta: { requiresAuth: true } },
  { path: '/plaza', name: 'plaza', component: PlazaView },
  { path: '/character-square', name: 'characterSquare', component: PlazaView },
  { path: '/research', name: 'research', component: ResearchView, meta: { requiresAuth: true } },
  { path: '/memory', name: 'memory', component: MemoryView, meta: { requiresAuth: true } },
  { path: '/knowledge', name: 'knowledge', component: KnowledgeBaseView, meta: { requiresAuth: true } },
  // 用户协议 + 隐私政策：公开访问，无需登录
  { path: '/agreement', name: 'agreement', component: UserAgreementView, meta: { requiresAuth: false } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫 - 权限检查
router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !localStorage.getItem('token')) {
    next('/login')
  } else if (to.meta.requiresAdmin && localStorage.getItem('isAdmin') !== 'true') {
    next('/chat')
  } else {
    next()
  }
})

// 说明：主题统一由 composables/useTheme 的 initTheme() 在 App.vue onMounted 中
// 解析（localStorage 优先，否则跟随系统 prefers-color-scheme）并应用到 <html>。
// 此前此处的 afterEach 会在未存储主题时强制回退为 'light'，与 useTheme 的“跟随系统”
// 逻辑冲突：系统为暗色的用户每次路由切换都会被强制刷成亮色，造成主题闪烁。
// 主题类一旦 applyTheme 就常驻 <html>，路由切换不会改变它，因此这里无需重复设置。

export default router
