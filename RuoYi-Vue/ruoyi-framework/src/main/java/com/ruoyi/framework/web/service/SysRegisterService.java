package com.ruoyi.framework.web.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.RegisterBody;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.user.CaptchaException;
import com.ruoyi.common.exception.user.CaptchaExpireException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.MessageUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.manager.factory.AsyncFactory;
import com.ruoyi.system.domain.RentalOwnerProfile;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.IRentalOwnerProfileService;
import com.ruoyi.system.service.ISysRoleService;
import com.ruoyi.system.service.ISysUserService;

/**
 * 注册校验方法
 * 
 * @author ruoyi
 */
@Component
public class SysRegisterService
{
    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private IRentalOwnerProfileService rentalOwnerProfileService;

    /**
     * 注册
     */
    @Transactional(rollbackFor = Exception.class)
    public String register(RegisterBody registerBody)
    {
        String msg = "", username = registerBody.getUsername(), password = registerBody.getPassword();
        SysUser sysUser = new SysUser();
        sysUser.setUserName(username);

        // 验证码开关
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        if (captchaEnabled)
        {
            validateCaptcha(username, registerBody.getCode(), registerBody.getUuid());
        }

        if (StringUtils.isEmpty(username))
        {
            msg = "用户名不能为空";
        }
        else if (StringUtils.isEmpty(password))
        {
            msg = "用户密码不能为空";
        }
        else if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            msg = "账户长度必须在2到20个字符之间";
        }
        else if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            msg = "密码长度必须在5到20个字符之间";
        }
        else if (!userService.checkUserNameUnique(sysUser))
        {
            msg = "保存用户'" + username + "'失败，注册账号已存在";
        }
        else
        {
            if (StringUtils.isNotEmpty(registerBody.getPhonenumber()))
            {
                sysUser.setPhonenumber(registerBody.getPhonenumber());
                if (!userService.checkPhoneUnique(sysUser))
                {
                    msg = "保存用户'" + username + "'失败，手机号已存在";
                }
            }
            if (StringUtils.isEmpty(msg) && StringUtils.isNotEmpty(registerBody.getEmail()))
            {
                sysUser.setEmail(registerBody.getEmail());
                if (!userService.checkEmailUnique(sysUser))
                {
                    msg = "保存用户'" + username + "'失败，邮箱已存在";
                }
            }
            if (StringUtils.isEmpty(msg) && StringUtils.isEmpty(registerBody.getRegisterRole()))
            {
                msg = "请选择注册身份";
            }
            if (StringUtils.isEmpty(msg) && !isSupportedRegisterRole(registerBody.getRegisterRole()))
            {
                msg = "当前注册身份不受支持";
            }
            if (StringUtils.isEmpty(msg))
            {
                sysUser.setNickName(StringUtils.isNotEmpty(registerBody.getNickName()) ? registerBody.getNickName() : username);
                sysUser.setEmail(registerBody.getEmail());
                sysUser.setPhonenumber(registerBody.getPhonenumber());
                sysUser.setPwdUpdateDate(DateUtils.getNowDate());
                sysUser.setPassword(SecurityUtils.encryptPassword(password));
                Long roleId = resolveRoleId(registerBody.getRegisterRole());
                if (roleId == null)
                {
                    return "注册身份不存在，请先初始化业务角色";
                }
                sysUser.setRoleIds(new Long[] { roleId });
                boolean regFlag = userService.registerUser(sysUser);
                if (!regFlag)
                {
                    msg = "注册失败,请联系系统管理人员";
                }
                else
                {
                    userService.insertUserAuth(sysUser.getUserId(), new Long[] { roleId });
                    if (StringUtils.equals(registerBody.getRegisterRole(), "owner"))
                    {
                        createOwnerProfile(sysUser.getUserId(), registerBody);
                    }
                    AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.REGISTER, MessageUtils.message("user.register.success")));
                }
            }
        }
        return msg;
    }

    /**
     * 校验验证码
     * 
     * @param username 用户名
     * @param code 验证码
     * @param uuid 唯一标识
     * @return 结果
     */
    public void validateCaptcha(String username, String code, String uuid)
    {
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.nvl(uuid, "");
        String captcha = redisCache.getCacheObject(verifyKey);
        redisCache.deleteObject(verifyKey);
        if (captcha == null)
        {
            throw new CaptchaExpireException();
        }
        if (!code.equalsIgnoreCase(captcha))
        {
            throw new CaptchaException();
        }
    }

    private boolean isSupportedRegisterRole(String roleKey)
    {
        return StringUtils.equalsAnyIgnoreCase(roleKey, "user", "owner", "agent");
    }

    private Long resolveRoleId(String roleKey)
    {
        List<SysRole> roles = roleService.selectRoleAll();
        for (SysRole role : roles)
        {
            if (StringUtils.equals(roleKey, role.getRoleKey()))
            {
                return role.getRoleId();
            }
        }
        return null;
    }

    private void createOwnerProfile(Long userId, RegisterBody registerBody)
    {
        RentalOwnerProfile exists = rentalOwnerProfileService.selectRentalOwnerProfileByOwnerId(userId);
        if (exists != null)
        {
            return;
        }
        RentalOwnerProfile profile = new RentalOwnerProfile();
        profile.setOwnerId(userId);
        profile.setRealName(StringUtils.defaultIfEmpty(registerBody.getRealName(), registerBody.getNickName()));
        profile.setContactPhone(registerBody.getPhonenumber());
        profile.setVerifyStatus("0");
        profile.setVerifyReason("注册创建，待补充完整户主实名资料");
        rentalOwnerProfileService.insertRentalOwnerProfile(profile);
    }
}
