<template>
  <div class="social-feed-container">
    <div class="social-header">
      <button class="back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回</span>
      </button>
      <h1>社交动态</h1>
      <button class="btn-primary" @click="showPostModal = true">发布动态</button>
    </div>
    
    <div class="search-bar">
      <input 
        v-model="searchKeyword" 
        placeholder="搜索动态、用户、话题..." 
        @keyup.enter="handleSearch"
        class="search-input"
      />
      <button @click="handleSearch" class="search-btn">搜索</button>
    </div>
    
    <div v-if="searchResults && searchResults.length > 0" class="search-results">
      <h3>搜索结果</h3>
      <div v-for="post in searchResults" :key="post.id" class="post-card" @click="trackPostView(post.id)">
        <div class="post-header">
          <div class="avatar-circle">
            <span>{{ post.userName?.charAt(0) || 'U' }}</span>
          </div>
          <div class="user-info">
            <span class="user-name">{{ post.userName }}</span>
            <span class="post-time">{{ formatTime(post.createdAt) }}</span>
          </div>
        </div>
        <div class="post-content">{{ post.content }}</div>
      </div>
    </div>
    
    <div class="feed-tabs">
      <button 
        v-for="tab in tabs" 
        :key="tab.key"
        :class="['tab-btn', { active: activeTab === tab.key }]"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
      </button>
    </div>
    
    <div class="posts-container">
      <div 
        v-for="post in posts" 
        :key="post.id" 
        class="post-card"
        @click="trackPostView(post.id)"
      >
        <div class="post-header">
          <div class="avatar-circle">
            <span>{{ post.userName?.charAt(0) || 'U' }}</span>
          </div>
          <div class="user-info">
            <span class="user-name">{{ post.userName }}</span>
            <span class="post-time">{{ formatTime(post.createdAt) }}</span>
          </div>
          <div v-if="post.userId === currentUserId" class="post-actions">
            <button @click.stop="deletePost(post.id)" class="action-btn">删除</button>
          </div>
        </div>
        
        <div class="post-content">
          {{ post.content }}
        </div>
        
        <div v-if="post.mediaType === 'image' && post.mediaUrls" class="post-images">
          <img 
            v-for="(url, index) in JSON.parse(post.mediaUrls)" 
            :key="index" 
            :src="url" 
            class="post-image"
          />
        </div>
        
        <div v-if="post.mediaType === 'video' && post.videoUrl" class="post-video">
          <video :src="post.videoUrl" controls class="video-player"></video>
        </div>
        
        <div v-if="post.location" class="post-location">
          <span>📍 {{ post.location }}</span>
        </div>
        
        <div class="post-stats">
          <span>{{ post.likeCount }} 赞</span>
          <span>{{ post.commentCount }} 评论</span>
          <span>{{ post.shareCount }} 分享</span>
        </div>
        
        <div class="post-actions-bar">
          <button 
            :class="['action-item', { liked: post.isLiked }]"
            @click="toggleLike('post', post.id)"
          >
            <span>{{ post.isLiked ? '❤️' : '🤍' }}</span>
            <span>{{ post.isLiked ? '已赞' : '点赞' }}</span>
          </button>
          <button class="action-item" @click="openComments(post.id)">
            <span>💬</span>
            <span>评论</span>
          </button>
          <button class="action-item" @click="sharePost(post.id)">
            <span>🔗</span>
            <span>分享</span>
          </button>
        </div>
        
        <div v-if="post.showComments" class="post-comments">
          <div class="comment-input">
            <input 
              v-model="newComment" 
              placeholder="写下评论..." 
              @keyup.enter="submitComment(post.id)"
            />
            <button @click="submitComment(post.id)" class="btn-submit">发送</button>
          </div>
          <div v-for="comment in post.comments" :key="comment.id" class="comment-item">
            <div class="comment-header">
              <span class="comment-user">{{ comment.userName }}</span>
              <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
            </div>
            <div class="comment-content">{{ comment.content }}</div>
            <button 
              :class="['comment-like', { liked: comment.isLiked }]"
              @click="toggleLike('comment', comment.id)"
            >
              {{ comment.isLiked ? '❤️' : '🤍' }} {{ comment.likeCount }}
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <div v-if="showPostModal" class="modal-overlay" @click="showPostModal = false">
      <div class="modal-content" @click.stop>
        <h2>发布动态</h2>
        <textarea 
          v-model="postContent" 
          placeholder="分享你的想法..." 
          rows="4"
        ></textarea>
        
        <div class="media-upload">
          <button @click="uploadImage" class="upload-btn">📷 上传图片</button>
          <button @click="uploadVideo" class="upload-btn">🎥 上传视频</button>
        </div>
        
        <div v-if="uploadedImages.length > 0" class="preview-images">
          <img 
            v-for="(url, index) in uploadedImages" 
            :key="index" 
            :src="url" 
            class="preview-image"
          />
        </div>
        
        <input v-model="postLocation" placeholder="添加位置..." />
        
        <div class="visibility-select">
          <span>可见范围:</span>
          <select v-model="postVisibility">
            <option value="public">公开</option>
            <option value="friends">好友</option>
            <option value="private">仅自己</option>
          </select>
        </div>
        
        <div class="modal-actions">
          <button @click="showPostModal = false" class="btn-cancel">取消</button>
          <button @click="submitPost" class="btn-primary">发布</button>
        </div>
      </div>
    </div>
    
    <input type="file" ref="imageInput" accept="image/*" style="display: none" @change="handleImageUpload" />
    <input type="file" ref="videoInput" accept="video/*" style="display: none" @change="handleVideoUpload" />
  </div>
