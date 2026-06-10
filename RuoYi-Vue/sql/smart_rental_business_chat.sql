-- Smart rental business roles and reusable business chat.
-- Execute this after ry_20260417.sql and smart_rental_schema.sql in the same ry-vue database.

-- Business roles. RuoYi super admin keeps role_key = 'admin'.
INSERT INTO sys_role
    (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly,
     status, del_flag, create_by, create_time, update_by, update_time, remark)
SELECT '普通租户', 'user', 3, '2', 1, 1, '0', '0', 'admin', SYSDATE(), '', NULL, '租户用户端角色'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'user');

INSERT INTO sys_role
    (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly,
     status, del_flag, create_by, create_time, update_by, update_time, remark)
SELECT '户主', 'owner', 4, '2', 1, 1, '0', '0', 'admin', SYSDATE(), '', NULL, '房源户主角色'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'owner');

INSERT INTO sys_role
    (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly,
     status, del_flag, create_by, create_time, update_by, update_time, remark)
SELECT '中介', 'agent', 5, '2', 1, 1, '0', '0', 'admin', SYSDATE(), '', NULL, '房源中介角色'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'agent');

CREATE TABLE IF NOT EXISTS biz_chat_session (
  session_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '业务聊天会话ID',
  biz_type VARCHAR(32) NOT NULL COMMENT '业务类型:entrust/appointment/intention/contract',
  biz_id BIGINT NOT NULL COMMENT '业务主键ID',
  title VARCHAR(200) DEFAULT NULL COMMENT '会话标题',
  status CHAR(1) DEFAULT '0' COMMENT '状态:0正常,1关闭',
  last_message VARCHAR(500) DEFAULT NULL COMMENT '最后一条消息摘要',
  last_message_time DATETIME DEFAULT NULL COMMENT '最后消息时间',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (session_id),
  UNIQUE KEY uk_biz_chat_session_biz (biz_type, biz_id),
  KEY idx_biz_chat_session_last_time (last_message_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务聊天会话表';

CREATE TABLE IF NOT EXISTS biz_chat_session_user (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '成员ID',
  session_id BIGINT NOT NULL COMMENT '业务聊天会话ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  user_role VARCHAR(32) DEFAULT NULL COMMENT '业务角色:tenant/owner/agent/admin',
  unread_count INT DEFAULT 0 COMMENT '未读数量',
  last_read_message_id BIGINT DEFAULT NULL COMMENT '最后已读消息ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_biz_chat_session_user (session_id, user_id),
  KEY idx_biz_chat_user (user_id),
  CONSTRAINT fk_biz_chat_session_user_session
    FOREIGN KEY (session_id) REFERENCES biz_chat_session(session_id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务聊天成员表';

CREATE TABLE IF NOT EXISTS biz_chat_message (
  message_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  session_id BIGINT NOT NULL COMMENT '业务聊天会话ID',
  sender_id BIGINT NOT NULL COMMENT '发送人用户ID',
  message_type VARCHAR(32) DEFAULT 'text' COMMENT '消息类型:text/image/file/system',
  content MEDIUMTEXT NOT NULL COMMENT '消息内容',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (message_id),
  KEY idx_biz_chat_message_session (session_id, message_id),
  CONSTRAINT fk_biz_chat_message_session
    FOREIGN KEY (session_id) REFERENCES biz_chat_session(session_id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务聊天消息表';
