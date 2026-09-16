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
        <el-table-column label="未读转队通知" width="140">
          <template #default="{ row }">
            <el-badge :value="unreadCounts[row.id] || 0" :hidden="!unreadCounts[row.id]" type="danger">
              <el-button size="small" @click="openNotifications(row)">
                {{ unreadCounts[row.id] ? `查看（${unreadCounts[row.id]}）` : '查看' }}
              </el-button>
            </el-badge>
          </template>
        </el-table-column>
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

    <el-dialog
      v-model="notificationVisible"
      :title="currentTeam ? `${currentTeam.teamName} - 转队通知` : '转队通知'"
      width="720px"
      @closed="handleNotificationClosed"
    >
      <template #header>
        <div class="notification-header">
          <span>{{ currentTeam ? `${currentTeam.teamName} - 转队通知` : '转队通知' }}</span>
          <el-button
            type="primary"
            size="small"
            :disabled="!hasUnreadNotification"
            @click="markAllNotificationsRead"
          >
            全部标已读
          </el-button>
        </div>
      </template>

      <el-table :data="notifications" border v-loading="notificationLoading">
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.direction === 1 ? 'warning' : 'success'">
              {{ row.direction === 1 ? '调出' : '调入' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="通知内容" min-width="280">
          <template #default="{ row }">
            <div :class="['notification-content', { unread: row.readStatus === 0 }]">
              {{ row.content }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="counterpartTeamName" label="对方小队" width="120" />
        <el-table-column prop="createTime" label="通知时间" width="170" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.readStatus === 0 ? 'danger' : 'info'">
              {{ row.readStatus === 0 ? '未读' : '已读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button
              size="small"
              :disabled="row.readStatus === 1"
              @click="markNotificationRead(row)"
            >
              标已读
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!notificationLoading && notifications.length === 0" description="暂无转队通知" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { teamApi, type SurveyTeam } from '@/api/team'
import { notificationApi, type TransferNotification } from '@/api/notification'

const teams = ref<SurveyTeam[]>([])
const formVisible = ref(false)
const editingId = ref<number | null>(null)
const unreadCounts = ref<Record<number, number>>({})
const notificationVisible = ref(false)
const notificationLoading = ref(false)
const currentTeam = ref<SurveyTeam | null>(null)
const notifications = ref<TransferNotification[]>([])

const form = ref({
  teamCode: '',
  teamName: '',
  leaderName: '',
  leaderPhone: '',
  description: '',
  maxLoadCapacity: 0
})

const hasUnreadNotification = computed(() => notifications.value.some(item => item.readStatus === 0))

const loadUnreadCounts = async () => {
  try {
    const data = await notificationApi.getAllUnreadCounts()
    const counts: Record<number, number> = {}
    Object.entries(data).forEach(([teamId, count]) => {
      counts[Number(teamId)] = Number(count)
    })
    unreadCounts.value = counts
  } catch (error: any) {
    ElMessage.error(error.message || '未读数加载失败')
  }
}

const loadData = async () => {
  teams.value = await teamApi.getAll()
  await loadUnreadCounts()
}

const openNotifications = async (team: SurveyTeam) => {
  currentTeam.value = team
  notificationVisible.value = true
  notificationLoading.value = true
  try {
    notifications.value = await notificationApi.getByTeam(team.id)
  } catch (error: any) {
    ElMessage.error(error.message || '通知加载失败')
    notifications.value = []
  } finally {
    notificationLoading.value = false
  }
}

const markNotificationRead = async (notification: TransferNotification) => {
  if (!currentTeam.value) return
  try {
    await notificationApi.markAsRead(currentTeam.value.id, notification.id)
    notification.readStatus = 1
    const currentCount = unreadCounts.value[currentTeam.value.id] || 0
    unreadCounts.value[currentTeam.value.id] = Math.max(0, currentCount - 1)
  } catch (error: any) {
    ElMessage.error(error.message || '标记已读失败')
  }
}

const markAllNotificationsRead = async () => {
  if (!currentTeam.value) return
  try {
    await notificationApi.markAllAsRead(currentTeam.value.id)
    notifications.value.forEach(item => {
      item.readStatus = 1
    })
    unreadCounts.value[currentTeam.value.id] = 0
  } catch (error: any) {
    ElMessage.error(error.message || '全部已读失败')
  }
}

const handleNotificationClosed = () => {
  currentTeam.value = null
  notifications.value = []
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
  try {
    await loadData()
    ElMessage.success('刷新成功')
  } catch (error: any) {
    ElMessage.error(error.message || '刷新失败')
  }
}

onMounted(loadData)
</script>

<style scoped>
.view-container {
  max-width: 1400px;
}

.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.notification-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.notification-content {
  color: #606266;
}

.notification-content.unread {
  color: #303133;
  font-weight: 600;
}
</style>
