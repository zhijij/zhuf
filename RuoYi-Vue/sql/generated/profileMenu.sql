-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('户主资料', '3', '1', 'profile', 'system/profile/index', 1, 0, 'C', '0', '0', 'system:profile:list', '#', 'admin', sysdate(), '', null, '户主资料菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('户主资料查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'system:profile:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('户主资料新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'system:profile:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('户主资料修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'system:profile:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('户主资料删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'system:profile:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('户主资料导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'system:profile:export',       '#', 'admin', sysdate(), '', null, '');