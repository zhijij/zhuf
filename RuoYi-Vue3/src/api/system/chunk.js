import request from '@/utils/request'

// 查询AI知识库分片列表
export function listChunk(query) {
  return request({
    url: '/system/chunk/list',
    method: 'get',
    params: query
  })
}

// 查询AI知识库分片详细
export function getChunk(chunkId) {
  return request({
    url: '/system/chunk/' + chunkId,
    method: 'get'
  })
}

// 新增AI知识库分片
export function addChunk(data) {
  return request({
    url: '/system/chunk',
    method: 'post',
    data: data
  })
}

// 修改AI知识库分片
export function updateChunk(data) {
  return request({
    url: '/system/chunk',
    method: 'put',
    data: data
  })
}

// 删除AI知识库分片
export function delChunk(chunkId) {
  return request({
    url: '/system/chunk/' + chunkId,
    method: 'delete'
  })
}
