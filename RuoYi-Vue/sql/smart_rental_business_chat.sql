-- Smart rental business roles, test accounts, and reusable business chat.
-- Execute this after ry_20260417.sql and smart_rental_schema.sql in the same ry-vue database.

UPDATE sys_config
SET config_value = 'true',
    update_by = 'admin',
    update_time = SYSDATE()
WHERE config_key = 'sys.account.registerUser';

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

INSERT INTO sys_role
    (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly,
     status, del_flag, create_by, create_time, update_by, update_time, remark)
SELECT '房源审核员', 'auditor', 6, '2', 1, 1, '0', '0', 'admin', SYSDATE(), '', NULL, '房源合规审核员角色'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'auditor');

UPDATE sys_role
SET role_name = '房源审核员',
    remark = '房源合规审核员角色'
WHERE role_key = 'auditor';

-- Demo users. Default password is admin123.
INSERT INTO sys_user
    (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password,
     status, del_flag, login_ip, login_date, pwd_update_date, create_by, create_time,
     update_by, update_time, remark)
SELECT 105, 'tenant_test', '演示租户', '00', 'tenant@test.local', '15000000001', '0', '',
       '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2',
       '0', '0', '127.0.0.1', SYSDATE(), SYSDATE(), 'admin', SYSDATE(), '', NULL, '租户演示账号'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name = 'tenant_test');

INSERT INTO sys_user
    (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password,
     status, del_flag, login_ip, login_date, pwd_update_date, create_by, create_time,
     update_by, update_time, remark)
SELECT 105, 'owner_test', '演示户主', '00', 'owner@test.local', '15000000002', '0', '',
       '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2',
       '0', '0', '127.0.0.1', SYSDATE(), SYSDATE(), 'admin', SYSDATE(), '', NULL, '户主演示账号'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name = 'owner_test');

INSERT INTO sys_user
    (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password,
     status, del_flag, login_ip, login_date, pwd_update_date, create_by, create_time,
     update_by, update_time, remark)
SELECT 105, 'agent_test', '演示中介', '00', 'agent@test.local', '15000000003', '0', '',
       '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2',
       '0', '0', '127.0.0.1', SYSDATE(), SYSDATE(), 'admin', SYSDATE(), '', NULL, '中介演示账号'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name = 'agent_test');

INSERT INTO sys_user
    (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password,
     status, del_flag, login_ip, login_date, pwd_update_date, create_by, create_time,
     update_by, update_time, remark)
SELECT 105, 'auditor_test', '房源审核员', '00', 'auditor@test.local', '15000000004', '0', '',
       '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2',
       '0', '0', '127.0.0.1', SYSDATE(), SYSDATE(), 'admin', SYSDATE(), '', NULL, '房源审核员演示账号'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name = 'auditor_test');

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM sys_user u
JOIN sys_role r ON r.role_key = 'user'
WHERE u.user_name = 'tenant_test'
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.user_id AND ur.role_id = r.role_id
  );

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM sys_user u
JOIN sys_role r ON r.role_key = 'owner'
WHERE u.user_name = 'owner_test'
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.user_id AND ur.role_id = r.role_id
  );

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM sys_user u
JOIN sys_role r ON r.role_key = 'agent'
WHERE u.user_name = 'agent_test'
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.user_id AND ur.role_id = r.role_id
  );

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM sys_user u
JOIN sys_role r ON r.role_key = 'auditor'
WHERE u.user_name = 'auditor_test'
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.user_id AND ur.role_id = r.role_id
  );

DELETE rm
FROM sys_role_menu rm
JOIN sys_role r ON r.role_id = rm.role_id
JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE r.role_key = 'auditor'
  AND (
    m.perms LIKE 'system:doc:%'
    OR m.perms LIKE 'system:chunk:%'
    OR m.perms LIKE 'system:task:%'
    OR m.perms LIKE 'system:memory:%'
    OR m.perms LIKE 'system:session:%'
    OR m.perms LIKE 'system:message:%'
    OR m.perms LIKE 'system:log:%'
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON (
  m.perms LIKE 'system:doc:%'
  OR m.perms LIKE 'system:chunk:%'
  OR m.perms LIKE 'system:task:%'
  OR m.perms LIKE 'system:memory:%'
  OR m.perms LIKE 'system:session:%'
  OR m.perms LIKE 'system:message:%'
  OR m.perms LIKE 'system:log:%'
)
WHERE r.role_key = 'admin'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.perms IN ('system:house:audit')
WHERE r.role_key = 'auditor'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id
  );

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
