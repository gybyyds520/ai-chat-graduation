import axios from 'axios'
import logger from './logger.js'

const request = axios.create({
  baseURL: '',
  timeout: 30000
})

request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    if (!config.headers['Content-Type'] && !(config.data instanceof FormData)) {
      config.headers['Content-Type'] = 'application/json;charset=UTF-8'
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  response => {
    if (response.data && response.data.code === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      localStorage.removeItem('isAdmin')
      window.location.href = '/login'
      return Promise.reject(new Error('未授权'))
    }
    return response.data
  },
  error => {
    logger.error('请求错误:', error)
    const errorMsg = error.response?.data?.message || error.response?.statusText || error.message || '请求失败'
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      localStorage.removeItem('isAdmin')
      window.location.href = '/login'
    } else if (error.response?.status === 500) {
      logger.error('服务器错误详情:', error.response.data)
    }
    return Promise.reject(error)
  }
)

export default request