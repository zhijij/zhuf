import router from './router'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { isHttp, isPathMatch } from '@/utils/validate'
import { isRelogin } from '@/utils/request'
import useUserStore from '@/store/modules/user'
import useLockStore from '@/store/modules/lock'
import useSettingsStore from '@/store/modules/settings'
import usePermissionStore from '@/store/modules/permission'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/register']

const isWhiteList = (path) => {
  return whiteList.some(pattern => isPathMatch(pattern, path))
}

const getHomeByRoles = (roles = []) => {
  if (roles.includes('admin')) return '/index'
  if (roles.includes('auditor')) return '/portal/auditor'
  return '/portal/index'
}
const isPortalRoute = (path = '') => path.startsWith('/portal')
const businessSafeRoutes = ['/portal/index', '/portal/auditor', '/portal/chat', '/login', '/register', '/401', '/404', '/lock']
const shouldRedirectBusinessUser = (roles = [], path = '') => {
  if (!roles.length || roles.includes('admin')) {
    return false
  }
  return !(isPortalRoute(path) || businessSafeRoutes.includes(path))
}
const shouldRedirectAuditor = (roles = [], to = {}) => {
  return roles.includes('auditor') && to.path === '/portal/index' && to.query?.entry !== 'audit'
}
const shouldBlockAuditorWorkbench = (roles = [], path = '') => {
  return path === '/portal/auditor' && !roles.includes('auditor')
}
const shouldRedirectSuperAdminFromPortal = (roles = [], path = '') => {
  return roles.includes('admin') && !roles.includes('auditor') && isPortalRoute(path)
}

router.beforeEach(async (to, from) => {
  NProgress.start()
  if (getToken()) {
    to.meta.title && useSettingsStore().setTitle(to.meta.title)
    const isLock = useLockStore().isLock
    if (to.path === '/login') {
      NProgress.done()
      return { path: useUserStore().roles.length ? getHomeByRoles(useUserStore().roles) : '/' }
    }
    if (isWhiteList(to.path)) {
      return true
    }
    if (isLock && to.path !== '/lock') {
      NProgress.done()
      return { path: '/lock' }
    }
    if (!isLock && to.path === '/lock') {
      NProgress.done()
      return { path: '/' }
    }
    if (useUserStore().roles.length === 0) {
      isRelogin.show = true
      try {
        // 拉取user_info信息
        await useUserStore().getInfo()
        isRelogin.show = false
        // 根据roles权限生成可访问的路由
        const accessRoutes = await usePermissionStore().generateRoutes()
        accessRoutes.forEach(route => {
          if (!isHttp(route.path)) {
            router.addRoute(route)
          }
        })
        const roles = useUserStore().roles || []
        if (shouldRedirectSuperAdminFromPortal(roles, to.path)) {
          return { path: '/index', replace: true }
        }
        if (shouldBlockAuditorWorkbench(roles, to.path)) {
          return { path: '/portal/index', replace: true }
        }
        if (shouldRedirectAuditor(roles, to)) {
          return { path: '/portal/auditor', replace: true }
        }
        if (shouldRedirectBusinessUser(roles, to.path)) {
          return { path: getHomeByRoles(roles), replace: true }
        }
        // 重新导航到目标路由，确保动态路由已注册
        return { ...to, replace: true }
      } catch (err) {
        await useUserStore().logOut()
        ElMessage.error(err)
        return { path: '/login' }
      }
    }
    const roles = useUserStore().roles || []
    if (shouldRedirectSuperAdminFromPortal(roles, to.path)) {
      NProgress.done()
      return { path: '/index' }
    }
    if (shouldBlockAuditorWorkbench(roles, to.path)) {
      NProgress.done()
      return { path: '/portal/index' }
    }
    if (shouldRedirectAuditor(roles, to)) {
      NProgress.done()
      return { path: '/portal/auditor' }
    }
    if (shouldRedirectBusinessUser(roles, to.path)) {
      NProgress.done()
      return { path: getHomeByRoles(roles) }
    }
    return true
  } else {
    // 没有token
    if (isWhiteList(to.path)) {
      // 在免登录白名单，直接进入
      return true
    }
    NProgress.done()
    return `/login?redirect=${to.fullPath}` // 否则全部重定向到登录页
  }
})

router.afterEach(() => {
  NProgress.done()
})
