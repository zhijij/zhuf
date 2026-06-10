-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('租赁意向', '3', '1', 'intention', 'system/intention/index', 1, 0, 'C', '0', '0', 'system:intention:list', '#', 'admin', sysdate(), '', null, '租赁意向菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('租赁意向查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'system:intention:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('租赁意向新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'system:intention:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('租赁意向修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'system:intention:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('租赁意向删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'system:intention:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('租赁意向导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'system:intention:export',       '#', 'admin', sysdate(), '', null, '');