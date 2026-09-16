import axios from './axios'

export interface TransferNotification {
  id: number
  teamId: number
  teamName: string
  workstationId: number
  workstationNo: string
  transferId: number
  /** 1=调出通知；2=调入通知 */
  direction: number
  counterpartTeamName: string
  title: string
  content: string
  readStatus: number
  readTime?: string | null
  createTime: string
}

export const notificationApi = {
  getByTeam(teamId: number, unreadOnly = false): Promise<TransferNotification[]> {
    return axios.get(`/teams/${teamId}/notifications`, {
      params: unreadOnly ? { unreadOnly: true } : {}
    })
  },
  getUnreadCount(teamId: number): Promise<number> {
    return axios.get(`/teams/${teamId}/notifications/unread-count`)
  },
  getAllUnreadCounts(): Promise<Record<string, number>> {
    return axios.get('/notifications/unread-counts')
  },
  markAsRead(teamId: number, notificationId: number): Promise<void> {
    return axios.put(`/teams/${teamId}/notifications/${notificationId}/read`)
  },
  markAllAsRead(teamId: number): Promise<number> {
    return axios.put(`/teams/${teamId}/notifications/read-all`)
  }
}
