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

export function getAiCapabilities() {
  return request({
    url: '/rental/ai/capabilities',
    method: 'get'
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

export function processAiIndexTask(taskId) {
  return request({
    url: `/rental/ai/index/tasks/${taskId}/process`,
    method: 'post'
  })
}

export function processPendingAiIndexTasks(data) {
  return request({
    url: '/rental/ai/index/tasks/process-pending',
    method: 'post',
    data
  })
}

export function inspectAiHouseDocument(houseId) {
  return request({
    url: `/rental/ai/houses/${houseId}/document`,
    method: 'get'
  })
}

export function indexKnowledgeDocument(data) {
  return request({
    url: '/rental/ai/index/knowledge',
    method: 'post',
    data
  })
}

export function seedKnowledgeDocuments() {
  return request({
    url: '/rental/ai/index/knowledge/seed',
    method: 'post'
  })
}
