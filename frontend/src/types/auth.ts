export type UserRole = 'USER' | 'TRAINER' | 'ADMIN'

export type UserResponse = {
  id: number
  email: string
  displayName: string
  role: UserRole
}

export type AuthResponse = {
  accessToken: string
  tokenType: string
  expiresInMs: number
  user: UserResponse
}

export type ApiError = {
  message: string
  status?: number
  errors?: Record<string, string>
}

export type MessageResponse = { message: string }
