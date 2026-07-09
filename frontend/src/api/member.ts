import axios from './axios'

export interface TeamMember {
  id: number
  memberNo: string
  memberName: string
  phone: string
  position: string
  teamId: number
  teamName?: string
}

export const memberApi = {
  getAll(): Promise<TeamMember[]> {
    return axios.get('/members')
  },
  getById(id: number): Promise<TeamMember> {
    return axios.get(`/members/${id}`)
  },
  getByMemberNo(memberNo: string): Promise<TeamMember> {
    return axios.get(`/members/by-no/${memberNo}`)
  },
  getByTeamId(teamId: number): Promise<TeamMember[]> {
    return axios.get(`/members/by-team/${teamId}`)
  },
  create(data: Omit<TeamMember, 'id'>): Promise<TeamMember> {
    return axios.post('/members', data)
  },
  update(id: number, data: Omit<TeamMember, 'id'>): Promise<TeamMember> {
    return axios.put(`/members/${id}`, data)
  },
  delete(id: number): Promise<void> {
    return axios.delete(`/members/${id}`)
  }
}