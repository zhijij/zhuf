-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('房源收藏', '3', '1', 'favorite', 'system/favorite/index', 1, 0, 'C', '0', '0', 'system:favorite:list', '#', 'admin', sysdate(), '', null, '房源收藏菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('房源收藏查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'system:favorite:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('房源收藏新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'system:favorite:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('房源收藏修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'system:favorite:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('房源收藏删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'system:favorite:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('房源收藏导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'system:favorite:export',       '#', 'admin', sysdate(), '', null, '');