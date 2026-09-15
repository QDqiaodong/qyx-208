import axios from './axios'

export interface SurveyTeam {
  id: number
  teamName: string
  teamCode: string
  leaderName: string
  leaderPhone: string
  description: string
  maxLoadCapacity: number
}

export interface TeamAssetOverview {
  teamId: number
  teamName: string
  teamCode: string
  workstationCount: number
  maxLoadCapacity: number
  totalLoadCapacity: number
  remainingLoadCapacity: number
  workstations: any[]
}

export const teamApi = {
  getAll(): Promise<SurveyTeam[]> {
    return axios.get('/teams')
  },
  getById(id: number): Promise<SurveyTeam> {
    return axios.get(`/teams/${id}`)
  },
  getByCode(teamCode: string): Promise<SurveyTeam> {
    return axios.get(`/teams/by-code/${teamCode}`)
  },
  create(data: Omit<SurveyTeam, 'id'>): Promise<SurveyTeam> {
    return axios.post('/teams', data)
  },
  update(id: number, data: Omit<SurveyTeam, 'id'>): Promise<SurveyTeam> {
    return axios.put(`/teams/${id}`, data)
  },
  delete(id: number): Promise<void> {
    return axios.delete(`/teams/${id}`)
  },
  getTeamAssetOverview(id: number): Promise<TeamAssetOverview> {
    return axios.get(`/teams/${id}/asset-overview`)
  },
  getAllTeamAssetOverview(): Promise<TeamAssetOverview[]> {
    return axios.get('/teams/asset-overview')
  }
}