</template>

<script setup>import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ArrowLeft } from '@element-plus/icons-vue';
import request from '@/utils/request';
import { getCurrentUserId } from '@/utils/auth';
const router = useRouter();
// 返回上一页
const goBack = () => {
  const fromRoute = router.options.history.state.value?.back;
  if (fromRoute && fromRoute.fullPath) {
    router.back();
  } else {
    router.push('/chat');
  }
};
const showPostModal = ref(false);
const postContent = ref('');
const postLocation = ref('');
const postVisibility = ref('public');
const uploadedImages = ref([]);
const uploadedVideo = ref('');
const newComment = ref('');
const activeTab = ref('recommended');
const tabs = [
 { key: 'recommended', label: '推荐' },
 { key: 'public', label: '热门' },
 { key: 'friends', label: '好友' }
];
const posts = ref([]);
const searchKeyword = ref('');
const searchResults = ref(null);
const currentUserId = ref(getCurrentUserId() || 0);
const imageInput = ref(null);
const videoInput = ref(null);
const loadPosts = async () => {
 try {
 let res;
 if (activeTab.value === 'recommended') {
 res = await request.get(`/api/recommendation/posts?userId=${currentUserId.value}&limit=20`);
 }
 else if (activeTab.value === 'public') {
 res = await request.get('/api/social/posts/public?limit=20');
 }
 else {
 res = await request.post('/api/social/posts/friends', {
 userIds: [1, 2, 3],
 limit: 20
 });
 }
 if (res.code === 200) {
 posts.value = res.data.map(post => ({
 ...post,
 isLiked: false,
 showComments: false,
 comments: []
 }));
 }
 }
 catch (error) {
 console.error('加载动态失败:', error);
 }
};
const submitPost = async () => {
 if (!postContent.value.trim())
 return;
 try {
 const mediaType = uploadedImages.value.length > 0 ? 'image' : (uploadedVideo.value ? 'video' : 'text');
 const mediaUrls = uploadedImages.value.length > 0 ? JSON.stringify(uploadedImages.value) : null;
 await request.post('/api/social/post', {
 userId: currentUserId.value,
 content: postContent.value,
 mediaType,
 mediaUrls,
 videoUrl: uploadedVideo.value,
 location: postLocation.value,
 visibility: postVisibility.value
 });
 showPostModal.value = false;
 postContent.value = '';
 postLocation.value = '';
 uploadedImages.value = [];
 uploadedVideo.value = '';
 loadPosts();
 }
 catch (error) {
 console.error('发布失败:', error);
 }
};
const toggleLike = async (targetType, targetId) => {
 try {
 const res = await request.post('/api/social/like', {
 targetType,
 targetId,
 userId: currentUserId.value
 });
 if (res.code === 200) {
 if (targetType === 'post') {
 const post = posts.value.find(p => p.id === targetId);
 if (post) {
 post.isLiked = res.data.liked;
 post.likeCount = res.data.count;
 }
 }
 else {
 posts.value.forEach(post => {
 const comment = post.comments.find(c => c.id === targetId);
 if (comment) {
 comment.isLiked = res.data.liked;
 comment.likeCount = res.data.count;
 }
 });
 }
 }
 }
 catch (error) {
 console.error('点赞失败:', error);
 }
};
const openComments = async (postId) => {
 const post = posts.value.find(p => p.id === postId);
 if (!post)
 return;
 post.showComments = !post.showComments;
 if (post.showComments && post.comments.length === 0) {
 try {
 const res = await request.get(`/api/social/comments/${postId}`);
 if (res.code === 200) {
 post.comments = res.data.map(c => ({ ...c, isLiked: false }));
 }
 }
 catch (error) {
 console.error('加载评论失败:', error);
 }
 }
};
const submitComment = async (postId) => {
 if (!newComment.value.trim())
 return;
 try {
 const res = await request.post('/api/social/comment', {
 postId,
 userId: currentUserId.value,
 content: newComment.value
 });
 if (res.code === 200) {
 const post = posts.value.find(p => p.id === postId);
 if (post) {
 post.comments.unshift({ ...res.data, isLiked: false });
 post.commentCount++;
 }
 newComment.value = '';
 }
 }
 catch (error) {
 console.error('评论失败:', error);
 }
};
const deletePost = async (postId) => {
 if (!confirm('确定删除这条动态吗？'))
 return;
 try {
 await request.delete(`/api/social/post/${postId}?userId=${currentUserId.value}`);
 posts.value = posts.value.filter(p => p.id !== postId);
 }
 catch (error) {
 console.error('删除失败:', error);
 }
};
const sharePost = async (postId) => {
 try {
 await request.post('/api/social/share', {
 postId,
 userId: currentUserId.value,
 shareText: '分享了一条动态',
 shareType: 'feed'
 });
 alert('分享成功');
 }
 catch (error) {
 console.error('分享失败:', error);
 }
};
const handleSearch = async () => {
 if (!searchKeyword.value.trim()) {
 searchResults.value = null;
 return;
 }
 try {
 const res = await request.get(`/api/search/posts?keyword=${encodeURIComponent(searchKeyword.value)}&limit=20`);
 if (res.code === 200) {
 searchResults.value = res.data;
 }
 }
 catch (error) {
 console.error('搜索失败:', error);
 }
};
const trackPostView = async (postId) => {
 try {
 await request.post('/api/recommendation/track/view', {
 userId: currentUserId.value,
 postId
 });
 }
 catch (error) {
 console.error('跟踪失败:', error);
 }
};
const uploadImage = () => {
 imageInput.value?.click();
};
const handleImageUpload = async (event) => {
 const file = event.target.files?.[0];
 if (!file)
 return;
 try {
 const formData = new FormData();
 formData.append('file', file);
 const res = await request.post('/api/social/upload/image', formData);
 if (res.code === 200) {
 uploadedImages.value.push(res.data.url);
 }
 }
 catch (error) {
 console.error('上传失败:', error);
 }
};
const uploadVideo = () => {
 videoInput.value?.click();
};
const handleVideoUpload = async (event) => {
 const file = event.target.files?.[0];
 if (!file)
 return;
 try {
 const formData = new FormData();
 formData.append('file', file);
 const res = await request.post('/api/social/upload/video', formData);
 if (res.code === 200) {
 uploadedVideo.value = res.data.url;
 }
 }
 catch (error) {
 console.error('上传失败:', error);
 }
};
const formatTime = (timeStr) => {
 if (!timeStr)
 return '';
 const date = new Date(timeStr);
 const now = new Date();
 const diff = now - date;
 const minutes = Math.floor(diff / 60000);
 const hours = Math.floor(diff / 3600000);
 const days = Math.floor(diff / 86400000);
 if (minutes < 1)
 return '刚刚';
 if (minutes < 60)
 return `${minutes}分钟前`;
 if (hours < 24)
 return `${hours}小时前`;
 return `${days}天前`;
};
onMounted(() => {
 loadPosts();
});
</script>

