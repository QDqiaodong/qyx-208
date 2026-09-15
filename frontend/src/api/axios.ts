import axios from 'axios'

const instance = axios.create({
  baseURL: '/api',
  timeout: 10000
})

instance.interceptors.response.use(
  response => {
    if (response.data.code === 200) {
      return response.data.data
    }
    throw new Error(response.data.message || '请求失败')
  },
  error => {
    console.error('API Error:', error)
    // 优先抛出后端返回的业务错误信息（如超限原因），而不是 HTTP 状态码
    const message = error.response?.data?.message || error.message || '请求失败'
    throw new Error(message)
  }
)

export default instance
