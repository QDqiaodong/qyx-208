<template>
  <div class="view-container">
    <el-card>
      <div class="toolbar">
        <el-button type="primary" @click="openForm()">送检登记</el-button>
        <el-button @click="loadData">刷新</el-button>
        <el-select v-model="filterTeamId" placeholder="按小队筛选" clearable style="width: 180px" @change="loadData">
          <el-option v-for="t in teams" :key="t.id" :label="t.teamName" :value="t.id" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="按状态筛选" clearable style="width: 140px" @change="loadData">
          <el-option label="在途" :value="1" />
          <el-option label="办结" :value="2" />
        </el-select>
      </div>

      <el-alert
        type="warning"
        :closable="false"
        title="同一小队、同一送检日、同一袋号只能登记一条；两人同交时只有先到的进账，后到的会失败。办结袋不能直接改小队或袋重，须先退回在途。"
        style="margin-bottom: 15px"
      />

      <el-table :data="bags" border>
        <el-table-column prop="bagNo" label="袋号" width="130" />
        <el-table-column prop="teamName" label="所属小队" width="140" />
        <el-table-column label="送检队员" width="160">
          <template #default="{ row }">
            {{ row.memberName }}（{{ row.memberNo }}）
          </template>
        </el-table-column>
        <el-table-column prop="bagWeight" label="袋重(kg)" width="100" />
        <el-table-column prop="submitDate" label="送检日" width="120" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'warning' : 'success'">
              {{ row.status === 1 ? '在途' : '办结' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="completeTime" label="办结时间" width="170">
          <template #default="{ row }">{{ row.completeTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <template v-if="row.status === 1">
              <el-button size="small" @click="openForm(row)">编辑</el-button>
              <el-button size="small" type="success" @click="doComplete(row.id)">出站办结</el-button>
            </template>
            <template v-else>
              <el-button size="small" type="warning" @click="doReturnTransit(row.id)">退回在途</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="formVisible" :title="editingId ? '编辑送检登记' : '样品袋送检登记'" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="袋号" required>
          <el-input v-model="form.bagNo" placeholder="如 BAG-2026-001" />
        </el-form-item>
        <el-form-item label="所属小队" required>
          <el-select v-model="form.teamId" placeholder="选择小队" style="width: 100%" @change="onTeamChange">
            <el-option v-for="t in teams" :key="t.id" :label="t.teamName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="送检队员" required>
          <el-select v-model="form.memberId" placeholder="先选择所属小队" style="width: 100%">
            <el-option
              v-for="m in teamMembers"
              :key="m.id"
              :label="`${m.memberName}（${m.memberNo}）`"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="袋重(kg)" required>
          <el-input-number v-model="form.bagWeight" :min="0.01" :precision="2" :step="0.5" />
        </el-form-item>
        <el-form-item label="送检日" required>
          <el-date-picker v-model="form.submitDate" type="date" value-format="YYYY-MM-DD" placeholder="选择送检日" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveBag">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { sampleBagApi, type SampleBag } from '@/api/sampleBag'
import { teamApi, type SurveyTeam } from '@/api/team'
import { memberApi, type TeamMember } from '@/api/member'

const bags = ref<SampleBag[]>([])
const teams = ref<SurveyTeam[]>([])
const teamMembers = ref<TeamMember[]>([])
const filterTeamId = ref<number | null>(null)
const filterStatus = ref<number | null>(null)
const formVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const form = ref({
  bagNo: '',
  teamId: null as number | null,
  memberId: null as number | null,
  bagWeight: 1,
  submitDate: new Date().toISOString().slice(0, 10)
})

const loadTeams = async () => {
  teams.value = await teamApi.getAll()
}

const loadMembersOfTeam = async (teamId: number | null) => {
  if (!teamId) {
    teamMembers.value = []
    return
  }
  teamMembers.value = await memberApi.getByTeamId(teamId)
}

const onTeamChange = (teamId: number | null) => {
  form.value.memberId = null
  loadMembersOfTeam(teamId)
}

const loadData = async () => {
  bags.value = await sampleBagApi.getAll({
    teamId: filterTeamId.value,
    status: filterStatus.value
  })
}

const openForm = async (row?: SampleBag) => {
  await loadTeams()
  if (row) {
    editingId.value = row.id
    form.value = {
      bagNo: row.bagNo,
      teamId: row.teamId,
      memberId: row.memberId,
      bagWeight: row.bagWeight,
      submitDate: row.submitDate
    }
    await loadMembersOfTeam(row.teamId)
  } else {
    editingId.value = null
    form.value = {
      bagNo: '',
      teamId: filterTeamId.value,
      memberId: null,
      bagWeight: 1,
      submitDate: new Date().toISOString().slice(0, 10)
    }
    await loadMembersOfTeam(filterTeamId.value)
  }
  formVisible.value = true
}

const saveBag = async () => {
  if (!form.value.bagNo.trim()) {
    ElMessage.warning('请填写袋号')
    return
  }
  if (!form.value.teamId) {
    ElMessage.warning('请选择所属小队')
    return
  }
  if (!form.value.memberId) {
    ElMessage.warning('请选择送检队员')
    return
  }
  if (!form.value.submitDate) {
    ElMessage.warning('请选择送检日')
    return
  }
  saving.value = true
  try {
    const payload = {
      bagNo: form.value.bagNo.trim(),
      teamId: form.value.teamId,
      memberId: form.value.memberId,
      bagWeight: form.value.bagWeight,
      submitDate: form.value.submitDate
    }
    if (editingId.value) {
      await sampleBagApi.update(editingId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await sampleBagApi.register(payload)
      ElMessage.success('送检登记成功，已在途')
    }
    formVisible.value = false
    await loadData()
  } catch (error: any) {
    // 后端是唯一裁判：重复落账/办结袋直接修改等都在这里明确失败原因
    ElMessage.error(error.message || '操作失败')
  } finally {
    saving.value = false
  }
}

const doComplete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确认该袋已出站办结？办结后将不能直接改小队或袋重。', '出站办结', {
      type: 'warning'
    })
    await sampleBagApi.complete(id)
    ElMessage.success('已出站办结')
    await loadData()
  } catch (error: any) {
    if (error !== 'cancel' && error?.message) {
      ElMessage.error(error.message || '办结失败')
    }
  }
}

const doReturnTransit = async (id: number) => {
  try {
    await sampleBagApi.returnToTransit(id)
    ElMessage.success('已退回在途，可修改后重新办结')
    await loadData()
  } catch (error: any) {
    ElMessage.error(error.message || '退回失败')
  }
}

onMounted(async () => {
  await loadTeams()
  await loadData()
})
</script>

<style scoped>
.view-container {
  max-width: 1200px;
}

.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 20px;
}
</style>
