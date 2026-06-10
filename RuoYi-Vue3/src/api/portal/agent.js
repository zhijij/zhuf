import request from '@/utils/request'

export function listAgentEntrusts(query) {
  return request({
    url: '/rental/agent/entrusts',
    method: 'get',
    params: query
  })
}

export function confirmAgentEntrust(entrustId) {
  return request({
    url: `/rental/agent/entrusts/${entrustId}/confirm`,
    method: 'post'
  })
}

export function rejectAgentEntrust(entrustId) {
  return request({
    url: `/rental/agent/entrusts/${entrustId}/reject`,
    method: 'post'
  })
}
