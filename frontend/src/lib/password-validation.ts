export function passwordError(password: string, repeated?: string): string | null {
  if (password.length < 8) return 'Hasło musi mieć co najmniej 8 znaków.'
  if (password.length > 72 || new TextEncoder().encode(password).length > 72) return 'Hasło jest za długie (maksymalnie 72 bajty; polskie znaki zajmują więcej miejsca).'
  if (repeated !== undefined && password !== repeated) return 'Wpisane hasła różnią się.'
  return null
}
