import request from '@/utils/request'

export function getPortalHomeSummary() {
  return request({
    url: '/rental/portal/summary',
    method: 'get'
  })
}

export function getPortalRolePanels() {
  return request({
    url: '/rental/portal/panels',
    method: 'get'
  })
}
