import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../views/HomeView.vue'),
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
    },
    {
      path: '/admin',
      component: () => import('../layouts/AdminLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: '/admin/dashboard' },
        {
          path: 'dashboard',
          name: 'admin-dashboard',
          component: () => import('../views/admin/DashboardView.vue'),
        },
        {
          path: 'members',
          name: 'admin-members',
          component: () => import('../views/admin/MembersView.vue'),
        },
        {
          path: 'providers',
          name: 'admin-providers',
          component: () => import('../views/admin/ProvidersView.vue'),
        },
        {
          path: 'policies',
          name: 'admin-policies',
          component: () => import('../views/admin/PoliciesView.vue'),
        },
        {
          path: 'claims',
          name: 'admin-claims',
          component: () => import('../views/admin/ClaimsView.vue'),
        },
        {
          path: 'reports',
          name: 'admin-reports',
          component: () => import('../views/admin/ReportsView.vue'),
        },
      ],
    },
    {
      path: '/provider',
      component: () => import('../layouts/ProviderLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: '/provider/verify' },
        {
          path: 'verify',
          name: 'provider-verify',
          component: () => import('../views/provider/VerifyMemberView.vue'),
        },
        {
          path: 'claim',
          name: 'provider-claim',
          component: () => import('../views/provider/SubmitClaimView.vue'),
        },
        {
          path: 'preauth',
          name: 'provider-preauth',
          component: () => import('../views/provider/PreAuthorizationView.vue'),
        },
        {
          path: 'history',
          name: 'provider-history',
          component: () => import('../views/provider/TransactionHistoryView.vue'),
        },
      ],
    },
    {
      // Demo-mode only: no real member identity/login exists yet, so this
      // portal is intentionally not behind requiresAuth. See IMPLEMENTATION_GUIDE.md §9.
      path: '/member',
      component: () => import('../layouts/MemberLayout.vue'),
      children: [
        { path: '', redirect: '/member/card' },
        {
          path: 'card',
          name: 'member-card',
          component: () => import('../views/member/MyCardView.vue'),
        },
        {
          path: 'benefits',
          name: 'member-benefits',
          component: () => import('../views/member/PolicyBenefitsView.vue'),
        },
        {
          path: 'claims',
          name: 'member-claims',
          component: () => import('../views/member/ClaimsHistoryView.vue'),
        },
        {
          path: 'dependents',
          name: 'member-dependents',
          component: () => import('../views/member/DependentsView.vue'),
        },
      ],
    },
  ],
})

function postLoginHome(): string {
  const auth = useAuthStore()
  return auth.isProvider ? '/provider/verify' : '/admin/dashboard'
}

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.hasRealSession) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && auth.hasRealSession) {
    return { path: postLoginHome() }
  }
  return true
})

export default router
