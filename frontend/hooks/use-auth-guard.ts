"use client"

import { useEffect, useMemo, useState } from "react"
import { useRouter } from "next/navigation"
import { clearSession, getSessionUser, getToken } from "@/lib/auth-session"
import { getCurrentAdmin } from "@/lib/api"

type AuthState = {
  token: string
  user: {
    cpf: string
    email: string
    fullName: string
    employeeId: number
    employeeName: string
    role: string
  }
}

export function useAuthGuard() {
  const router = useRouter()
  const [state, setState] = useState<AuthState | null>(null)
  const [isCheckingAuth, setIsCheckingAuth] = useState(true)

  useEffect(() => {
    const token = getToken()
    const cachedUser = getSessionUser()

    if (!token || !cachedUser) {
      clearSession()
      router.replace("/")
      setIsCheckingAuth(false)
      return
    }

    const validate = async () => {
      try {
        const user = await getCurrentAdmin(token)
        setState({ token, user })
      } catch {
        clearSession()
        router.replace("/")
      } finally {
        setIsCheckingAuth(false)
      }
    }

    validate()
  }, [router])

  const logout = useMemo(
    () => () => {
      clearSession()
      router.replace("/")
    },
    [router]
  )

  return {
    token: state?.token || null,
    user: state?.user || null,
    isCheckingAuth,
    logout,
  }
}
