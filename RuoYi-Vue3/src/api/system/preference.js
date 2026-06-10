import request from '@/utils/request'

// 查询租户租房偏好列表
export function listPreference(query) {
  return request({
    url: '/system/preference/list',
    method: 'get',
    params: query
  })
}

// 查询租户租房偏好详细
export function getPreference(preferenceId) {
  return request({
    url: '/system/preference/' + preferenceId,
    method: 'get'
  })
}

// 新增租户租房偏好
export function addPreference(data) {
  return request({
    url: '/system/preference',
    method: 'post',
    data: data
  })
}

// 修改租户租房偏好
export function updatePreference(data) {
  return request({
    url: '/system/preference',
    method: 'put',
    data: data
  })
}

// 删除租户租房偏好
export function delPreference(preferenceId) {
  return request({
    url: '/system/preference/' + preferenceId,
    method: 'delete'
  })
}
