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

export function createPortalAppointment(data) {
  return request({
    url: '/rental/portal/appointments',
    method: 'post',
    data
  })
}
