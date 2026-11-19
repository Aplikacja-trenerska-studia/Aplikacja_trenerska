import type { UserRole } from '../types/auth'
import type { AccountStatus, AdminUser } from '../types/admin'

export type AdminFilters = { q: string; role: UserRole | ''; status: AccountStatus | ''; page: number }
const roles = ['USER', 'TRAINER', 'ADMIN']
const statuses = ['ACTIVE', 'PENDING', 'BLOCKED']
const pageSize = 20
const maximumPage = Math.floor(2147483647 / pageSize)

export function readAdminFilters(params: URLSearchParams): AdminFilters {
  const role = params.get('role') || ''
  const status = params.get('status') || ''
  const page = params.get('page') || '0'
  return {
    q: (params.get('q') || '').trim().slice(0, 255),
    role: roles.includes(role) ? role as UserRole : '',
    status: statuses.includes(status) ? status as AccountStatus : '',
    page: /^\d{1,10}$/.test(page) && Number(page) <= maximumPage ? Number(page) : 0,
  }
}

export function adminSearchParams(filters: AdminFilters, page = filters.page) {
  const params = new URLSearchParams()
  if (filters.q.trim()) params.set('q', filters.q.trim().slice(0, 255))
  if (filters.role) params.set('role', filters.role)
  if (filters.status) params.set('status', filters.status)
  if (page > 0) params.set('page', String(page))
  return params
}

export function adminUsersPath(filters: AdminFilters) {
  const params = adminSearchParams(filters)
  params.set('page', String(filters.page))
  params.set('size', String(pageSize))
  return `/api/admin/users?${params}`
}

export function accountStatus(user: Pick<AdminUser, 'enabled' | 'blocked'>): AccountStatus {
  return user.blocked ? 'BLOCKED' : user.enabled ? 'ACTIVE' : 'PENDING'
}

export function canChangeRole(user: AdminUser, actorId: number, role: UserRole) {
  return user.id !== actorId && user.enabled && user.role !== role && !(role === 'ADMIN' && user.blocked)
}
