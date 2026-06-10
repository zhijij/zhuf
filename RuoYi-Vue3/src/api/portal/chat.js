import request from '@/utils/request'

export function listChatSessions() {
  return request({
    url: '/rental/chat/sessions',
    method: 'get'
  })
}

export function listChatMessages(sessionId) {
  return request({
    url: `/rental/chat/sessions/${sessionId}/messages`,
    method: 'get'
  })
}

export function openChatSession(data) {
  return request({
    url: '/rental/chat/sessions/open',
    method: 'post',
    data
  })
}

export function sendChatMessage(data) {
  return request({
    url: '/rental/chat/messages',
    method: 'post',
    data
  })
}
