import request from '@/utils/request'

export function sendPortalAiChat(data) {
  return request({
    url: '/rental/ai/chat',
    method: 'post',
    data
  })
}
