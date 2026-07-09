<template>
  <div class="view-container">
    <el-card>
      <div class="toolbar">
        <el-button type="primary" @click="openForm">新增操作台</el-button>
        <el-button @click="refreshData">刷新</el-button>
      </div>
      <el-table :data="workstations" border>
        <el-table-column prop="workstationNo" label="操作台编号" />
        <el-table-column prop="loadCapacity" label="承重(kg)" />
        <el-table-column prop="workstationType" label="类型" />
        <el-table-column prop="adaptStation" label="适配工作站" />
        <el-table-column prop="currentTeamName" label="所属小队">
          <template #default="{ row }">
            <span>{{ row.currentTeamName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button size="small" @click="openForm(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteWorkstation(row.id)">删除</el-button>
            <el-button size="small" type="warning" @click="openTransfer(row)">转移</el-button>
            <el-button size="small" @click="viewTransferHistory(row.id)">变更记录</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="formVisible" title="操作台信息" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="操作台编号" required>
          <el-input v-model="form.workstationNo" />
        </el-form-item>
        <el-form-item label="承重(kg)" required>
          <el-input-number v-model="form.loadCapacity" :min="0" />
        </el-form-item>
        <el-form-item label="类型">
          <el-input v-model="form.workstationType" />
        </el-form-item>
        <el-form-item label="适配工作站">
          <el-input v-model="form.adaptStation" />
        </el-form-item>
        <el-form-item label="所属小队">
          <el-select v-model="form.currentTeamId" placeholder="选择小队">
            <el-option v-for="team in teams" :key="team.id" :label="team.teamName" :value="team.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="saveWorkstation">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="transferVisible" title="转移操作台" width="500px">
      <el-form :model="transferForm" label-width="100px">
        <el-form-item label="目标小队" required>
          <el-select v-model="transferForm.toTeamId" placeholder="选择目标小队">
            <el-option v-for="team in teams" :key="team.id" :label="team.teamName" :value="team.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="转移原因">
          <el-input type="textarea" v-model="transferForm.transferReason" />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="transferForm.operator" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="primary" @click="doTransfer">确认转移</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="historyVisible" title="变更记录" width="600px">
      <el-table :data="transferHistory" border>
        <el-table-column prop="transferTime" label="变更时间" />
        <el-table-column label="原属小队">
          <template #default="{ row }">
            <span>{{ row.fromTeam?.teamName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="toTeam.teamName" label="目标小队" />
        <el-table-column prop="transferReason" label="变更原因" />
        <el-table-column prop="operator" label="操作人" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { workstationApi, type Workstation, type TransferDTO } from '@/api/workstation'
import { teamApi, type SurveyTeam } from '@/api/team'

const workstations = ref<Workstation[]>([])
const teams = ref<SurveyTeam[]>([])
const formVisible = ref(false)
const transferVisible = ref(false)
const historyVisible = ref(false)
const transferHistory = ref<any[]>([])
const editingId = ref<number | null>(null)
const transferringId = ref<number | null>(null)

const form = ref({
  workstationNo: '',
  loadCapacity: 0,
  workstationType: '',
  adaptStation: '',
  currentTeamId: null as number | null
})

const transferForm = ref({
  workstationId: 0,
  toTeamId: 0,
  transferReason: '',
  operator: ''
})

const loadData = async () => {
  workstations.value = await workstationApi.getAll()
  teams.value = await teamApi.getAll()
}

const openForm = (row?: Workstation) => {
  if (row) {
    editingId.value = row.id
    form.value = {
      workstationNo: row.workstationNo,
      loadCapacity: row.loadCapacity,
      workstationType: row.workstationType,
      adaptStation: row.adaptStation,
      currentTeamId: row.currentTeamId
    }
  } else {
    editingId.value = null
    form.value = {
      workstationNo: '',
      loadCapacity: 0,
      workstationType: '',
      adaptStation: '',
      currentTeamId: null
    }
  }
  formVisible.value = true
}

const saveWorkstation = async () => {
  try {
    if (editingId.value) {
      await workstationApi.update(editingId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await workstationApi.create(form.value)
      ElMessage.success('创建成功')
    }
    formVisible.value = false
    await loadData()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const deleteWorkstation = async (id: number) => {
  try {
    await workstationApi.delete(id)
    ElMessage.success('删除成功')
    await loadData()
  } catch (error: any) {
    ElMessage.error(error.message || '删除失败')
  }
}

const openTransfer = (row: Workstation) => {
  transferringId.value = row.id
  transferForm.value = {
    workstationId: row.id,
    toTeamId: 0,
    transferReason: '',
    operator: ''
  }
  transferVisible.value = true
}

const doTransfer = async () => {
  try {
    await workstationApi.transfer(transferForm.value as TransferDTO)
    ElMessage.success('转移成功')
    transferVisible.value = false
    await loadData()
  } catch (error: any) {
    ElMessage.error(error.message || '转移失败')
  }
}

const viewTransferHistory = async (id: number) => {
  transferHistory.value = await workstationApi.getTransferHistory(id)
  historyVisible.value = true
}

const refreshData = async () => {
  await loadData()
  ElMessage.success('刷新成功')
}

onMounted(loadData)
</script>

<style scoped>
.view-container {
  max-width: 1200px;
}

.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}
</style>