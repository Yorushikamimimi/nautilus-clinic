import request from '@/utils/request'

/** 查询待本地模拟结算的处方列表 */
export function getPendingPatients() {
    return request({
        url: '/clinic/billing/pending-patients',
        method: 'get'
    })
}

/** 按就诊单ID查询待结算处方明细 */
export function getPrescription(consultationId) {
    return request({
        url: '/clinic/billing/prescription',
        method: 'get',
        params: { consultationId }
    })
}

/** 确认本地模拟结算并发药，不代表真实支付或医保结算 */
export function confirmSimulatedSettlement(consultationId) {
    return request({
        url: '/clinic/billing/confirm',
        method: 'post',
        params: { consultationId }
    })
}