<style scoped>
.social-feed-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.social-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.social-header h1 {
  font-size: 24px;
  font-weight: bold;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba(0, 0, 0, 0.05);
  border: none;
  border-radius: 8px;
  color: inherit;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.back-btn:hover {
  background: rgba(0, 0, 0, 0.1);
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
}

.btn-primary:hover {
  opacity: 0.9;
}

.feed-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.tab-btn {
  padding: 8px 16px;
  border: none;
  background: #f5f5f5;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
}

.tab-btn.active {
 background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
 color: white;
}

.search-bar {
 display: flex;
 gap: 10px;
 margin-bottom: 20px;
}

.search-input {
 flex: 1;
 padding: 12px 16px;
 border: 1px solid #ddd;
 border-radius: 24px;
 font-size: 14px;
}

.search-btn {
 padding: 12px 24px;
 border: none;
 background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
 color: white;
 border-radius: 24px;
 cursor: pointer;
}

.search-results {
 margin-bottom: 20px;
}

.search-results h3 {
 margin-bottom: 12px;
 font-size: 16px;
}

.posts-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.post-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.post-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.avatar-circle {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
}

.user-info {
  flex: 1;
}

.user-name {
  display: block;
  font-weight: bold;
}

.post-time {
  font-size: 12px;
  color: #999;
}

.post-actions {
  margin-left: auto;
}

.action-btn {
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  font-size: 12px;
}

.action-btn:hover {
  color: #ff4d4f;
}

.post-content {
  margin-bottom: 12px;
  line-height: 1.6;
}

.post-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.post-image {
  width: calc(33.33% - 6px);
  height: 120px;
  object-fit: cover;
  border-radius: 8px;
}

.post-video {
  margin-bottom: 12px;
}

.video-player {
  width: 100%;
  max-height: 300px;
  border-radius: 8px;
}

.post-location {
  font-size: 12px;
  color: #999;
  margin-bottom: 12px;
}

.post-stats {
  display: flex;
  gap: 20px;
  margin-bottom: 12px;
  font-size: 12px;
  color: #999;
}

.post-actions-bar {
  display: flex;
  justify-content: space-around;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  color: #666;
}

.action-item:hover {
  color: #667eea;
}

.action-item.liked span:first-child {
  color: #ff4d4f;
}

.post-comments {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.comment-input {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.comment-input input {
  flex: 1;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 20px;
  font-size: 14px;
}

.btn-submit {
  background: #667eea;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 20px;
  cursor: pointer;
}

.comment-item {
  margin-bottom: 12px;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.comment-user {
  font-weight: bold;
  font-size: 12px;
}

.comment-time {
  font-size: 10px;
  color: #999;
}

.comment-content {
  font-size: 14px;
  margin-bottom: 4px;
}

.comment-like {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 12px;
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
  background: white;
  border-radius: 16px;
  padding: 24px;
  width: 90%;
  max-width: 500px;
}

.modal-content h2 {
  margin-bottom: 16px;
}

.modal-content textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  resize: vertical;
  font-size: 14px;
}

.media-upload {
  display: flex;
  gap: 10px;
  margin: 16px 0;
}

.upload-btn {
  padding: 10px 20px;
  border: 1px solid #ddd;
  border-radius: 20px;
  background: white;
  cursor: pointer;
}

.preview-images {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.preview-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
}

.visibility-select {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn-cancel {
  padding: 10px 20px;
  border: 1px solid #ddd;
  border-radius: 20px;
  background: white;
  cursor: pointer;
}
</style>