import axios from 'axios'
import { API_BASE_URL } from '../config'
import { useAuthStore } from '../stores/auth'
import router from '../router'

const apiClient = axios.create({ baseURL: API_BASE_URL })

apiClient.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore().logout()
      router.push('/login')
    }
    return Promise.reject(error)
  },
)

export default apiClient
