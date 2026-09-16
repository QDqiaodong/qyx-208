<template>
  <div class="view-container">
    <el-tabs v-model="activeTab">
      <!-- ============ 油桶档案 ============ -->
      <el-tab-pane label="油桶档案" name="barrels">
        <el-card>
          <div class="toolbar">
            <el-button type="primary" @click="openBarrelForm">新建油桶</el-button>
            <el-button @click="loadBarrels">刷新</el-button>
          </div>

          <el-alert
            type="info"
            :closable="false"
            title="油桶独立建档，只记桶号、额定升数和当前余量；不是小队名下的操作台或样品袋，不计入小队资产。余量在领用/回灌落账当时即随桶持久化。"
            style="margin-bottom: 15px"
          />

          <el-table :data="barrels" border>
            <el-table-column prop="barrelNo" label="桶号" width="160" />
            <el-table-column prop="ratedCapacity" label="额定升数(L)" width="140" />
            <el-table-column label="当前余量(L)" width="140">
              <template #default="{ row }">
                <span :class="{ 'level-empty': row.currentLevel <= 0 }">{{ formatL(row.currentLevel) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="余量占比" min-width="200">
              <template #default="{ row }">
                <el-progress
                  :percentage="levelPercent(row)"
                  :status="row.currentLevel <= 0 ? 'exception' : undefined"
                />
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="120">
              <template #default="{ row }">{{ row.remark || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button size="small" @click="openIssueForm(row)">领油</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- ============ 领用台账 ============ -->
      <el-tab-pane label="油料领用台账" name="ledger">
        <el-card>
          <div class="toolbar toolbar-wrap">
            <el-button type="primary" @click="openIssueForm()">领用登记</el-button>
            <el-button @click="loadAll">刷新</el-button>
            <el-select v-model="filterBarrelId" placeholder="按油桶筛选" clearable style="width: 170px" @change="loadLedger">
              <el-option v-for="b in barrels" :key="b.id" :label="`桶 ${b.barrelNo}`" :value="b.id" />
            </el-select>
            <el-select v-model="filterTeamId" placeholder="按小队筛选" clearable style="width: 170px" @change="loadLedger">
              <el-option v-for="t in teams" :key="t.id" :label="t.teamName" :value="t.id" />
            </el-select>
            <el-select v-model="filterType" placeholder="按类型筛选" clearable style="width: 130px" @change="loadLedger">
              <el-option label="领用" :value="1" />
              <el-option label="回灌" :value="2" />
            </el-select>
          </div>

          <el-alert
            type="warning"
            :closable="false"
            title="余量不足时领用不进账、余量不变；两人同时舀同一桶，只先到的一笔成功，后到的失败，余量不会为负。回灌最多加到额定升数，失败时余量停在领用扣完后的数。"
            style="margin-bottom: 15px"
          />

          <el-table :data="ledger" border>
            <el-table-column label="类型" width="80">
              <template #default="{ row }">
                <el-tag :type="row.type === 1 ? 'danger' : 'success'">
                  {{ row.type === 1 ? '领用' : '回灌' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="ledgerDate" label="日期" width="115" />
            <el-table-column label="油桶" width="120">
              <template #default="{ row }">桶 {{ row.barrelNo }}</template>
            </el-table-column>
            <el-table-column prop="teamName" label="小队" width="140">
              <template #default="{ row }">{{ row.teamName || '-' }}</template>
            </el-table-column>
            <el-table-column label="队员" width="150">
              <template #default="{ row }">
                {{ row.memberName ? `${row.memberName}（${row.memberNo}）` : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="升数(L)" width="100">
              <template #default="{ row }">
                <span :class="row.type === 1 ? 'liters-out' : 'liters-in'">
                  {{ row.type === 1 ? '-' : '+' }}{{ formatL(row.liters) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="操作后余量(L)" width="130">
              <template #default="{ row }">{{ formatL(row.levelAfter) }}</template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="100">
              <template #default="{ row }">{{ row.remark || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button v-if="row.type === 1" size="small" type="success" @click="openReturnForm(row)">
                  回灌
                </el-button>
                <span v-else class="return-link">领单#{{ row.relatedIssueId }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- ============ 新建油桶 ============ -->
    <el-dialog v-model="barrelFormVisible" title="新建油桶" width="460px">
      <el-form :model="barrelForm" label-width="100px">
        <el-form-item label="桶号" required>
          <el-input v-model="barrelForm.barrelNo" placeholder="桶上贴的编号，如 D-01" />
        </el-form-item>
        <el-form-item label="额定升数(L)" required>
          <el-input-number v-model="barrelForm.ratedCapacity" :min="0.01" :precision="2" :step="20" />
        </el-form-item>
        <el-form-item label="初始余量(L)">
          <el-input-number v-model="barrelForm.currentLevel" :min="0" :precision="2" :step="20" />
          <div class="form-hint">不填默认满桶（=额定升数）</div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="barrelForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="barrelFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveBarrel">保存</el-button>
      </template>
    </el-dialog>

    <!-- ============ 领用登记 ============ -->
    <el-dialog v-model="issueFormVisible" title="出队领油登记" width="500px">
      <el-form :model="issueForm" label-width="100px">
        <el-form-item label="油桶" required>
          <el-select v-model="issueForm.barrelId" placeholder="选择油桶" style="width: 100%" @change="onIssueBarrelChange">
            <el-option
              v-for="b in barrels"
              :key="b.id"
              :label="`桶 ${b.barrelNo}（余量 ${formatL(b.currentLevel)} / ${formatL(b.ratedCapacity)} L）`"
              :value="b.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="领油小队" required>
          <el-select v-model="issueForm.teamId" placeholder="选择小队" style="width: 100%" @change="onIssueTeamChange">
            <el-option v-for="t in teams" :key="t.id" :label="t.teamName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="领油队员" required>
          <el-select v-model="issueForm.memberId" placeholder="先选择小队" style="width: 100%">
            <el-option
              v-for="m in teamMembers"
              :key="m.id"
              :label="`${m.memberName}（${m.memberNo}）`"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="舀走升数(L)" required>
          <el-input-number v-model="issueForm.liters" :min="0.01" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="领用日期" required>
          <el-date-picker v-model="issueForm.issueDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="issueForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <div v-if="selectedIssueBarrel" class="level-hint">
        桶 {{ selectedIssueBarrel.barrelNo }} 当前余量 <b>{{ formatL(selectedIssueBarrel.currentLevel) }}</b> /
        额定 {{ formatL(selectedIssueBarrel.ratedCapacity) }} L；余量不足时本笔不会进账。
      </div>
      <template #footer>
        <el-button @click="issueFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveIssue">登记并扣余量</el-button>
      </template>
    </el-dialog>

    <!-- ============ 回灌 ============ -->
    <el-dialog v-model="returnFormVisible" title="余油回灌" width="500px">
      <el-form :model="returnForm" label-width="100px">
        <el-form-item label="原领用单">
          <el-input :model-value="originLedgerText" disabled />
        </el-form-item>
        <el-form-item label="回灌到桶">
          <el-input :model-value="originLedger ? '桶 ' + originLedger.barrelNo : ''" disabled />
          <div class="form-hint">余油只能倒回原领用同一桶</div>
        </el-form-item>
        <el-form-item label="回灌升数(L)" required>
          <el-input-number v-model="returnForm.liters" :min="0.01" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="回灌日期" required>
          <el-date-picker v-model="returnForm.returnDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="returnForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <div v-if="originBarrel" class="level-hint">
        桶 {{ originBarrel.barrelNo }} 当前余量 <b>{{ formatL(originBarrel.currentLevel) }}</b> /
        额定 {{ formatL(originBarrel.ratedCapacity) }} L，本次最多还能倒回
        <b>{{ formatL(originBarrel.ratedCapacity - originBarrel.currentLevel) }}</b> L；超额则本笔回灌失败，余量不变。
      </div>
      <template #footer>
        <el-button @click="returnFormVisible = false">取消</el-button>
        <el-button type="success" :loading="saving" @click="saveReturn">回灌并加余量</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { fuelApi, type FuelBarrel, type FuelLedger } from '@/api/fuel'
import { teamApi, type SurveyTeam } from '@/api/team'
import { memberApi, type TeamMember } from '@/api/member'

const today = () => new Date().toISOString().slice(0, 10)

const activeTab = ref('barrels')

const barrels = ref<FuelBarrel[]>([])
const ledger = ref<FuelLedger[]>([])
const teams = ref<SurveyTeam[]>([])
const teamMembers = ref<TeamMember[]>([])

const filterBarrelId = ref<number | null>(null)
const filterTeamId = ref<number | null>(null)
const filterType = ref<number | null>(null)

const barrelFormVisible = ref(false)
const issueFormVisible = ref(false)
const returnFormVisible = ref(false)
const saving = ref(false)

const barrelForm = ref({ barrelNo: '', ratedCapacity: 200, currentLevel: null as number | null, remark: '' })

const issueForm = ref({
  barrelId: null as number | null,
  teamId: null as number | null,
  memberId: null as number | null,
  liters: 1,
  issueDate: today(),
  remark: ''
})

const returnForm = ref({ issueId: null as number | null, liters: 1, returnDate: today(), remark: '' })
const originLedger = ref<FuelLedger | null>(null)

const selectedIssueBarrel = computed(() => barrels.value.find(b => b.id === issueForm.value.barrelId) || null)
const originBarrel = computed(() => originLedger.value ? barrels.value.find(b => b.id === originLedger.value?.barrelId) || null : null)
const originLedgerText = computed(() => {
  const r = originLedger.value
  if (!r) return ''
  return `#${r.id}　桶 ${r.barrelNo} / ${r.teamName ?? ''} / ${r.memberName ?? ''}（${r.memberNo ?? ''}）`
})

const formatL = (v: number) => Number(v).toFixed(2)
const levelPercent = (row: FuelBarrel) => {
  if (!row.ratedCapacity) return 0
  return Math.min(100, Math.round((row.currentLevel / row.ratedCapacity) * 100))
}

const loadBarrels = async () => {
  barrels.value = await fuelApi.listBarrels()
}
const loadTeams = async () => {
  teams.value = await teamApi.getAll()
}
const loadLedger = async () => {
  ledger.value = await fuelApi.listLedger({
    barrelId: filterBarrelId.value,
    teamId: filterTeamId.value,
    type: filterType.value
  })
}
const loadAll = async () => {
  await loadBarrels()
  await loadLedger()
}

const onIssueTeamChange = async (teamId: number | null) => {
  issueForm.value.memberId = null
  teamMembers.value = teamId ? await memberApi.getByTeamId(teamId) : []
}
const onIssueBarrelChange = () => {}

const openBarrelForm = () => {
  barrelForm.value = { barrelNo: '', ratedCapacity: 200, currentLevel: null, remark: '' }
  barrelFormVisible.value = true
}

const saveBarrel = async () => {
  if (!barrelForm.value.barrelNo.trim()) {
    ElMessage.warning('请填写桶号')
    return
  }
  saving.value = true
  try {
    await fuelApi.createBarrel({
      barrelNo: barrelForm.value.barrelNo.trim(),
      ratedCapacity: barrelForm.value.ratedCapacity,
      currentLevel: barrelForm.value.currentLevel ?? undefined,
      remark: barrelForm.value.remark
    })
    ElMessage.success('油桶建档成功')
    barrelFormVisible.value = false
    await loadBarrels()
  } catch (error: any) {
    ElMessage.error(error.message || '建档失败')
  } finally {
    saving.value = false
  }
}

const openIssueForm = async (barrel?: FuelBarrel) => {
  await loadBarrels()
  await loadTeams()
  issueForm.value = {
    barrelId: barrel ? barrel.id : null,
    teamId: filterTeamId.value,
    memberId: null,
    liters: 1,
    issueDate: today(),
    remark: ''
  }
  teamMembers.value = filterTeamId.value ? await memberApi.getByTeamId(filterTeamId.value) : []
  issueFormVisible.value = true
}

const saveIssue = async () => {
  if (!issueForm.value.barrelId) {
    ElMessage.warning('请选择油桶')
    return
  }
  if (!issueForm.value.teamId) {
    ElMessage.warning('请选择领油小队')
    return
  }
  if (!issueForm.value.memberId) {
    ElMessage.warning('请选择领油队员')
    return
  }
  saving.value = true
  try {
    await fuelApi.issue({
      barrelId: issueForm.value.barrelId,
      teamId: issueForm.value.teamId,
      memberId: issueForm.value.memberId,
      liters: issueForm.value.liters,
      issueDate: issueForm.value.issueDate,
      remark: issueForm.value.remark
    })
    ElMessage.success('领用登记成功，桶余量已扣减')
    issueFormVisible.value = false
    await loadAll()
  } catch (error: any) {
    // 后端是唯一裁判：余量不足（含并发后到）明确失败，台账与余量均不变
    ElMessage.error(error.message || '领用失败')
  } finally {
    saving.value = false
  }
}

const openReturnForm = (row: FuelLedger) => {
  originLedger.value = row
  returnForm.value = { issueId: row.id, liters: 1, returnDate: today(), remark: '' }
  returnFormVisible.value = true
}

const saveReturn = async () => {
  if (!returnForm.value.issueId) {
    ElMessage.warning('缺少原领用记录')
    return
  }
  saving.value = true
  try {
    await fuelApi.returnFuel({
      issueId: returnForm.value.issueId,
      liters: returnForm.value.liters,
      returnDate: returnForm.value.returnDate,
      remark: returnForm.value.remark
    })
    ElMessage.success('回灌成功，桶余量已增加')
    returnFormVisible.value = false
    await loadAll()
  } catch (error: any) {
    // 回灌会超过额定升数时后端整笔失败，余量停在领用扣完后的数
    ElMessage.error(error.message || '回灌失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadTeams()
  await loadAll()
})
</script>

<style scoped>
.view-container {
  max-width: 1280px;
}

.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 20px;
}

.toolbar-wrap {
  flex-wrap: wrap;
}

.level-empty {
  color: #f56c6c;
  font-weight: 600;
}

.liters-out {
  color: #f56c6c;
  font-weight: 600;
}

.liters-in {
  color: #67c23a;
  font-weight: 600;
}

.return-link {
  color: #909399;
  font-size: 12px;
}

.form-hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

.level-hint {
  margin-top: 8px;
  padding: 8px 12px;
  background: #fdf6ec;
  border: 1px solid #f5dab1;
  border-radius: 4px;
  font-size: 13px;
  color: #b88230;
}
</style>
