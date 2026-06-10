import request from '@/utils/request'

// 查询房源委托关系列表
export function listEntrust(query) {
  return request({
    url: '/system/entrust/list',
    method: 'get',
    params: query
  })
}

// 查询房源委托关系详细
export function getEntrust(entrustId) {
  return request({
    url: '/system/entrust/' + entrustId,
    method: 'get'
  })
}

// 新增房源委托关系
export function addEntrust(data) {
  return request({
    url: '/system/entrust',
    method: 'post',
    data: data
  })
}

// 修改房源委托关系
export function updateEntrust(data) {
  return request({
    url: '/system/entrust',
    method: 'put',
    data: data
  })
}

// 删除房源委托关系
export function delEntrust(entrustId) {
  return request({
    url: '/system/entrust/' + entrustId,
    method: 'delete'
  })
}
