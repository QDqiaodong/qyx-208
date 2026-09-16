import axios from './axios'

/** 油桶档案 */
export interface FuelBarrel {
  id: number
  /** 桶号 */
  barrelNo: string
  /** 额定升数 */
  ratedCapacity: number
  /** 当前余量（升），落库在桶上，刷新/重开页面仍是扣减后的值 */
  currentLevel: number
  remark?: string
}

/** 台账流水类型：1=领用，2=回灌 */
export type FuelLedgerType = 1 | 2

export interface FuelLedger {
  id: number
  type: FuelLedgerType
  barrelId: number
  barrelNo: string
  teamId?: number
  teamName?: string
  memberId?: number
  memberNo?: string
  memberName?: string
  /** 本次升数：领用为舀走量，回灌为倒回量 */
  liters: number
  /** 登记日期 yyyy-MM-dd */
  ledgerDate: string
  /** 回灌关联的原领用台账 id */
  relatedIssueId?: number | null
  /** 操作后桶余量快照 */
  levelAfter: number
  remark?: string
}

export interface FuelIssuePayload {
  barrelId: number
  teamId: number
  memberId: number
  liters: number
  issueDate?: string
  remark?: string
}

export interface FuelReturnPayload {
  issueId: number
  liters: number
  returnDate?: string
  remark?: string
}

export interface FuelLedgerQuery {
  barrelId?: number | null
  teamId?: number | null
  memberId?: number | null
  type?: number | null
}

export const fuelApi = {
  // ---- 油桶档案 ----
  listBarrels(): Promise<FuelBarrel[]> {
    return axios.get('/fuel-barrels')
  },
  createBarrel(data: Omit<FuelBarrel, 'id' | 'currentLevel'> & { currentLevel?: number }): Promise<FuelBarrel> {
    return axios.post('/fuel-barrels', data)
  },

  // ---- 领用 / 回灌台账 ----
  listLedger(params?: FuelLedgerQuery): Promise<FuelLedger[]> {
    return axios.get('/fuel-ledger', { params: params || {} })
  },
  issue(data: FuelIssuePayload): Promise<FuelLedger> {
    return axios.post('/fuel-ledger/issue', data)
  },
  returnFuel(data: FuelReturnPayload): Promise<FuelLedger> {
    return axios.post('/fuel-ledger/return', data)
  }
}
