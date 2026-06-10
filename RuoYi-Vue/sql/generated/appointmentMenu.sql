-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('看房预约', '3', '1', 'appointment', 'system/appointment/index', 1, 0, 'C', '0', '0', 'system:appointment:list', '#', 'admin', sysdate(), '', null, '看房预约菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('看房预约查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'system:appointment:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('看房预约新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'system:appointment:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('看房预约修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'system:appointment:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('看房预约删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'system:appointment:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('看房预约导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'system:appointment:export',       '#', 'admin', sysdate(), '', null, '');