package com.ruoyi.web.controller.rental;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.BizChatSession;
import com.ruoyi.system.domain.RentalContract;
import com.ruoyi.system.domain.dto.RentalContractConfirmRequest;
import com.ruoyi.system.domain.dto.RentalAppointmentActionRequest;
import com.ruoyi.system.enums.RentalContractPartyRole;
import com.ruoyi.system.service.IBizChatService;
import com.ruoyi.system.service.IRentalContractService;

/**
 * Contract collaboration APIs.
 */
@RestController
@RequestMapping("/rental/contract")
public class RentalContractBusinessController extends BaseController
{
    @Autowired
    private IRentalContractService rentalContractService;

    @Autowired
    private IBizChatService bizChatService;

    @PreAuthorize("@ss.hasAnyExactRoles('user,tenant,owner,agent')")
    @GetMapping("/my")
    public AjaxResult listMyContracts(RentalContract query)
    {
        return AjaxResult.success(rentalContractService.selectMyRentalContractList(getUserId()));
    }

    @PreAuthorize("@ss.hasAnyExactRoles('user,tenant,owner,agent')")
    @GetMapping("/{contractId}")
    public AjaxResult detail(@PathVariable Long contractId)
    {
        return AjaxResult.success(rentalContractService.selectRentalContractDetail(contractId, getUserId(), false));
    }

