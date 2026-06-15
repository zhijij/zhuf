export const transactionConfigs = {
  ownerCreateHouse: { title: '新建房源', submitText: '提交审核' },
  ownerSubmitAudit: { title: '重新提交审核', submitText: '重新提交', form: 'none' },
  tenantAppointment: { title: '预约看房', submitText: '提交预约' },
  tenantIntention: { title: '提交租赁意向', submitText: '提交意向' },
  tenantDeal: { title: '成交申请', submitText: '提交申请', form: 'deal' },
  ownerEntrust: { title: '发起委托', submitText: '发送邀请' },
  agentApplyEntrust: { title: '申请承接', submitText: '提交申请' },
  agentConfirmAppointment: { title: '确认预约', submitText: '确认预约', form: 'none' },
  agentCompleteAppointment: { title: '完成看房', submitText: '完成看房', form: 'none' },
  agentRejectAppointment: { title: '拒绝预约', submitText: '拒绝预约', form: 'reason', reasonLabel: '拒绝原因' },
  agentFollowIntention: { title: '跟进意向', submitText: '保存跟进' },
  agentInvalidIntention: { title: '标记意向无效', submitText: '标记无效', form: 'reason', reasonLabel: '无效原因' },
  agentDealIntention: { title: '意向转成交', submitText: '确认成交', form: 'deal' },
  agentDealHouse: { title: '确认房源成交', submitText: '确认成交', form: 'deal' },
  contractSubmitSign: { title: '提交合同签署', submitText: '提交签署', form: 'none' },
  contractTenantConfirm: { title: '租户确认合同', submitText: '确认合同', form: 'opinion' },
  contractOwnerConfirm: { title: '户主确认合同', submitText: '确认合同', form: 'opinion' },
  contractAgentConfirm: { title: '中介确认合同', submitText: '确认合同', form: 'opinion' },
  contractReject: { title: '拒绝合同', submitText: '拒绝合同', form: 'opinion' },
  contractActivate: { title: '合同生效', submitText: '确认生效', form: 'none' },
  contractVoid: { title: '作废合同', submitText: '作废合同', form: 'reason', reasonLabel: '作废原因' },
  contractTerminate: { title: '终止合同', submitText: '终止合同', form: 'reason', reasonLabel: '终止原因' },
  contractOpinion: { title: '合同确认', submitText: '提交', form: 'opinion' },
  adminApproveHouse: { title: '房源合规审核', submitText: '通过并发布', form: 'audit' },
  adminRejectHouse: { title: '驳回房源', submitText: '确认驳回', form: 'audit' }
}

export const roleConfig = {
  admin: { title: '房源合规审核', eyebrow: '审核队列', actionTitle: '审核动作', actionHint: '审核员只处理房源合规性：通过后发布，驳回后记录原因。' },
  tenant: { title: '租户找房', eyebrow: '推荐房源', actionTitle: '求租动作', actionHint: '收藏、预约、提交意向都在这里完成。' },
  owner: { title: '户主委托', eyebrow: '我的房源', actionTitle: '委托动作', actionHint: '处理中介申请，或指定中介发起委托。' },
  agent: { title: '中介拓客', eyebrow: '可承接房源', actionTitle: '承接动作', actionHint: '响应户主委托，也可以主动申请承接。' },
  contract: { title: '合同协作', eyebrow: '合同队列', actionTitle: '合同动作', actionHint: '推进三方确认，拒绝时请填写处理意见。' }
}

export const modeMeta = {
  admin: { label: '合规审核', shortLabel: '审核' },
  tenant: { label: '租户找房', shortLabel: '找房' },
  owner: { label: '户主房源', shortLabel: '房源' },
  agent: { label: '中介业务', shortLabel: '业务' },
  contract: { label: '合同协作', shortLabel: '合同' }
}

export const quickTags = ['整租', '可沟通', '待处理', '合同', '中介申请', '推荐房源']

export const floatingAiPresets = ['总结当前业务', '生成房源文案', '梳理合同风险']

