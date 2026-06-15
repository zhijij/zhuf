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

export function geocodeOwnerHouse(query) {
  return request({
    url: '/rental/owner/houses/geocode',
    method: 'get',
    params: query
  })
}

export function uploadOwnerHouseImage(data) {
  return request({
    url: '/rental/owner/houses/images/upload',
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getOwnerHouse(houseId) {
  return request({
    url: `/rental/owner/houses/${houseId}`,
    method: 'get'
  })
}

export function submitOwnerHouseAudit(houseId) {
  return request({
    url: `/rental/owner/houses/${houseId}/submit-audit`,
    method: 'post'
  })
}

export function listOwnerCandidateAgents(houseId) {
  return request({
    url: `/rental/owner/houses/${houseId}/candidate-agents`,
    method: 'get'
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

export function listOwnerEntrustApplications(query) {
  return request({
    url: '/rental/owner/entrust-applications',
    method: 'get',
    params: query
  })
}

export function confirmOwnerEntrust(entrustId) {
  return request({
    url: `/rental/owner/entrusts/${entrustId}/confirm`,
    method: 'post'
  })
}

export function rejectOwnerEntrust(entrustId) {
  return request({
    url: `/rental/owner/entrusts/${entrustId}/reject`,
    method: 'post'
  })
}
