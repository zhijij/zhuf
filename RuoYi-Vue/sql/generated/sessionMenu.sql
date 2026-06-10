-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI会话', '3', '1', 'session', 'system/session/index', 1, 0, 'C', '0', '0', 'system:session:list', '#', 'admin', sysdate(), '', null, 'AI会话菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI会话查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'system:session:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI会话新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'system:session:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI会话修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'system:session:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI会话删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'system:session:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI会话导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'system:session:export',       '#', 'admin', sysdate(), '', null, '');