export const businessAiCards = {
  admin: {
    title: 'AI 辅助合规审查',
    description: '结合房源字段生成审核要点，结果只写入审核意见，最终通过或驳回仍由审核员确认。',
    actionLabel: '生成审核意见'
  },
  tenant: {
    title: 'AI 推荐房源',
    description: '按预算、位置和当前房源上下文生成推荐摘要，房源详情仍从业务接口读取。',
    actionLabel: '生成推荐'
  },
  owner: {
    title: 'AI 房源文案',
    description: '根据当前房源生成发布描述或委托沟通话术，创建和提交仍通过业务表单完成。',
    actionLabel: '生成文案'
  },
  agent: {
    title: 'AI 跟进建议',
    description: '根据当前委托、预约或意向生成下一步跟进话术，不自动变更业务状态。',
    actionLabel: '生成建议'
  },
  contract: {
    title: 'AI 合同风险提示',
    description: '提取合同风险点和确认意见草稿，签署、拒绝、生效仍走合同接口。',
    actionLabel: '梳理风险'
  }
}

export const aiAssistantProfiles = {
  tenant: {
    badge: '租户 AI 助手',
    eyebrow: '找房协作',
    title: '租户智能找房助手',
    summary: '围绕预算、通勤、户型和合同风险提供建议，帮你更快做出看房和意向决策。',
    placeholder: '例如：预算 4500，想找通勤方便的两居，顺便看看当前房源值不值得约看',
    presets: ['按预算推荐房源', '分析当前房源是否值得预约', '梳理合同风险'],
    capabilities: ['预算匹配', '看房建议', '合同风险', '沟通问题清单']
  },
  owner: {
    badge: '户主 AI 助手',
    eyebrow: '发布协作',
    title: '户主房源经营助手',
    summary: '围绕房源发布、委托中介、审核补充和合同协作，帮你把业务动作做得更完整。',
    placeholder: '例如：帮我润色当前房源文案，或者告诉我这套房源还缺哪些审核信息',
    presets: ['润色当前房源文案', '检查当前房源审核缺口', '生成委托沟通话术'],
    capabilities: ['房源文案', '审核补充', '委托沟通', '合同确认']
  },
  agent: {
    badge: '中介 AI 助手',
    eyebrow: '成交推进',
    title: '中介业务跟进助手',
    summary: '围绕可承接房源、客户预约、意向推进和签约沟通，给你更贴近业务的下一步建议。',
    placeholder: '例如：帮我给当前客户生成跟进话术，或者判断这条意向下一步该怎么推进',
    presets: ['生成当前业务跟进话术', '总结当前客户下一步动作', '梳理合同签约风险'],
    capabilities: ['跟进话术', '预约推进', '意向转化', '签约提醒']
  },
  contract: {
    badge: '合同 AI 助手',
    eyebrow: '签约协作',
    title: '合同确认与风险助手',
    summary: '聚焦租期、押金、付款周期、交付清单和违约责任，帮你在确认前看清主要风险。',
    placeholder: '例如：帮我看这份合同还缺什么，或者当前合同应该提醒双方注意哪些点',
    presets: ['梳理当前合同风险', '生成合同确认意见', '总结当前合同下一步动作'],
    capabilities: ['风险提炼', '确认意见', '交付检查', '违约提醒']
  },
  admin: {
    badge: '审核 AI 助手',
    eyebrow: '合规辅助',
    title: '审核意见辅助助手',
    summary: '只为审核员服务，聚焦房源字段完整性、风险词、审核意见草稿和索引任务提醒。',
    placeholder: '例如：请结合当前房源生成审核意见，并指出必须补充的字段',
    presets: ['生成当前房源审核意见', '检查当前房源硬性问题', '说明索引状态与处理建议'],
    capabilities: ['字段审查', '风险提示', '审核意见', '索引提醒']
  }
}

export const knowledgeSourceLabels = {
  house: '房源',
  contract: '合同',
  policy: '政策',
  faq: 'FAQ',
  chat: '聊天记录',
  enterprise: '企业制度'
}
