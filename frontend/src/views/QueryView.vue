<template>
  <div class="view-container">
    <el-card>
      <div class="search-section">
        <el-input
          v-model="memberNo"
          placeholder="请输入队员编号"
          style="width: 300px"
          @keyup.enter="doSearch"
        />
        <el-button type="primary" @click="doSearch">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <div v-if="memberInfo" class="member-info">
        <h3>队员信息</h3>
        <el-descriptions :column="4" border>
          <el-descriptions-item label="队员编号">{{ memberInfo.memberNo }}</el-descriptions-item>
          <el-descriptions-item label="队员姓名">{{ memberInfo.memberName }}</el-descriptions-item>
          <el-descriptions-item label="职位">{{ memberInfo.position }}</el-descriptions-item>
          <el-descriptions-item label="所属小队">{{ memberInfo.teamName }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div v-if="teamOverview" class="overview-info">
        <h3>所属小队承重情况</h3>
        <el-descriptions :column="4" border>
          <el-descriptions-item label="在用操作台数量">{{ teamOverview.workstationCount }}</el-descriptions-item>
          <el-descriptions-item label="承重上限(kg)">{{ teamOverview.maxLoadCapacity.toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="已用承重(kg)">{{ teamOverview.totalLoadCapacity.toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="剩余承重(kg)">
            <span :style="{ color: teamOverview.remainingLoadCapacity <= 0 ? '#f56c6c' : '#67c23a', fontWeight: 'bold' }">
              {{ teamOverview.remainingLoadCapacity.toFixed(2) }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="在途样品袋(只)">{{ teamOverview.inTransitBagCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="在途袋重合计(kg)">{{ (teamOverview.inTransitBagWeight ?? 0).toFixed(2) }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div v-if="workstations.length > 0" class="result-section">
        <h3>所属小队操作台列表</h3>
        <el-table :data="workstations" border>
          <el-table-column prop="workstationNo" label="操作台编号" />
          <el-table-column prop="loadCapacity" label="承重(kg)" />
          <el-table-column prop="workstationType" label="类型" />
          <el-table-column prop="adaptStation" label="适配工作站" />
          <el-table-column prop="currentTeamName" label="所属小队" />
        </el-table>
      </div>

      <div v-if="inTransitBags.length > 0" class="result-section">
        <h3>所属小队在途样品袋</h3>
        <el-table :data="inTransitBags" border>
          <el-table-column prop="bagNo" label="袋号" />
          <el-table-column prop="memberName" label="送检队员" />
          <el-table-column prop="bagWeight" label="袋重(kg)" />
          <el-table-column prop="submitDate" label="送检日" />
        </el-table>
      </div>

      <div v-if="searchDone && workstations.length === 0 && !error" class="empty-section">
        <el-empty description="该队员所属小队暂无操作台" />
      </div>

      <div v-if="error" class="error-section">
        <el-alert type="error" :message="error" show-icon />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { queryApi } from '@/api/query'
import { memberApi, type TeamMember } from '@/api/member'
import { sampleBagApi, type SampleBag } from '@/api/sampleBag'
import type { Workstation } from '@/api/workstation'
import type { TeamAssetOverview } from '@/api/team'

const memberNo = ref('')
const workstations = ref<Workstation[]>([])
const inTransitBags = ref<SampleBag[]>([])
const memberInfo = ref<TeamMember | null>(null)
const teamOverview = ref<TeamAssetOverview | null>(null)
const searchDone = ref(false)
const error = ref('')

const doSearch = async () => {
  if (!memberNo.value.trim()) {
    ElMessage.warning('请输入队员编号')
    return
  }

  searchDone.value = false
  error.value = ''

  try {
    memberInfo.value = await memberApi.getByMemberNo(memberNo.value.trim())
    workstations.value = await queryApi.getWorkstationsByMemberNo(memberNo.value.trim())
    teamOverview.value = await queryApi.getTeamAssetOverviewByMemberNo(memberNo.value.trim())
    if (teamOverview.value?.teamId) {
      inTransitBags.value = await sampleBagApi.getAll({
        teamId: teamOverview.value.teamId,
        status: 1
      })
    } else {
      inTransitBags.value = []
    }
    searchDone.value = true
    ElMessage.success('查询成功')
  } catch (err: any) {
    error.value = err.message || '查询失败'
    workstations.value = []
    inTransitBags.value = []
    memberInfo.value = null
    teamOverview.value = null
    searchDone.value = true
  }
}

const resetSearch = () => {
  memberNo.value = ''
  workstations.value = []
  inTransitBags.value = []
  memberInfo.value = null
  teamOverview.value = null
  searchDone.value = false
  error.value = ''
}
</script>

<style scoped>
.view-container {
  max-width: 1200px;
}

.search-section {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.member-info {
  margin-bottom: 20px;
}

.member-info h3 {
  font-size: 16px;
  margin-bottom: 10px;
  color: #333;
}

.overview-info {
  margin-bottom: 20px;
}

.overview-info h3 {
  font-size: 16px;
  margin-bottom: 10px;
  color: #333;
}

.result-section {
  margin-top: 20px;
}

.result-section h3 {
  font-size: 16px;
  margin-bottom: 10px;
  color: #333;
}

.empty-section {
  margin-top: 20px;
}

.error-section {
  margin-top: 20px;
}
</style>