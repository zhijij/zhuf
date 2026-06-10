-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('房源委托关系', '3', '1', 'entrust', 'system/entrust/index', 1, 0, 'C', '0', '0', 'system:entrust:list', '#', 'admin', sysdate(), '', null, '房源委托关系菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('房源委托关系查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'system:entrust:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('房源委托关系新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'system:entrust:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('房源委托关系修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'system:entrust:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('房源委托关系删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'system:entrust:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('房源委托关系导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'system:entrust:export',       '#', 'admin', sysdate(), '', null, '');