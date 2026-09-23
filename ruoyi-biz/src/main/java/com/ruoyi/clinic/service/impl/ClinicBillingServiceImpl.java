package com.ruoyi.clinic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.clinic.domain.NautilusConsultation;
import com.ruoyi.clinic.domain.NautilusPatient;
import com.ruoyi.clinic.mapper.NautilusConsultationMapper;
import com.ruoyi.clinic.mapper.NautilusPatientMapper;
import com.ruoyi.clinic.service.IClinicBillingService;
import com.ruoyi.clinic.service.INautilusConsultationService;
import com.ruoyi.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 本地模拟结算服务；不接入真实支付或医保。 */
@Service
@RequiredArgsConstructor
public class ClinicBillingServiceImpl implements IClinicBillingService {

    private final NautilusConsultationMapper consultationMapper;
    private final NautilusPatientMapper patientMapper;
    private final INautilusConsultationService consultationService;

    @Override
    public List<Map<String, Object>> listPendingPatients() {
        List<NautilusConsultation> pending = consultationMapper.selectList(
                new LambdaQueryWrapper<NautilusConsultation>()
                        .eq(NautilusConsultation::getStatus, "1")
                        .orderByDesc(NautilusConsultation::getCreateTime));
        List<Long> patientIds = pending.stream()
                .map(NautilusConsultation::getPatientId)
                .distinct().collect(Collectors.toList());
        if (patientIds.isEmpty()) {
            return List.of();
        }

        List<NautilusPatient> patients = patientMapper.selectList(
                new LambdaQueryWrapper<NautilusPatient>()
                        .in(NautilusPatient::getPatientId, patientIds));
        Map<Long, NautilusPatient> patientMap = patients.stream()
                .collect(Collectors.toMap(NautilusPatient::getPatientId, p -> p, (a, b) -> a));

        return pending.stream().map(consultation -> {
            NautilusPatient patient = patientMap.get(consultation.getPatientId());
            Map<String, Object> result = new HashMap<>();
            result.put("consultationId", consultation.getConsultationId().toString());
            result.put("patientId", consultation.getPatientId().toString());
            result.put("patientName", patient == null ? "未知患者" : patient.getPatientName());
            result.put("createTime", consultation.getCreateTime());
            return result;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> queryPrescriptionByConsultationId(Long consultationId) {
        if (consultationId == null) {
            throw new ServiceException("就诊单ID不能为空");
        }
        NautilusConsultation consultation = consultationMapper.selectById(consultationId);
        if (consultation == null) {
            throw new ServiceException("就诊单不存在");
        }
        if (!"1".equals(consultation.getStatus())) {
            throw new ServiceException("该就诊单不是待结算状态");
        }
        if (consultation.getPrescriptionPayload() == null) {
            throw new ServiceException("该就诊单暂无待结算处方");
        }
        return consultation.getPrescriptionPayload();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmSimulatedSettlement(Long consultationId) {
        consultationService.dispenseMedication(consultationId);
        return true;
    }
}
