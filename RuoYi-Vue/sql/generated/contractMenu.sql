-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('租赁合同', '3', '1', 'contract', 'system/contract/index', 1, 0, 'C', '0', '0', 'system:contract:list', '#', 'admin', sysdate(), '', null, '租赁合同菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('租赁合同查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'system:contract:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('租赁合同新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'system:contract:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('租赁合同修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'system:contract:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('租赁合同删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'system:contract:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('租赁合同导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'system:contract:export',       '#', 'admin', sysdate(), '', null, '');