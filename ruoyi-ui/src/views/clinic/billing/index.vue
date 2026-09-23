<template>
  <div class="app-container">
    <el-row :gutter="24">

      <!-- 左栏：处方明细 (60%) -->
      <el-col :span="14">
        <el-card shadow="hover">
          <div slot="header" style="font-weight: bold; font-size: 16px;">
            <i class="el-icon-s-order" style="margin-right: 6px;"></i>处方明细
            <span v-if="selectedPatientName" style="font-size: 13px; color: #909399; margin-left: 10px;">
              — {{ selectedPatientName }}
            </span>
          </div>

          <div v-if="!prescriptionItems.length" style="text-align:center; color:#c0c4cc; padding: 40px 0;">
            <i class="el-icon-document" style="font-size: 40px;"></i>
            <p>请先选择待结算处方</p>
          </div>

          <el-table v-else :data="prescriptionItems" border stripe>
            <el-table-column label="药品编码" prop="itemCode" width="200" />
            <el-table-column label="药品名称" prop="itemName" />
            <el-table-column label="数量" prop="quantity" width="80" align="center" />
          </el-table>

          <div v-if="prescriptionItems.length" style="margin-top: 24px; text-align: right; padding-right: 8px;">
            <span style="font-size: 14px; color: #909399;">共 {{ prescriptionItems.length }} 种药品</span>
          </div>
        </el-card>
      </el-col>

      <!-- 右栏：本地模拟结算控制台 (40%) -->
      <el-col :span="10">
        <el-card shadow="hover">
          <div slot="header" style="font-weight: bold; font-size: 16px;">
            <i class="el-icon-bank-card" style="margin-right: 6px;"></i>本地模拟结算
          </div>

          <el-form :model="form" label-width="100px" style="margin-top: 10px;">
            <el-form-item label="选择就诊单">
              <el-select
                v-model="form.consultationId"
                placeholder="请选择待模拟结算处方"
                style="width: 100%;"
                :loading="loadingPatients"
                @change="handleConsultationChange"
              >
                <el-option
                  v-for="p in pendingPatients"
                  :key="p.consultationId"
                  :label="formatConsultationOption(p)"
                  :value="p.consultationId"
                />
              </el-select>
            </el-form-item>
          </el-form>

          <div style="display: flex; flex-direction: column; gap: 12px; padding: 0 20px 10px;">
            <el-button
              type="success"
              icon="el-icon-check"
              style="width: 100%;"
              :loading="paying"
              :disabled="!form.consultationId"
              @click="handleConfirmSettlement"
            >
              确认本地模拟结算并发药
            </el-button>
          </div>
          <el-alert
            title="此操作仅更新本地就诊单状态并扣减库存，不会产生真实账单或支付。"
            type="info"
            :closable="false"
            show-icon
            style="margin: 12px 20px 0;"
          />

          <el-alert
            v-if="payResult"
            :title="payResult.message"
            :type="payResult.type"
            show-icon
            style="margin: 12px 20px 0;"
          />
        </el-card>
      </el-col>

    </el-row>
  </div>
</template>

<script>
import { getPendingPatients, getPrescription, confirmSimulatedSettlement } from '@/api/clinic/billing'

export default {
  name: 'ClinicBilling',
  data() {
    return {
      loadingPatients: false,
      paying: false,
      payResult: null,
      pendingPatients: [],      // [{consultationId, patientId, patientName}, ...]
      prescriptionItems: [],
      selectedPatientName: '',
      form: {
        consultationId: ''
      }
    }
  },
  created() {
    this.loadPendingPatients()
  },
  activated() {
    // 从其他页面切回时自动刷新待模拟结算列表
    this.loadPendingPatients()
    this.prescriptionItems = []
    this.selectedPatientName = ''
    this.form.consultationId = ''
    this.payResult = null
  },
  methods: {
    formatConsultationOption(consultation) {
      const createdAt = consultation.createTime
        ? String(consultation.createTime).replace('T', ' ').slice(0, 16)
        : '时间未记录'
      return `${consultation.patientName} · ${createdAt} · 就诊单 ${consultation.consultationId}`
    },

    loadPendingPatients() {
      this.loadingPatients = true
      getPendingPatients().then(res => {
        this.pendingPatients = res.data || []
      }).catch(err => {
        this.$modal.msgError(err.msg || '获取患者列表失败')
      }).finally(() => {
        this.loadingPatients = false
      })
    },

    handleConsultationChange(consultationId) {
      this.prescriptionItems = []
      this.payResult = null
      const found = this.pendingPatients.find(p => p.consultationId === consultationId)
      this.selectedPatientName = found ? found.patientName : ''
      if (!consultationId) return
      getPrescription(consultationId).then(res => {
        this.prescriptionItems = res.data || []
      }).catch(err => {
        this.$modal.msgError(err.msg || '加载处方失败')
      })
    },

    handleConfirmSettlement() {
      this.paying = true
      this.payResult = null
      confirmSimulatedSettlement(this.form.consultationId).then(() => {
        this.payResult = { type: 'success', message: `患者 ${this.selectedPatientName} 本地模拟结算确认成功，库存已扣减。` }
        this.$modal.msgSuccess('本地模拟结算确认成功')
        // 确认成功后刷新待结算列表
        this.form.consultationId = ''
        this.selectedPatientName = ''
        this.prescriptionItems = []
        this.loadPendingPatients()
      }).catch(err => {
        const msg = err.msg || err.message || '模拟结算确认失败'
        this.payResult = { type: 'error', message: `❌ ${msg}` }
        this.$modal.msgError(msg)
      }).finally(() => {
        this.paying = false
      })
    }
  }
}
</script>
