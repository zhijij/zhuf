export function formatValue(value) {
  return value === undefined || value === null || value === '' ? '-' : value
}

export function formatMoney(value) {
  return value === undefined || value === null || value === '' ? '-' : `${value} 元/月`
}

export function formatPercent(value) {
  if (value === undefined || value === null || value === '') return '-'
  const number = Number(value)
  if (Number.isNaN(number)) return value
  return `${(number * 100).toFixed(2)}%`
}

export function intentionLevelLabel(value) {
  const map = { 1: '低', 2: '中', 3: '高' }
  return map[String(value)] || value || '-'
}

export function taskStatusLabel(status) {
  const map = {
    0: '待处理',
    1: '处理中',
    2: '已完成',
    3: '失败',
    pending: '待处理',
    running: '处理中',
    success: '已完成',
    failed: '失败'
  }
  return map[status] || status || '未设置'
}

export function aiIndexStatusLabel(status, indexed = false) {
  if (indexed || ['1', 1].includes(status)) return '已索引'
  if (['2', 2].includes(status)) return '索引失败'
  return '未索引'
}

export function normalizeAiResponse(response) {
  const data = normalizeAiResponsePayload(response)
  if (typeof data === 'string') {
    return { answer: data, toolCalls: [], suggestions: null }
  }
  return {
    answer: data?.answer || data?.msg || '智能体接口已收到请求。',
    intent: data?.intent || '',
    intentLabel: data?.intentLabel || '',
    toolCalls: data?.toolCalls || [],
    suggestions: data?.suggestions || null,
    nextActions: data?.nextActions || []
  }
}

export function normalizeAiResponsePayload(response) {
  const data = response?.data && typeof response.data === 'object' ? response.data : response
  if (data?.data && typeof data.data === 'object') {
    return data.data
  }
  return data || {}
}
