import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import HomeView from '../views/HomeView.vue'
import PlanView from '../views/PlanView.vue'
import TransportView from '../views/TransportView.vue'
import WeatherView from '../views/WeatherView.vue'
import HotelView from '../views/HotelView.vue'
import SpotView from '../views/SpotView.vue'
import AdminView from '../views/AdminView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import MyTripsView from '../views/MyTripsView.vue'
import TripDetailView from '../views/TripDetailView.vue'
import { useAuth } from '../composables/useAuth'

const protectedMeta = { requiresAuth: true }

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView, meta: { guestOnly: true } },
    { path: '/register', component: RegisterView, meta: { guestOnly: true } },
    { path: '/', component: HomeView, meta: protectedMeta },
    { path: '/plan', redirect: '/planner' },
    { path: '/planner', component: PlanView, meta: protectedMeta },
    { path: '/transport', component: TransportView, meta: protectedMeta },
    { path: '/weather', component: WeatherView, meta: protectedMeta },
    { path: '/hotels', component: HotelView, meta: protectedMeta },
    { path: '/spots', component: SpotView, meta: protectedMeta },
    { path: '/my-trips', component: MyTripsView, meta: protectedMeta },
    { path: '/my-trips/:id', component: TripDetailView, meta: protectedMeta },
    { path: '/admin', component: AdminView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.beforeEach(async (to) => {
  const auth = useAuth()
  auth.restoreFromLocalStorage()

  if (to.meta.guestOnly && auth.isLoggedIn.value) {
    return '/'
  }

  if (to.meta.requiresAuth) {
    const valid = await auth.validateProfile()
    if (!valid) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }

  if (to.meta.requiresAdmin && !auth.isAdmin.value) {
    ElMessage.warning('当前账号没有管理员权限')
    return '/'
  }

  return true
})

export default router
