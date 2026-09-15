<template>
  <div class="view-container">
    <el-card>
      <div class="toolbar">
        <el-button type="primary" @click="openForm">新增小队</el-button>
        <el-button @click="refreshData">刷新</el-button>
      </div>
      <el-table :data="teams" border>
        <el-table-column prop="teamCode" label="小队编码" />
        <el-table-column prop="teamName" label="小队名称" />
        <el-table-column prop="leaderName" label="队长" />
        <el-table-column prop="leaderPhone" label="联系电话" />
        <el-table-column prop="maxLoadCapacity" label="随队总承重上限(kg)" />
        <el-table-column prop="description" label="备注" />
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button size="small" @click="openForm(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteTeam(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="formVisible" title="小队信息" width="500px">
      <el-form :model="form" label-width="160px">
        <el-form-item label="小队编码" required>
          <el-input v-model="form.teamCode" />
        </el-form-item>
        <el-form-item label="小队名称" required>
          <el-input v-model="form.teamName" />
        </el-form-item>
        <el-form-item label="队长">
          <el-input v-model="form.leaderName" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.leaderPhone" />
        </el-form-item>
        <el-form-item label="随队总承重上限(kg)" required>
          <el-input-number v-model="form.maxLoadCapacity" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input type="textarea" v-model="form.description" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTeam">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { teamApi, type SurveyTeam } from '@/api/team'

const teams = ref<SurveyTeam[]>([])
const formVisible = ref(false)
const editingId = ref<number | null>(null)

const form = ref({
  teamCode: '',
  teamName: '',
  leaderName: '',
  leaderPhone: '',
  description: '',
  maxLoadCapacity: 0
})

const loadData = async () => {
  teams.value = await teamApi.getAll()
}

const openForm = (row?: SurveyTeam) => {
  if (row) {
    editingId.value = row.id
    form.value = {
      teamCode: row.teamCode,
      teamName: row.teamName,
      leaderName: row.leaderName,
      leaderPhone: row.leaderPhone,
      description: row.description,
      maxLoadCapacity: row.maxLoadCapacity
    }
  } else {
    editingId.value = null
    form.value = {
      teamCode: '',
      teamName: '',
      leaderName: '',
      leaderPhone: '',
      description: '',
      maxLoadCapacity: 0
    }
  }
  formVisible.value = true
}

const saveTeam = async () => {
  try {
    if (editingId.value) {
      await teamApi.update(editingId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await teamApi.create(form.value)
      ElMessage.success('创建成功')
    }
    formVisible.value = false
    await loadData()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const deleteTeam = async (id: number) => {
  try {
    await teamApi.delete(id)
    ElMessage.success('删除成功')
    await loadData()
  } catch (error: any) {
    ElMessage.error(error.message || '删除失败')
  }
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
