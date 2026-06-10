import request from '@/utils/request'

// 查询租赁意向列表
export function listIntention(query) {
  return request({
    url: '/system/intention/list',
    method: 'get',
    params: query
  })
}

// 查询租赁意向详细
export function getIntention(intentionId) {
  return request({
    url: '/system/intention/' + intentionId,
    method: 'get'
  })
}

// 新增租赁意向
export function addIntention(data) {
  return request({
    url: '/system/intention',
    method: 'post',
    data: data
  })
}

// 修改租赁意向
export function updateIntention(data) {
  return request({
    url: '/system/intention',
    method: 'put',
    data: data
  })
}

// 删除租赁意向
export function delIntention(intentionId) {
  return request({
    url: '/system/intention/' + intentionId,
    method: 'delete'
  })
}
