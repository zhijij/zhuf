-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('租户租房偏好', '3', '1', 'preference', 'system/preference/index', 1, 0, 'C', '0', '0', 'system:preference:list', '#', 'admin', sysdate(), '', null, '租户租房偏好菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('租户租房偏好查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'system:preference:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('租户租房偏好新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'system:preference:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('租户租房偏好修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'system:preference:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('租户租房偏好删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'system:preference:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('租户租房偏好导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'system:preference:export',       '#', 'admin', sysdate(), '', null, '');