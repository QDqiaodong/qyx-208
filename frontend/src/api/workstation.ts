import axios from './axios'

export interface Workstation {
  id: number
  workstationNo: string
  loadCapacity: number
  workstationType: string
  adaptStation: string
  currentTeamId: number | null
  currentTeamName?: string | null
}

export interface TransferDTO {
  workstationId: number
  toTeamId: number
  transferReason: string
  operator: string
}

export const workstationApi = {
  getAll(): Promise<Workstation[]> {
    return axios.get('/workstations')
  },
  getById(id: number): Promise<Workstation> {
    return axios.get(`/workstations/${id}`)
  },
  getByNo(workstationNo: string): Promise<Workstation> {
    return axios.get(`/workstations/by-no/${workstationNo}`)
  },
  getByTeamId(teamId: number): Promise<Workstation[]> {
    return axios.get(`/workstations/by-team/${teamId}`)
  },
  create(data: Omit<Workstation, 'id'>): Promise<Workstation> {
    return axios.post('/workstations', data)
  },
  update(id: number, data: Omit<Workstation, 'id'>): Promise<Workstation> {
    return axios.put(`/workstations/${id}`, data)
  },
  delete(id: number): Promise<void> {
    return axios.delete(`/workstations/${id}`)
  },
  transfer(data: TransferDTO): Promise<void> {
    return axios.post('/workstations/transfer', data)
  },
  getTransferHistory(id: number): Promise<any[]> {
    return axios.get(`/workstations/${id}/transfer-history`)
  },
  getLoadCapacity(workstationNo: string): Promise<number> {
    return axios.get(`/workstations/${workstationNo}/load-capacity`)
  }
}