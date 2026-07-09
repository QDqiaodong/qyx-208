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
    throw error
  }
)

export default instance