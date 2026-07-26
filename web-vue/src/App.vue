<template>
  <router-view />
  <FloatingAssistant />
</template>

<script setup>
import { onMounted } from 'vue'
import FloatingAssistant from './components/FloatingAssistant.vue'

onMounted(() => {
  // 启动时应用已保存的主题
  const savedTheme = localStorage.getItem('theme') || 'light'
  applyTheme(savedTheme)
})

function applyTheme(theme) {
  const root = document.documentElement
  if (theme === 'auto') {
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    root.setAttribute('data-theme', prefersDark ? 'dark' : 'light')
  } else {
    root.setAttribute('data-theme', theme)
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  width: 100%;
  height: 100%;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}

/* ===== 暗色模式全局样式 ===== */
html[data-theme="dark"] body {
  background-color: #1a1a2e;
  color: #e0e0e0;
}

html[data-theme="dark"] .el-card {
  background-color: #16213e;
  border-color: #2d2e3e;
  color: #e0e0e0;
}

html[data-theme="dark"] .el-card__header {
  border-bottom-color: #2d2e3e;
  color: #e0e0e0;
}

html[data-theme="dark"] .el-card__body {
  color: #e0e0e0;
}

html[data-theme="dark"] .el-table {
  --el-table-bg-color: #16213e;
  --el-table-tr-bg-color: #16213e;
  --el-table-header-bg-color: #2d2e3e;
  --el-table-text-color: #e0e0e0;
  --el-table-border-color: #2d2e3e;
  --el-table-row-hover-bg-color: #1f2b47;
}

html[data-theme="dark"] .el-table th.el-table__cell {
  background-color: #2d2e3e;
  color: #e0e0e0;
}

html[data-theme="dark"] .el-input__wrapper {
  background-color: #2d2e3e;
  box-shadow: 0 0 0 1px #3d3e4e inset;
}

html[data-theme="dark"] .el-input__wrapper:hover {
  box-shadow: 0 0 0 1px #409eff inset;
}

html[data-theme="dark"] .el-input__inner {
  color: #e0e0e0;
}

html[data-theme="dark"] .el-input__inner::placeholder {
  color: #888;
}

html[data-theme="dark"] .el-select .el-input__wrapper {
  background-color: #2d2e3e;
}

html[data-theme="dark"] .el-textarea__inner {
  background-color: #2d2e3e;
  color: #e0e0e0;
  border-color: #3d3e4e;
}

html[data-theme="dark"] .el-menu {
  background-color: #0f0f1a !important;
}

html[data-theme="dark"] .el-menu-item {
  color: #a0a3bd !important;
}

html[data-theme="dark"] .el-menu-item:hover {
  background-color: #1a2040 !important;
}

html[data-theme="dark"] .el-menu-item.is-active {
  color: #409eff !important;
}

html[data-theme="dark"] .el-sub-menu__title {
  color: #a0a3bd !important;
}

html[data-theme="dark"] .el-dialog {
  background-color: #16213e;
  color: #e0e0e0;
}

html[data-theme="dark"] .el-dialog__header {
  border-bottom-color: #2d2e3e;
}

html[data-theme="dark"] .el-dialog__title {
  color: #e0e0e0;
}

html[data-theme="dark"] .el-pagination {
  --el-pagination-bg-color: #16213e;
  --el-pagination-text-color: #e0e0e0;
  --el-pagination-button-bg-color: #2d2e3e;
  --el-pagination-button-color: #e0e0e0;
  --el-pagination-hover-color: #409eff;
}

html[data-theme="dark"] .el-tag {
  border-color: #3d3e4e;
}

html[data-theme="dark"] .el-form-item__label {
  color: #e0e0e0;
}

html[data-theme="dark"] .el-divider__text {
  background-color: #16213e;
  color: #a0a3bd;
}

html[data-theme="dark"] .el-divider {
  border-top-color: #2d2e3e;
}

html[data-theme="dark"] .el-empty__description p {
  color: #888;
}

html[data-theme="dark"] .el-upload-dragger {
  background-color: #1a2040;
  border-color: #3d3e4e;
}

html[data-theme="dark"] .el-upload-dragger:hover {
  border-color: #409eff;
}

html[data-theme="dark"] .el-dropdown-menu {
  background-color: #16213e;
  border-color: #2d2e3e;
}

html[data-theme="dark"] .el-dropdown-menu__item {
  color: #e0e0e0;
}

html[data-theme="dark"] .el-dropdown-menu__item:hover {
  background-color: #1f2b47;
  color: #409eff;
}

html[data-theme="dark"] .el-alert {
  border-color: #3d3e4e;
}

html[data-theme="dark"] .el-checkbox__label {
  color: #e0e0e0;
}

html[data-theme="dark"] .el-radio__label {
  color: #e0e0e0;
}

html[data-theme="dark"] .el-progress-bar__outer {
  background-color: #2d2e3e;
}

/* ===== 亮色模式（默认） ===== */
html[data-theme="light"] body, body {
  background-color: #f5f7fa;
  color: #333;
}

/* ===== 暗色模式：Layout 覆盖 ===== */
html[data-theme="dark"] .content,
html[data-theme="dark"] .mobile-content {
  background-color: #1a1a2e;
}

html[data-theme="dark"] .sidebar {
  background-color: #0f0f1a;
}

html[data-theme="dark"] .header {
  background-color: #16213e;
  border-bottom-color: #2d2e3e;
}

html[data-theme="dark"] .mobile-header {
  background-color: #0f0f1a;
}

html[data-theme="dark"] .logo {
  border-bottom-color: #2d2e3e;
}

html[data-theme="dark"] .nickname {
  color: #a0a3bd;
}

html[data-theme="dark"] .el-radio-button__inner {
  background-color: #2d2e3e;
  border-color: #3d3e4e;
  color: #e0e0e0;
}

html[data-theme="dark"] .el-radio-button__original-radio:checked + .el-radio-button__inner {
  background-color: #409eff;
  border-color: #409eff;
  color: #fff;
}

html[data-theme="dark"] .el-select-dropdown {
  background-color: #16213e;
  border-color: #2d2e3e;
}

html[data-theme="dark"] .el-select-dropdown__item {
  color: #e0e0e0;
}

html[data-theme="dark"] .el-select-dropdown__item.hover,
html[data-theme="dark"] .el-select-dropdown__item:hover {
  background-color: #1f2b47;
}

html[data-theme="dark"] .el-popper {
  background-color: #16213e;
  border-color: #2d2e3e;
}

html[data-theme="dark"] .el-overlay {
  background-color: rgba(0, 0, 0, 0.6);
}

html[data-theme="dark"] .el-message {
  background-color: #16213e;
  border-color: #2d2e3e;
  color: #e0e0e0;
}

/* 修复下拉框/弹出层在所有模式下的可见性 */
.el-select-dropdown, .el-popper, .el-dropdown-menu {
  z-index: 9999 !important;
}

.el-select-dropdown__item {
  color: #333;
}

html[data-theme="dark"] .el-select-dropdown__item {
  color: #e0e0e0;
}

html[data-theme="dark"] .el-select-dropdown__item.selected {
  color: #409eff;
  font-weight: bold;
}

html[data-theme="dark"] .el-tag {
  background-color: #2d2e3e;
  border-color: #3d3e4e;
  color: #e0e0e0;
}

html[data-theme="dark"] .el-button--default {
  background-color: #2d2e3e;
  border-color: #3d3e4e;
  color: #e0e0e0;
}

html[data-theme="dark"] .el-button--default:hover {
  background-color: #3d4e6e;
  border-color: #409eff;
  color: #409eff;
}

html[data-theme="dark"] .el-input-number .el-input__wrapper {
  background-color: #2d2e3e;
}

html[data-theme="dark"] .el-radio-button__inner {
  background-color: #2d2e3e;
  border-color: #3d3e4e;
  color: #e0e0e0;
}

html[data-theme="dark"] .el-radio-button__original-radio:checked + .el-radio-button__inner {
  background-color: #409eff;
  color: #fff;
}

html[data-theme="dark"] .el-collapse-item__header {
  background-color: #16213e;
  color: #e0e0e0;
  border-color: #2d2e3e;
}

html[data-theme="dark"] .el-collapse-item__wrap {
  background-color: #16213e;
  border-color: #2d2e3e;
}
</style>
