import request from '@/utils/request'

export function listTenantHouses(query) {
  return request({
    url: '/rental/tenant/houses',
    method: 'get',
    params: query
  })
}

export function getTenantHouse(houseId) {
  return request({
    url: `/rental/tenant/houses/${houseId}`,
    method: 'get'
  })
}

export function listTenantFavorites(query) {
  return request({
    url: '/rental/tenant/favorites',
    method: 'get',
    params: query
  })
}

export function favoriteTenantHouse(houseId) {
  return request({
    url: `/rental/tenant/favorites/${houseId}`,
    method: 'post'
  })
}

export function cancelTenantFavorite(houseId) {
  return request({
    url: `/rental/tenant/favorites/${houseId}`,
    method: 'delete'
  })
}

export function listTenantAppointments(query) {
  return request({
    url: '/rental/tenant/appointments',
    method: 'get',
    params: query
  })
}

export function createTenantAppointment(data) {
  return request({
    url: '/rental/tenant/appointments',
    method: 'post',
    data
  })
}

export function cancelTenantAppointment(appointmentId, data) {
  return request({
    url: `/rental/tenant/appointments/${appointmentId}/cancel`,
    method: 'post',
    data
  })
}

export function listTenantIntentions(query) {
  return request({
    url: '/rental/tenant/intentions',
    method: 'get',
    params: query
  })
}

export function createTenantIntention(data) {
  return request({
    url: '/rental/tenant/intentions',
    method: 'post',
    data
  })
}

export function abandonTenantIntention(intentionId) {
  return request({
    url: `/rental/tenant/intentions/${intentionId}/abandon`,
    method: 'post'
  })
}

export function applyTenantDeal(houseId, data) {
  return request({
    url: `/rental/tenant/houses/${houseId}/deal`,
    method: 'post',
    data
  })
}

export function listTenantContracts(query) {
  return request({
    url: '/rental/tenant/contracts',
    method: 'get',
    params: query
  })
}
