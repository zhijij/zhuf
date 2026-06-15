import request from '@/utils/request'

const AI_REQUEST_TIMEOUT = 60000

export function sendPortalAiChat(data) {
  return request({
    url: '/rental/ai/chat',
    method: 'post',
    data,
    timeout: AI_REQUEST_TIMEOUT
  })
}

export function listAiConversations(query) {
  return request({
    url: '/rental/ai/conversations',
    method: 'get',
    params: query
  })
}

export function createAiConversation(data) {
  return request({
    url: '/rental/ai/conversations',
    method: 'post',
    data
  })
}

export function getAiConversation(conversationId) {
  return request({
    url: `/rental/ai/conversations/${conversationId}`,
    method: 'get'
  })
}

export function deleteAiConversation(conversationId) {
  return request({
    url: `/rental/ai/conversations/${conversationId}`,
    method: 'delete'
  })
}

export function updateAiConversationContext(conversationId, data) {
  return request({
    url: `/rental/ai/conversations/${conversationId}/context`,
    method: 'put',
    data
  })
}

export function summarizeAiConversation(conversationId) {
  return request({
    url: `/rental/ai/conversations/${conversationId}/summary`,
    method: 'post'
  })
}

export function sendAiConversationMessage(conversationId, data) {
  return request({
    url: `/rental/ai/conversations/${conversationId}/messages`,
    method: 'post',
    data,
    timeout: AI_REQUEST_TIMEOUT
  })
}

export function recommendRentalHouses(data) {
  return request({
    url: '/rental/ai/recommend',
    method: 'post',
    data,
    timeout: AI_REQUEST_TIMEOUT
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
