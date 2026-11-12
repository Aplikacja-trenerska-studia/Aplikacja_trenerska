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
  errors?: Record<string, string>
}
