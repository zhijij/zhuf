-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI用户记忆', '3', '1', 'memory', 'system/memory/index', 1, 0, 'C', '0', '0', 'system:memory:list', '#', 'admin', sysdate(), '', null, 'AI用户记忆菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI用户记忆查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'system:memory:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI用户记忆新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'system:memory:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI用户记忆修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'system:memory:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI用户记忆删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'system:memory:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI用户记忆导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'system:memory:export',       '#', 'admin', sysdate(), '', null, '');