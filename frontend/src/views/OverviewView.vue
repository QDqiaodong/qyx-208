<template>
  <div class="view-container">
    <el-card>
      <div class="toolbar">
        <el-button @click="loadData">刷新</el-button>
      </div>

      <div v-if="overviews.length === 0" class="empty-section">
        <el-empty description="暂无小队资产数据" />
      </div>

      <div v-for="overview in overviews" :key="overview.teamId" class="overview-card">
        <el-card>
          <div class="overview-header">
            <div class="team-info">
              <h3>{{ overview.teamName }}</h3>
              <span class="team-code">{{ overview.teamCode }}</span>
            </div>
            <div class="stats">
              <div class="stat-item">
                <span class="stat-value">{{ overview.workstationCount }}</span>
                <span class="stat-label">在用操作台数量</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">{{ overview.maxLoadCapacity.toFixed(2) }}</span>
                <span class="stat-label">承重上限(kg)</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">{{ overview.totalLoadCapacity.toFixed(2) }}</span>
                <span class="stat-label">已用承重(kg)</span>
              </div>
              <div class="stat-item">
                <span class="stat-value" :class="{ 'remaining-zero': overview.remainingLoadCapacity <= 0 }">
                  {{ overview.remainingLoadCapacity.toFixed(2) }}
                </span>
                <span class="stat-label">剩余承重(kg)</span>
              </div>
            </div>
          </div>

          <div class="usage-bar">
            <el-progress
              :percentage="usagePercentage(overview)"
              :status="usagePercentage(overview) >= 100 ? 'exception' : usagePercentage(overview) >= 80 ? 'warning' : 'success'"
              :stroke-width="14"
            />
          </div>

          <div v-if="overview.workstations.length > 0" class="workstation-list">
            <h4>操作台明细</h4>
            <el-table :data="overview.workstations" border size="small">
              <el-table-column prop="workstationNo" label="编号" width="120" />
              <el-table-column prop="loadCapacity" label="承重(kg)" width="100" />
              <el-table-column prop="workstationType" label="类型" />
              <el-table-column prop="adaptStation" label="适配工作站" />
            </el-table>
          </div>

          <div v-else class="no-workstation">
            <el-empty description="暂无操作台" :image-size="60" />
          </div>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { teamApi, type TeamAssetOverview } from '@/api/team'

const overviews = ref<TeamAssetOverview[]>([])

const usagePercentage = (overview: TeamAssetOverview) => {
  if (!overview.maxLoadCapacity || overview.maxLoadCapacity <= 0) {
    return overview.totalLoadCapacity > 0 ? 100 : 0
  }
  return Math.min(100, Math.round((overview.totalLoadCapacity / overview.maxLoadCapacity) * 100))
}

const loadData = async () => {
  try {
    overviews.value = await teamApi.getAllTeamAssetOverview()
    ElMessage.success('加载成功')
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  }
}

onMounted(loadData)
</script>

<style scoped>
.view-container {
  max-width: 1200px;
}

.toolbar {
  margin-bottom: 20px;
}

.empty-section {
  padding: 40px;
}

.overview-card {
  margin-bottom: 20px;
}

.overview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.team-info h3 {
  font-size: 18px;
  margin: 0;
  color: #333;
}

.team-code {
  font-size: 12px;
  color: #999;
  margin-left: 10px;
}

.stats {
  display: flex;
  gap: 30px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 24px;
  font-weight: bold;
  color: #1a365d;
}

.stat-label {
  font-size: 12px;
  color: #999;
}

.remaining-zero {
  color: #f56c6c;
}

.usage-bar {
  margin-bottom: 15px;
}

.workstation-list {
  margin-top: 15px;
}

.workstation-list h4 {
  font-size: 14px;
  margin-bottom: 10px;
  color: #666;
}

.no-workstation {
  padding: 20px;
}
</style>