import type { UserRole } from './auth'

export type AdminUser = {
  id: number
  email: string
  displayName: string
  role: UserRole
  enabled: boolean
  blocked: boolean
  createdAt: string
}

export type AdminOverview = {
  users: number
  activeUsers: number
  trainers: number
  admins: number
  plans: number
  results: number
  groups: number
}

export type AdminUserPage = {
  content: AdminUser[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type AccountStatus = 'ACTIVE' | 'PENDING' | 'BLOCKED'

export const adminRoleLabels: Record<UserRole, string> = {
  USER: 'Użytkownik', TRAINER: 'Trener', ADMIN: 'Administrator',
}

export const accountStatusLabels: Record<AccountStatus, string> = {
  ACTIVE: 'Aktywne', PENDING: 'Oczekujące na potwierdzenie', BLOCKED: 'Zablokowane',
}
