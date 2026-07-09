import axios from './axios'
import type { Workstation } from './workstation'
import type { TeamAssetOverview } from './team'

export const queryApi = {
  getWorkstationsByMemberNo(memberNo: string): Promise<Workstation[]> {
    return axios.get(`/query/member/${memberNo}/workstations`)
  },
  getTeamAssetOverviewByMemberNo(memberNo: string): Promise<TeamAssetOverview> {
    return axios.get(`/query/member/${memberNo}/team-asset-overview`)
  }
}