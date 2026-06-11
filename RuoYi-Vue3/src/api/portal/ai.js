import request from '@/utils/request'

export function sendPortalAiChat(data) {
  return request({
    url: '/rental/ai/chat',
    method: 'post',
    data
  })
}

export function recommendRentalHouses(data) {
  return request({
    url: '/rental/ai/recommend',
    method: 'post',
    data
  })
}

export function createAiIndexTask(data) {
  return request({
    url: '/rental/ai/index/tasks',
    method: 'post',
    data
  })
}

export function listAiIndexTasks(query) {
  return request({
    url: '/rental/ai/index/tasks',
    method: 'get',
    params: query
  })
}

export function inspectAiHouseDocument(houseId) {
  return request({
    url: `/rental/ai/houses/${houseId}/document`,
    method: 'get'
  })
}
