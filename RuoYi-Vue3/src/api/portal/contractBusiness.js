import request from '@/utils/request'

export function listMyContracts(query) {
  return request({
    url: '/rental/contract/my',
    method: 'get',
    params: query
  })
}

export function getContractDetail(contractId) {
  return request({
    url: `/rental/contract/${contractId}`,
    method: 'get'
  })
}

export function submitContractSign(contractId) {
  return request({
    url: `/rental/contract/${contractId}/submit-sign`,
    method: 'post'
  })
}

export function confirmTenantContract(contractId, data) {
  return request({
    url: `/rental/contract/${contractId}/tenant-confirm`,
    method: 'post',
    data
  })
}

export function confirmOwnerContract(contractId, data) {
  return request({
    url: `/rental/contract/${contractId}/owner-confirm`,
    method: 'post',
    data
  })
}

export function confirmAgentContract(contractId, data) {
  return request({
    url: `/rental/contract/${contractId}/agent-confirm`,
    method: 'post',
    data
  })
}

export function rejectContract(contractId, data) {
  return request({
    url: `/rental/contract/${contractId}/reject`,
    method: 'post',
    data
  })
}

export function activateContract(contractId) {
  return request({
    url: `/rental/contract/${contractId}/activate`,
    method: 'post'
  })
}

export function voidContract(contractId, data) {
  return request({
    url: `/rental/contract/${contractId}/void`,
    method: 'post',
    data
  })
}

export function terminateContract(contractId, data) {
  return request({
    url: `/rental/contract/${contractId}/terminate`,
    method: 'post',
    data
  })
}

export function openContractChat(contractId) {
  return request({
    url: `/rental/contract/${contractId}/chat`,
    method: 'post'
  })
}
