package com.ruoyi.clinic.controller;

import com.ruoyi.clinic.service.IClinicBillingService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** 本地模拟结算控制器，不接入真实支付或医保。 */
@Tag(name = "诊所模拟结算管理", description = "本地模拟结算确认与处方查询")
@RestController
@RequestMapping("/clinic/billing")
@RequiredArgsConstructor
public class ClinicBillingController {

    private final IClinicBillingService clinicBillingService;

    /** 获取待模拟结算的处方列表 */
    @Operation(summary = "查询待模拟结算处方")
    @PreAuthorize("@ss.hasPermi('clinic:billing:list')")
    @GetMapping("/pending-patients")
    public AjaxResult pendingPatients() {
        return AjaxResult.success(clinicBillingService.listPendingPatients());
    }

    /** 按就诊单ID查询待结算处方明细 */
    @Operation(summary = "查询待结算处方明细")
    @PreAuthorize("@ss.hasPermi('clinic:billing:list')")
    @GetMapping("/prescription")
    public AjaxResult getPrescription(@RequestParam Long consultationId) {
        return AjaxResult.success(clinicBillingService.queryPrescriptionByConsultationId(consultationId));
    }

    /** 确认本地模拟结算并发药 */
    @Operation(summary = "确认本地模拟结算并发药")
    @PreAuthorize("@ss.hasPermi('clinic:billing:pay')")
    @Log(title = "诊所结算", businessType = BusinessType.UPDATE)
    @PostMapping("/confirm")
    public AjaxResult confirmSimulatedSettlement(@RequestParam Long consultationId) {
        clinicBillingService.confirmSimulatedSettlement(consultationId);
        return AjaxResult.success("本地模拟结算确认成功，已完成发药");
    }
}