    @PreAuthorize("@ss.hasAnyExactRoles('owner,agent')")
    @Log(title = "提交合同确认", businessType = BusinessType.UPDATE)
    @PostMapping("/{contractId}/submit-sign")
    public AjaxResult submitSign(@PathVariable Long contractId)
    {
        rentalContractService.submitContractSign(contractId, getUserId(), false);
        BizChatSession chatSession = openContractChat(contractId);
        bizChatService.sendSystemMessage("contract", contractId, "合同已提交签署，请各方确认。");
        return AjaxResult.success("合同已提交签署").put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyExactRoles('user,tenant')")
    @Log(title = "租户确认合同", businessType = BusinessType.UPDATE)
    @PostMapping("/{contractId}/tenant-confirm")
    public AjaxResult tenantConfirm(@PathVariable Long contractId, @RequestBody(required = false) RentalContractConfirmRequest request)
    {
        String opinion = request == null ? null : request.getOpinion();
        rentalContractService.confirmContract(contractId, getUserId(), RentalContractPartyRole.TENANT.code(), opinion);
        BizChatSession chatSession = openContractChat(contractId);
        bizChatService.sendSystemMessage("contract", contractId,
                opinion == null || opinion.isBlank() ? "租户已确认合同。" : "租户已确认合同，意见：" + opinion);
        return AjaxResult.success("租户已确认").put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyExactRoles('owner')")
    @Log(title = "房东确认合同", businessType = BusinessType.UPDATE)
    @PostMapping("/{contractId}/owner-confirm")
    public AjaxResult ownerConfirm(@PathVariable Long contractId, @RequestBody(required = false) RentalContractConfirmRequest request)
    {
        String opinion = request == null ? null : request.getOpinion();
        rentalContractService.confirmContract(contractId, getUserId(), RentalContractPartyRole.OWNER.code(), opinion);
        BizChatSession chatSession = openContractChat(contractId);
        bizChatService.sendSystemMessage("contract", contractId,
                opinion == null || opinion.isBlank() ? "户主已确认合同。" : "户主已确认合同，意见：" + opinion);
        return AjaxResult.success("房东已确认").put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyExactRoles('agent')")
    @Log(title = "中介确认合同", businessType = BusinessType.UPDATE)
    @PostMapping("/{contractId}/agent-confirm")
    public AjaxResult agentConfirm(@PathVariable Long contractId, @RequestBody(required = false) RentalContractConfirmRequest request)
    {
        String opinion = request == null ? null : request.getOpinion();
        rentalContractService.confirmContract(contractId, getUserId(), RentalContractPartyRole.AGENT.code(), opinion);
        BizChatSession chatSession = openContractChat(contractId);
        bizChatService.sendSystemMessage("contract", contractId,
                opinion == null || opinion.isBlank() ? "中介已确认合同。" : "中介已确认合同，意见：" + opinion);
        return AjaxResult.success("中介已确认").put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyExactRoles('user,tenant,owner,agent')")
    @Log(title = "拒绝合同", businessType = BusinessType.UPDATE)
    @PostMapping("/{contractId}/reject")
    public AjaxResult reject(@PathVariable Long contractId, @RequestBody(required = false) RentalContractConfirmRequest request)
    {
        String opinion = request == null ? null : request.getOpinion();
        String role = resolveCurrentContractRole(contractId);
        rentalContractService.rejectContract(contractId, getUserId(), role, opinion);
        BizChatSession chatSession = openContractChat(contractId);
        bizChatService.sendSystemMessage("contract", contractId,
                opinion == null || opinion.isBlank() ? "合同已被拒绝。" : "合同已被拒绝，意见：" + opinion);
        return AjaxResult.success("合同已拒绝").put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyExactRoles('owner,agent')")
    @Log(title = "合同生效", businessType = BusinessType.UPDATE)
    @PostMapping("/{contractId}/activate")
    public AjaxResult activate(@PathVariable Long contractId)
    {
        rentalContractService.activateContract(contractId, getUserId(), false);
        BizChatSession chatSession = openContractChat(contractId);
        bizChatService.sendSystemMessage("contract", contractId, "合同已生效。");
        return AjaxResult.success("合同已生效").put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyExactRoles('user,tenant,owner,agent')")
    @Log(title = "作废合同", businessType = BusinessType.UPDATE)
    @PostMapping("/{contractId}/void")
    public AjaxResult voidContract(@PathVariable Long contractId, @RequestBody(required = false) RentalAppointmentActionRequest request)
    {
        String reason = request == null ? null : request.getReason();
        rentalContractService.voidContract(contractId, getUserId(), false, reason);
        BizChatSession chatSession = openContractChat(contractId);
        bizChatService.sendSystemMessage("contract", contractId,
                reason == null || reason.isBlank() ? "合同已作废。" : "合同已作废，原因：" + reason);
        return AjaxResult.success("合同已作废").put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyExactRoles('owner,agent')")
    @Log(title = "终止合同", businessType = BusinessType.UPDATE)
    @PostMapping("/{contractId}/terminate")
    public AjaxResult terminate(@PathVariable Long contractId, @RequestBody(required = false) RentalAppointmentActionRequest request)
    {
        String reason = request == null ? null : request.getReason();
        rentalContractService.terminateContract(contractId, getUserId(), false, reason);
        BizChatSession chatSession = openContractChat(contractId);
        bizChatService.sendSystemMessage("contract", contractId,
                reason == null || reason.isBlank() ? "合同已终止。" : "合同已终止，原因：" + reason);
        return AjaxResult.success("合同已终止").put("chatSession", chatSession);
    }

    @PreAuthorize("@ss.hasAnyExactRoles('user,tenant,owner,agent')")
    @PostMapping("/{contractId}/chat")
    public AjaxResult chat(@PathVariable Long contractId)
    {
        return AjaxResult.success(openContractChat(contractId));
    }

    private BizChatSession openContractChat(Long contractId)
    {
        return bizChatService.openSession("contract", contractId);
    }

    private String resolveCurrentContractRole(Long contractId)
    {
        RentalContract contract = rentalContractService.selectRentalContractByContractId(contractId);
        Long userId = getUserId();
        if (contract == null)
        {
            throw new ServiceException("合同不存在");
        }
        if (userId.equals(contract.getTenantId()))
        {
            return RentalContractPartyRole.TENANT.code();
        }
        if (userId.equals(contract.getOwnerId()))
        {
            return RentalContractPartyRole.OWNER.code();
        }
        if (userId.equals(contract.getAgentId()))
        {
            return RentalContractPartyRole.AGENT.code();
        }
        throw new ServiceException("当前用户不是合同参与方");
    }
}
