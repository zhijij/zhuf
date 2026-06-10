import request from '@/utils/request'

// 查询看房预约列表
export function listAppointment(query) {
  return request({
    url: '/system/appointment/list',
    method: 'get',
    params: query
  })
}

// 查询看房预约详细
export function getAppointment(appointmentId) {
  return request({
    url: '/system/appointment/' + appointmentId,
    method: 'get'
  })
}

// 新增看房预约
export function addAppointment(data) {
  return request({
    url: '/system/appointment',
    method: 'post',
    data: data
  })
}

// 修改看房预约
export function updateAppointment(data) {
  return request({
    url: '/system/appointment',
    method: 'put',
    data: data
  })
}

// 删除看房预约
export function delAppointment(appointmentId) {
  return request({
    url: '/system/appointment/' + appointmentId,
    method: 'delete'
  })
}
