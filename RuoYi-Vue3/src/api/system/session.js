import request from '@/utils/request'

// 查询AI会话列表
export function listSession(query) {
  return request({
    url: '/system/session/list',
    method: 'get',
    params: query
  })
}

// 查询AI会话详细
export function getSession(sessionId) {
  return request({
    url: '/system/session/' + sessionId,
    method: 'get'
  })
}

// 新增AI会话
export function addSession(data) {
  return request({
    url: '/system/session',
    method: 'post',
    data: data
  })
}

// 修改AI会话
export function updateSession(data) {
  return request({
    url: '/system/session',
    method: 'put',
    data: data
  })
}

// 删除AI会话
export function delSession(sessionId) {
  return request({
    url: '/system/session/' + sessionId,
    method: 'delete'
  })
}
