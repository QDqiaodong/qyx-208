<template>
  <div class="app-container">
    <el-container>
      <el-aside width="220px" class="aside">
        <div class="logo">
          <h2>地质勘测台账系统</h2>
        </div>
        <el-menu :default-active="activeTab" class="menu" @select="handleTabChange">
          <el-menu-item index="workstation">
            <el-icon><component :is="Icons.Laptop" /></el-icon>
            <span>操作台管理</span>
          </el-menu-item>
          <el-menu-item index="team">
            <el-icon><component :is="Icons.UserFilled" /></el-icon>
            <span>勘测小队管理</span>
          </el-menu-item>
          <el-menu-item index="member">
            <el-icon><component :is="Icons.User" /></el-icon>
            <span>队员管理</span>
          </el-menu-item>
          <el-menu-item index="sample-bag">
            <el-icon><component :is="Icons.Box" /></el-icon>
            <span>样品袋送检登记</span>
          </el-menu-item>
          <el-menu-item index="query">
            <el-icon><component :is="Icons.Search" /></el-icon>
            <span>队员查询资产</span>
          </el-menu-item>
          <el-menu-item index="overview">
            <el-icon><component :is="Icons.DataAnalysis" /></el-icon>
            <span>小队资产总览</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-container>
        <el-header class="header">
          <h1>{{ pageTitle }}</h1>
        </el-header>
        <el-main class="main">
          <WorkstationView v-if="activeTab === 'workstation'" />
          <TeamView v-else-if="activeTab === 'team'" />
          <MemberView v-else-if="activeTab === 'member'" />
          <SampleBagView v-else-if="activeTab === 'sample-bag'" />
          <QueryView v-else-if="activeTab === 'query'" />
          <OverviewView v-else-if="activeTab === 'overview'" />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, markRaw } from 'vue'
import { Monitor, UserFilled, User, Search, DataAnalysis, Box } from '@element-plus/icons-vue'
import WorkstationView from './views/WorkstationView.vue'
import TeamView from './views/TeamView.vue'
import MemberView from './views/MemberView.vue'
import SampleBagView from './views/SampleBagView.vue'
import QueryView from './views/QueryView.vue'
import OverviewView from './views/OverviewView.vue'

const Icons = {
  Laptop: markRaw(Monitor),
  UserFilled: markRaw(UserFilled),
  User: markRaw(User),
  Search: markRaw(Search),
  DataAnalysis: markRaw(DataAnalysis),
  Box: markRaw(Box)
}

const activeTab = ref('workstation')

const pageTitle = computed(() => {
  const titles: Record<string, string> = {
    workstation: '操作台管理',
    team: '勘测小队管理',
    member: '队员管理',
    'sample-bag': '样品袋送检登记',
    query: '按队员编号查询资产',
    overview: '小队资产总览'
  }
  return titles[activeTab.value] || ''
})

const handleTabChange = (key: string) => {
  activeTab.value = key
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.app-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.aside {
  background: linear-gradient(180deg, #1a365d 0%, #2c5282 100%);
  color: white;
}

.logo {
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo h2 {
  font-size: 16px;
  font-weight: 600;
}

.menu {
  border-right: none;
}

.menu :deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.9);
  height: 50px;
  line-height: 50px;
}

.menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.1);
}

.menu :deep(.el-menu-item.is-active) {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.header {
  background: white;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  align-items: center;
  padding-left: 20px;
}

.header h1 {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.main {
  background: #f5f5f5;
  padding: 20px;
  overflow-y: auto;
}
</style>