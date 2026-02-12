"use client"

const TOKEN_KEY = "jva_admin_token"
const USER_KEY = "jva_admin_user"

export type AdminSessionUser = {
  cpf: string
  email: string
  fullName: string
  employeeId: number
  employeeName: string
  role: string
}

export type AdminSession = {
  accessToken: string
  tokenType: string
  user: AdminSessionUser
}

export function saveSession(session: AdminSession, remember: boolean) {
  if (typeof window === "undefined") return

  const storage = remember ? window.localStorage : window.sessionStorage
  const alternateStorage = remember ? window.sessionStorage : window.localStorage

  storage.setItem(TOKEN_KEY, session.accessToken)
  storage.setItem(USER_KEY, JSON.stringify(session.user))

  alternateStorage.removeItem(TOKEN_KEY)
  alternateStorage.removeItem(USER_KEY)
}

export function getToken(): string | null {
  if (typeof window === "undefined") return null
  return window.localStorage.getItem(TOKEN_KEY) || window.sessionStorage.getItem(TOKEN_KEY)
}

export function getSessionUser(): AdminSessionUser | null {
  if (typeof window === "undefined") return null

  const rawUser =
    window.localStorage.getItem(USER_KEY) || window.sessionStorage.getItem(USER_KEY)
  if (!rawUser) return null

  try {
    return JSON.parse(rawUser) as AdminSessionUser
  } catch {
    return null
  }
}

export function clearSession() {
  if (typeof window === "undefined") return
  window.localStorage.removeItem(TOKEN_KEY)
  window.localStorage.removeItem(USER_KEY)
  window.sessionStorage.removeItem(TOKEN_KEY)
  window.sessionStorage.removeItem(USER_KEY)
}
