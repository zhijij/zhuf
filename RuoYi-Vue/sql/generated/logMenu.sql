-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI工具调用审计', '3', '1', 'log', 'system/log/index', 1, 0, 'C', '0', '0', 'system:log:list', '#', 'admin', sysdate(), '', null, 'AI工具调用审计菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI工具调用审计查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'system:log:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI工具调用审计新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'system:log:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI工具调用审计修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'system:log:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI工具调用审计删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'system:log:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('AI工具调用审计导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'system:log:export',       '#', 'admin', sysdate(), '', null, '');