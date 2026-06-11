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

export function listAgentHouses(query) {
  return request({
    url: '/rental/agent/houses',
    method: 'get',
    params: query
  })
}

export function getAgentHouse(houseId) {
  return request({
    url: `/rental/agent/houses/${houseId}`,
    method: 'get'
  })
}

export function listAgentAppointments(query) {
  return request({
    url: '/rental/agent/appointments',
    method: 'get',
    params: query
  })
}

export function confirmAgentAppointment(appointmentId) {
  return request({
    url: `/rental/agent/appointments/${appointmentId}/confirm`,
    method: 'post'
  })
}

export function rejectAgentAppointment(appointmentId, data) {
  return request({
    url: `/rental/agent/appointments/${appointmentId}/reject`,
    method: 'post',
    data
  })
}

export function completeAgentAppointment(appointmentId) {
  return request({
    url: `/rental/agent/appointments/${appointmentId}/complete`,
    method: 'post'
  })
}

export function listAgentIntentions(query) {
  return request({
    url: '/rental/agent/intentions',
    method: 'get',
    params: query
  })
}

export function followAgentIntention(intentionId, data) {
  return request({
    url: `/rental/agent/intentions/${intentionId}/follow`,
    method: 'post',
    data
  })
}

export function invalidAgentIntention(intentionId, data) {
  return request({
    url: `/rental/agent/intentions/${intentionId}/invalid`,
    method: 'post',
    data
  })
}

export function dealAgentIntention(intentionId, data) {
  return request({
    url: `/rental/agent/intentions/${intentionId}/deal`,
    method: 'post',
    data
  })
}

export function dealAgentHouse(houseId, data) {
  return request({
    url: `/rental/agent/houses/${houseId}/deal`,
    method: 'post',
    data
  })
}
