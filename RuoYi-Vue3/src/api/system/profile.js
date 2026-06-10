import request from '@/utils/request'

// 查询户主资料列表
export function listProfile(query) {
  return request({
    url: '/system/profile/list',
    method: 'get',
    params: query
  })
}

// 查询户主资料详细
export function getProfile(ownerId) {
  return request({
    url: '/system/profile/' + ownerId,
    method: 'get'
  })
}

// 新增户主资料
export function addProfile(data) {
  return request({
    url: '/system/profile',
    method: 'post',
    data: data
  })
}

// 修改户主资料
export function updateProfile(data) {
  return request({
    url: '/system/profile',
    method: 'put',
    data: data
  })
}

// 删除户主资料
export function delProfile(ownerId) {
  return request({
    url: '/system/profile/' + ownerId,
    method: 'delete'
  })
}
