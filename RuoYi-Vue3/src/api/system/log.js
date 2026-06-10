import request from '@/utils/request'

// 查询AI工具调用审计列表
export function listLog(query) {
  return request({
    url: '/system/log/list',
    method: 'get',
    params: query
  })
}

// 查询AI工具调用审计详细
export function getLog(logId) {
  return request({
    url: '/system/log/' + logId,
    method: 'get'
  })
}

// 新增AI工具调用审计
export function addLog(data) {
  return request({
    url: '/system/log',
    method: 'post',
    data: data
  })
}

// 修改AI工具调用审计
export function updateLog(data) {
  return request({
    url: '/system/log',
    method: 'put',
    data: data
  })
}

// 删除AI工具调用审计
export function delLog(logId) {
  return request({
    url: '/system/log/' + logId,
    method: 'delete'
  })
}
