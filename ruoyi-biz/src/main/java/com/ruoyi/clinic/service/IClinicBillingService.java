package com.ruoyi.clinic.service;

import java.util.List;
import java.util.Map;

/**
 * 诊所结算服务接口
 */
public interface IClinicBillingService {

    /**
     * 列出所有待模拟结算处方（返回 consultationId + patientId + patientName）
     */
    List<Map<String, Object>> listPendingPatients();

    /**
     * 按就诊单ID查询待处理处方明细
     *
     * @param consultationId 就诊单ID
     * @return 处方 payload 列表
     */
    List<Map<String, Object>> queryPrescriptionByConsultationId(Long consultationId);

    /**
     * 确认本地模拟结算并发药，不调用真实支付或医保服务。
     *
     * @param consultationId 就诊单ID
     */
    boolean confirmSimulatedSettlement(Long consultationId);
}
