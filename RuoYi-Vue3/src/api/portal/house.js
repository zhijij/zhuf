import request from '@/utils/request'

export function listPortalHouses(query) {
  return request({
    url: '/rental/portal/houses',
    method: 'get',
    params: query
  })
}

export function getPortalHouseDetail(houseId) {
  return request({
    url: `/rental/portal/houses/${houseId}`,
    method: 'get'
  })
}

export function getPortalHouseMapContext(houseId, query) {
  return request({
    url: `/rental/portal/map/houses/${houseId}/context`,
    method: 'get',
    params: query
  })
}

export function searchPortalAround(query) {
  return request({
    url: '/rental/portal/map/around',
    method: 'get',
    params: query
  })
}

export function createPortalAppointment(data) {
  return request({
    url: '/rental/portal/appointments',
    method: 'post',
    data
  })
}
