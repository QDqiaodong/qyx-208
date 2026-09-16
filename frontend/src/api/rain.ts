import axios from './axios'

/** 雨水收集桶档案 */
export interface RainBucket {
  id: number
  /** 桶号（全营唯一） */
  bucketNo: string
  remark?: string
}

/** 读数状态：1=有效，2=作废 */
export type RainReadingStatus = 1 | 2

/** 雨量读数（rec 本一行） */
export interface RainReading {
  id: number
  bucketId: number
  bucketNo: string
  /** 自然日 yyyy-MM-dd */
  readDate: string
  /** 本次毫米数 */
  millimeters: number
  status: RainReadingStatus
  /** 作废原因（作废行留痕） */
  voidReason?: string | null
  voidTime?: string | null
  /** 当前版本号：改数/作废时必须原样带回，对不上即被他人抢先（409） */
  version: number
  remark?: string
}

/** 气象卡片：一个自然日的合计 */
export interface RainDailyTotal {
  /** 自然日 yyyy-MM-dd */
  readDate: string
  /** 当日合计毫米数 = 各桶有效读数之和（作废不计） */
  totalMm: number
  /** 参与合计的有效读数条数 */
  validCount: number
  /** 卡片最近一次重算时间 */
  updateTime?: string
}

export interface RainReadingQuery {
  bucketId?: number | null
  date?: string | null
  status?: number | null
}

export const rainApi = {
  // ---- 收集桶档案 ----
  listBuckets(): Promise<RainBucket[]> {
    return axios.get('/rain-buckets')
  },
  createBucket(data: { bucketNo: string; remark?: string }): Promise<RainBucket> {
    return axios.post('/rain-buckets', data)
  },

  // ---- 雨量读数（rec 本） ----
  listReadings(params?: RainReadingQuery): Promise<RainReading[]> {
    return axios.get('/rain-readings', { params: params || {} })
  },
  record(data: { bucketId: number; readDate?: string; millimeters: number; remark?: string }): Promise<RainReading> {
    return axios.post('/rain-readings', data)
  },
  updateMillimeters(id: number, data: { millimeters: number; expectedVersion: number }): Promise<RainReading> {
    return axios.put(`/rain-readings/${id}`, data)
  },
  voidReading(id: number, data: { expectedVersion: number; voidReason: string }): Promise<RainReading> {
    return axios.post(`/rain-readings/${id}/void`, data)
  },

  // ---- 气象卡片 ----
  listCards(params?: { from?: string | null; to?: string | null }): Promise<RainDailyTotal[]> {
    return axios.get('/rain-cards', { params: params || {} })
  }
}
