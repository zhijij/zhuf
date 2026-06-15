-- 智能租赁系统完整建表SQL
-- 执行库：ry-vue

CREATE TABLE IF NOT EXISTS rental_owner_profile (
  owner_id BIGINT NOT NULL COMMENT '户主用户ID，对应sys_user.user_id',
  real_name VARCHAR(64) DEFAULT NULL COMMENT '真实姓名',
  id_card_no VARCHAR(64) DEFAULT NULL COMMENT '证件号，可加密存储',
  contact_phone VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
  bank_account VARCHAR(128) DEFAULT NULL COMMENT '收款账户，可加密存储',
  verify_status CHAR(1) DEFAULT '0' COMMENT '认证状态:0未认证,1待审核,2已认证,3拒绝',
  verify_reason VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='户主资料表';

CREATE TABLE IF NOT EXISTS rental_tenant_profile (
  tenant_id BIGINT NOT NULL COMMENT '租户用户ID，对应sys_user.user_id',
  nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
  gender CHAR(1) DEFAULT NULL COMMENT '性别',
  contact_phone VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
  preferred_city VARCHAR(64) DEFAULT NULL COMMENT '常驻城市',
  budget_min DECIMAL(10,2) DEFAULT NULL COMMENT '预算下限',
  budget_max DECIMAL(10,2) DEFAULT NULL COMMENT '预算上限',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户资料表';

CREATE TABLE IF NOT EXISTS rental_house (
  house_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '房源ID',
  owner_id BIGINT NOT NULL COMMENT '户主用户ID',
  agent_id BIGINT DEFAULT NULL COMMENT '中介用户ID，自主出租时为空',
  operation_mode CHAR(1) DEFAULT '0' COMMENT '运营方式:0户主自主出租,1委托中介',
  title VARCHAR(120) NOT NULL COMMENT '房源标题',
  city VARCHAR(64) NOT NULL COMMENT '城市',
  district VARCHAR(64) NOT NULL COMMENT '区域',
  street VARCHAR(128) DEFAULT NULL COMMENT '街道',
  community VARCHAR(128) DEFAULT NULL COMMENT '小区',
  address VARCHAR(255) DEFAULT NULL COMMENT '详细地址',
  longitude DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
  latitude DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
  rent_amount DECIMAL(10,2) NOT NULL COMMENT '月租金',
  deposit_amount DECIMAL(10,2) DEFAULT NULL COMMENT '押金',
  area DECIMAL(8,2) DEFAULT NULL COMMENT '面积',
  room_count INT DEFAULT NULL COMMENT '室',
  hall_count INT DEFAULT NULL COMMENT '厅',
  toilet_count INT DEFAULT NULL COMMENT '卫',
  floor_no INT DEFAULT NULL COMMENT '所在楼层',
  total_floor INT DEFAULT NULL COMMENT '总楼层',
  orientation VARCHAR(32) DEFAULT NULL COMMENT '朝向',
  rent_type CHAR(1) DEFAULT '0' COMMENT '出租方式:0整租,1合租',
  decoration VARCHAR(32) DEFAULT NULL COMMENT '装修',
  facilities VARCHAR(1000) DEFAULT NULL COMMENT '配套设施JSON或逗号分隔',
  tags VARCHAR(500) DEFAULT NULL COMMENT '标签',
  description TEXT COMMENT '房源描述',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态:0草稿,1待审核,2已发布,3驳回,4已出租,5下架',
  audit_status CHAR(1) DEFAULT '0' COMMENT '审核状态:0未提交,1待审,2通过,3拒绝',
  audit_reason VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
  view_count INT DEFAULT 0 COMMENT '浏览次数',
  favorite_count INT DEFAULT 0 COMMENT '收藏次数',
  ai_index_status CHAR(1) DEFAULT '0' COMMENT '向量索引状态:0未索引,1已索引,2失败',
  del_flag CHAR(1) DEFAULT '0' COMMENT '删除标志',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (house_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租赁房源表';

CREATE TABLE IF NOT EXISTS rental_house_image (
  image_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  house_id BIGINT NOT NULL COMMENT '房源ID',
  image_url VARCHAR(500) NOT NULL COMMENT '图片地址',
  image_type CHAR(1) DEFAULT '0' COMMENT '类型:0普通,1封面,2户型图',
  sort_no INT DEFAULT 0 COMMENT '排序',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (image_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房源图片表';

CREATE TABLE IF NOT EXISTS rental_house_entrust (
  entrust_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '委托ID',
  house_id BIGINT NOT NULL COMMENT '房源ID',
  owner_id BIGINT NOT NULL COMMENT '户主用户ID',
  agent_id BIGINT NOT NULL COMMENT '中介用户ID',
  entrust_scope VARCHAR(500) DEFAULT NULL COMMENT '委托范围',
  commission_rate DECIMAL(5,2) DEFAULT NULL COMMENT '佣金比例',
  start_date DATE DEFAULT NULL COMMENT '委托开始日期',
  end_date DATE DEFAULT NULL COMMENT '委托结束日期',
  status CHAR(1) DEFAULT '0' COMMENT '状态:0待确认,1生效中,2已拒绝,3已终止,4已过期',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (entrust_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房源委托关系表';

CREATE TABLE IF NOT EXISTS rental_house_favorite (
  favorite_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  house_id BIGINT NOT NULL COMMENT '房源ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (favorite_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房源收藏表';

CREATE TABLE IF NOT EXISTS rental_tenant_preference (
  preference_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '偏好ID',
  user_id BIGINT NOT NULL COMMENT '租户用户ID',
  city VARCHAR(64) DEFAULT NULL COMMENT '意向城市',
  districts VARCHAR(500) DEFAULT NULL COMMENT '意向区域',
  min_rent DECIMAL(10,2) DEFAULT NULL COMMENT '最低预算',
  max_rent DECIMAL(10,2) DEFAULT NULL COMMENT '最高预算',
  room_type VARCHAR(64) DEFAULT NULL COMMENT '户型偏好',
  commute_target VARCHAR(255) DEFAULT NULL COMMENT '通勤目标',
  commute_minutes INT DEFAULT NULL COMMENT '期望通勤分钟',
  required_tags VARCHAR(500) DEFAULT NULL COMMENT '必须标签',
  preferred_tags VARCHAR(500) DEFAULT NULL COMMENT '偏好标签',
  avoid_tags VARCHAR(500) DEFAULT NULL COMMENT '避雷标签',
  source CHAR(1) DEFAULT '0' COMMENT '来源:0用户填写,1AI提取',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (preference_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户租房偏好表';

CREATE TABLE IF NOT EXISTS rental_appointment (
  appointment_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '预约ID',
  house_id BIGINT NOT NULL COMMENT '房源ID',
  tenant_id BIGINT NOT NULL COMMENT '租户用户ID',
  owner_id BIGINT NOT NULL COMMENT '户主用户ID',
  agent_id BIGINT DEFAULT NULL COMMENT '中介用户ID，自主出租时为空',
  appointment_time DATETIME NOT NULL COMMENT '预约时间',
  contact_name VARCHAR(64) DEFAULT NULL COMMENT '联系人',
  contact_phone VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
  message VARCHAR(500) DEFAULT NULL COMMENT '留言',
  status CHAR(1) DEFAULT '0' COMMENT '状态:0待确认,1已确认,2已完成,3已取消,4已拒绝',
  cancel_reason VARCHAR(500) DEFAULT NULL COMMENT '取消/拒绝原因',
  source CHAR(1) DEFAULT '0' COMMENT '来源:0手动,1AI助手',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (appointment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='看房预约表';

CREATE TABLE IF NOT EXISTS rental_intention (
  intention_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '意向ID',
  house_id BIGINT NOT NULL COMMENT '房源ID',
  tenant_id BIGINT NOT NULL COMMENT '租户用户ID',
  owner_id BIGINT NOT NULL COMMENT '户主用户ID',
  agent_id BIGINT DEFAULT NULL COMMENT '中介用户ID，自主出租时为空',
  intention_level CHAR(1) DEFAULT '1' COMMENT '意向等级:1低,2中,3高',
  status CHAR(1) DEFAULT '0' COMMENT '状态:0跟进中,1已成交,2无效,3放弃',
  budget_amount DECIMAL(10,2) DEFAULT NULL COMMENT '预算',
  expected_move_in DATE DEFAULT NULL COMMENT '期望入住时间',
  note VARCHAR(1000) DEFAULT NULL COMMENT '备注',
  ai_summary VARCHAR(1000) DEFAULT NULL COMMENT 'AI意向摘要',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (intention_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租赁意向表';

CREATE TABLE IF NOT EXISTS rental_contract (
  contract_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '合同ID',
  house_id BIGINT NOT NULL COMMENT '房源ID',
  tenant_id BIGINT NOT NULL COMMENT '租户用户ID',
  owner_id BIGINT NOT NULL COMMENT '户主用户ID',
  agent_id BIGINT DEFAULT NULL COMMENT '中介用户ID，自主出租时为空',
  contract_no VARCHAR(64) NOT NULL COMMENT '合同编号',
  start_date DATE NOT NULL COMMENT '租期开始',
  end_date DATE NOT NULL COMMENT '租期结束',
  rent_amount DECIMAL(10,2) NOT NULL COMMENT '月租金',
  deposit_amount DECIMAL(10,2) DEFAULT NULL COMMENT '押金',
  payment_cycle VARCHAR(32) DEFAULT NULL COMMENT '付款周期',
  contract_content MEDIUMTEXT COMMENT '合同内容',
  ai_risk_summary TEXT COMMENT 'AI风险摘要',
  status CHAR(1) DEFAULT '0' COMMENT '状态:0草稿,1待签,2生效,3终止,4作废',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (contract_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租赁合同表';

CREATE TABLE IF NOT EXISTS rental_contract_confirm (
  confirm_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '确认ID',
  contract_id BIGINT NOT NULL COMMENT '合同ID',
  user_id BIGINT NOT NULL COMMENT '确认人用户ID',
  user_role VARCHAR(32) NOT NULL COMMENT '确认角色:tenant,owner,agent',
  confirm_status CHAR(1) DEFAULT '0' COMMENT '确认状态:0未确认,1已确认,2已拒绝',
  confirm_opinion VARCHAR(500) DEFAULT NULL COMMENT '确认意见',
  confirm_time DATETIME DEFAULT NULL COMMENT '确认时间',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (confirm_id),
  UNIQUE KEY uk_contract_role (contract_id, user_role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租赁合同确认表';

CREATE TABLE IF NOT EXISTS ai_chat_session (
  session_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role VARCHAR(32) NOT NULL COMMENT '用户角色',
  title VARCHAR(200) DEFAULT NULL COMMENT '会话标题',
  biz_type VARCHAR(64) DEFAULT NULL COMMENT '业务类型',
  status CHAR(1) DEFAULT '0' COMMENT '状态:0正常,1关闭',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI会话表';

CREATE TABLE IF NOT EXISTS ai_chat_message (
  message_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  session_id BIGINT NOT NULL COMMENT '会话ID',
  user_id BIGINT DEFAULT NULL COMMENT '用户ID',
  sender_type VARCHAR(32) NOT NULL COMMENT '发送方:user/assistant/tool/system',
  content MEDIUMTEXT COMMENT '消息内容',
  tool_name VARCHAR(128) DEFAULT NULL COMMENT '工具名称',
  tool_args JSON DEFAULT NULL COMMENT '工具参数',
  tool_result JSON DEFAULT NULL COMMENT '工具结果',
  token_count INT DEFAULT NULL COMMENT 'Token数量',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI消息表';

CREATE TABLE IF NOT EXISTS ai_user_memory (
  memory_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '记忆ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role VARCHAR(32) NOT NULL COMMENT '角色',
  memory_type VARCHAR(32) NOT NULL COMMENT '类型:preference/fact/summary',
  content VARCHAR(2000) NOT NULL COMMENT '记忆内容',
  importance DECIMAL(5,2) DEFAULT 0 COMMENT '重要度',
  vector_status CHAR(1) DEFAULT '0' COMMENT '向量状态:0未索引,1已索引,2失败',
  expires_at DATETIME DEFAULT NULL COMMENT '过期时间',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (memory_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI用户记忆表';

CREATE TABLE IF NOT EXISTS ai_knowledge_doc (
  doc_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '文档ID',
  title VARCHAR(200) NOT NULL COMMENT '标题',
  category VARCHAR(64) NOT NULL COMMENT '分类:faq/policy/contract/platform_rule',
  content MEDIUMTEXT NOT NULL COMMENT '内容',
  version VARCHAR(32) DEFAULT '1.0' COMMENT '版本',
  enabled CHAR(1) DEFAULT '1' COMMENT '是否启用',
  vector_status CHAR(1) DEFAULT '0' COMMENT '向量状态',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (doc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI知识库文档表';

CREATE TABLE IF NOT EXISTS ai_knowledge_chunk (
  chunk_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '分片ID',
  doc_id BIGINT NOT NULL COMMENT '文档ID',
  chunk_no INT NOT NULL COMMENT '分片序号',
  content TEXT NOT NULL COMMENT '分片内容',
  token_count INT DEFAULT NULL COMMENT 'Token数量',
  vector_status CHAR(1) DEFAULT '0' COMMENT '向量状态',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (chunk_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI知识库分片表';

CREATE TABLE IF NOT EXISTS ai_vector_index_task (
  task_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  source_type VARCHAR(64) NOT NULL COMMENT '来源类型:house/knowledge/memory',
  source_id BIGINT NOT NULL COMMENT '来源ID',
  action VARCHAR(32) NOT NULL COMMENT '动作:upsert/delete',
  status CHAR(1) DEFAULT '0' COMMENT '状态:0待处理,1处理中,2成功,3失败',
  retry_count INT DEFAULT 0 COMMENT '重试次数',
  error_msg VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI向量索引任务表';

CREATE TABLE IF NOT EXISTS ai_tool_audit_log (
  log_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  request_id VARCHAR(64) DEFAULT NULL COMMENT '请求ID',
  session_id BIGINT DEFAULT NULL COMMENT '会话ID',
  user_id BIGINT DEFAULT NULL COMMENT '用户ID',
  role VARCHAR(32) DEFAULT NULL COMMENT '角色',
  tool_name VARCHAR(128) NOT NULL COMMENT '工具名称',
  tool_args JSON DEFAULT NULL COMMENT '工具参数',
  tool_result JSON DEFAULT NULL COMMENT '工具结果',
  success CHAR(1) DEFAULT '1' COMMENT '是否成功',
  error_msg VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
  cost_ms INT DEFAULT NULL COMMENT '耗时毫秒',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI工具调用审计表';

-- 普通管理员/审核员：只负责房源合规审核，不进入超级管理员业务逻辑。
INSERT INTO sys_role
    (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly,
     status, del_flag, create_by, create_time, update_by, update_time, remark)
SELECT '普通管理员/审核员', 'auditor', 6, '2', 1, 1, '0', '0', 'admin', SYSDATE(), '', NULL, '房源合规审核员角色'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'auditor');

-- 审核员演示账号。默认密码：admin123。
INSERT INTO sys_user
    (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password,
     status, del_flag, login_ip, login_date, pwd_update_date, create_by, create_time,
     update_by, update_time, remark)
SELECT 105, 'auditor_test', '房源审核员', '00', 'auditor@test.local', '15000000004', '0', '',
       '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2',
       '0', '0', '127.0.0.1', SYSDATE(), SYSDATE(), 'admin', SYSDATE(), '', NULL, '普通管理员/房源审核员账号'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name = 'auditor_test');

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM sys_user u
JOIN sys_role r ON r.role_key = 'auditor'
WHERE u.user_name = 'auditor_test'
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.user_id AND ur.role_id = r.role_id
  );

-- 房源合法性审核按钮权限：分配给普通管理员角色使用，超级管理员只监督不执行业务审核。
INSERT INTO sys_menu (
  menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  '房源合法性审核', m.menu_id, '7', '', '', '', '',
  1, 0, 'F', '0', '0', 'system:house:audit', '#',
  'admin', sysdate(), '', null, '普通管理员执行房源合法性审核，超级管理员仅监督'
FROM sys_menu m
WHERE m.perms = 'system:house:list'
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu x WHERE x.perms = 'system:house:audit'
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.perms = 'system:house:audit'
WHERE r.role_key = 'auditor'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id
  );
