import axios from './axios'

/** 样品袋状态：1=在途，2=办结（出站） */
export type SampleBagStatus = 1 | 2

export interface SampleBag {
  id: number
  bagNo: string
  teamId: number
  teamName?: string
  memberId: number
  memberNo?: string
  memberName?: string
  bagWeight: number
  /** 送检日 yyyy-MM-dd */
  submitDate: string
  status?: SampleBagStatus
  completeTime?: string | null
}

export interface SampleBagQuery {
  teamId?: number | null
  memberId?: number | null
  status?: number | null
}

export const sampleBagApi = {
  getAll(params?: SampleBagQuery): Promise<SampleBag[]> {
    return axios.get('/sample-bags', { params: params || {} })
  },
  getById(id: number): Promise<SampleBag> {
    return axios.get(`/sample-bags/${id}`)
  },
  register(data: Omit<SampleBag, 'id' | 'teamName' | 'memberNo' | 'memberName' | 'status' | 'completeTime'>): Promise<SampleBag> {
    return axios.post('/sample-bags', data)
  },
  update(id: number, data: Partial<SampleBag>): Promise<SampleBag> {
    return axios.put(`/sample-bags/${id}`, data)
  },
  complete(id: number): Promise<SampleBag> {
    return axios.put(`/sample-bags/${id}/complete`)
  },
  returnToTransit(id: number): Promise<SampleBag> {
    return axios.put(`/sample-bags/${id}/return-transit`)
  }
}
