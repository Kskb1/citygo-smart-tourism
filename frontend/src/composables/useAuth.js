import { computed, ref } from 'vue'
import { api } from '../api/client'

const TOKEN_KEY = 'citygo:token'
const USER_KEY = 'citygo:user'

const token = ref(window.localStorage.getItem(TOKEN_KEY) || '')
const user = ref(readUser())
const profileChecked = ref(false)

function readUser() {
  try {
    return JSON.parse(window.localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

function saveSession(nextToken, nextUser) {
  token.value = nextToken
  user.value = nextUser
  profileChecked.value = true
  window.localStorage.setItem(TOKEN_KEY, nextToken)
  window.localStorage.setItem(USER_KEY, JSON.stringify(nextUser))
}

export function useAuth() {
  const isLoggedIn = computed(() => Boolean(token.value && user.value))
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  async function login(username, password) {
    const { data } = await api.post('/auth/login', { username, password })
    saveSession(data.token, data.user)
    return data.user
  }

  async function register(payload) {
    return api.post('/auth/register', payload).then(response => response.data)
  }

  function logout() {
    token.value = ''
    user.value = null
    profileChecked.value = false
    window.localStorage.removeItem(TOKEN_KEY)
    window.localStorage.removeItem(USER_KEY)
  }

  function restoreFromLocalStorage() {
    token.value = window.localStorage.getItem(TOKEN_KEY) || ''
    user.value = readUser()
  }

  async function validateProfile() {
    restoreFromLocalStorage()
    if (!token.value || !user.value) {
      logout()
      return false
    }
    if (profileChecked.value) return true
    try {
      const { data } = await api.get('/auth/profile')
      user.value = data
      window.localStorage.setItem(USER_KEY, JSON.stringify(data))
      profileChecked.value = true
      return true
    } catch {
      logout()
      return false
    }
  }

  return {
    token,
    user,
    isLoggedIn,
    isAdmin,
    login,
    register,
    logout,
    restoreFromLocalStorage,
    validateProfile
  }
}
