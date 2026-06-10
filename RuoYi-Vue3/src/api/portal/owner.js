import request from '@/utils/request'

export function listOwnerHouses(query) {
  return request({
    url: '/rental/owner/houses',
    method: 'get',
    params: query
  })
}

export function createOwnerHouse(data) {
  return request({
    url: '/rental/owner/houses',
    method: 'post',
    data
  })
}

export function submitOwnerHouseAudit(houseId) {
  return request({
    url: `/rental/owner/houses/${houseId}/submit-audit`,
    method: 'post'
  })
}

export function entrustOwnerHouse(houseId, data) {
  return request({
    url: `/rental/owner/houses/${houseId}/entrust`,
    method: 'post',
    data
  })
}

export function listOwnerEntrusts(query) {
  return request({
    url: '/rental/owner/entrusts',
    method: 'get',
    params: query
  })
}
