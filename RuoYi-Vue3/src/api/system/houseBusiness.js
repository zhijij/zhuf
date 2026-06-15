import request from '@/utils/request'

export function listPublicHouse(query) {
  return request({
    url: '/system/house/public-list',
    method: 'get',
    params: query
  })
}

export function getHouseBusinessDetail(houseId) {
  return request({
    url: `/system/house/detail/${houseId}`,
    method: 'get'
  })
}

export function listAuditHouse(query) {
  return request({
    url: '/system/house/audit-queue',
    method: 'get',
    params: query
  })
}

export function submitHouse(data) {
  return request({
    url: '/system/house/submit',
    method: 'post',
    data
  })
}

export function updateHouseBeforeApproval(houseId, data) {
  return request({
    url: `/system/house/${houseId}/before-approval`,
    method: 'put',
    data
  })
}

export function resubmitHouseAudit(houseId) {
  return request({
    url: `/system/house/${houseId}/resubmit`,
    method: 'post'
  })
}

export function cancelHouse(houseId, data) {
  return request({
    url: `/system/house/${houseId}/cancel`,
    method: 'post',
    data
  })
}

export function auditHouse(houseId, data) {
  return request({
    url: `/system/house/${houseId}/audit`,
    method: 'post',
    data
  })
}

export function entrustHouse(houseId, data) {
  return request({
    url: `/system/house/${houseId}/entrust`,
    method: 'post',
    data
  })
}

export function completeHouseDeal(houseId, data) {
  return request({
    url: `/system/house/${houseId}/deal`,
    method: 'post',
    data
  })
}
