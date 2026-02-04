"use client"

import React from "react"
import { useState } from "react"
import { useRouter } from "next/navigation"
import { Eye, EyeOff, Lock, Mail, Zap } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Checkbox } from "@/components/ui/checkbox"

export function LoginForm() {
  const router = useRouter()
  const [showPassword, setShowPassword] = useState(false)
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [isLoading, setIsLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsLoading(true)
    // Simula autenticação
    await new Promise((resolve) => setTimeout(resolve, 1500))
    setIsLoading(false)
    // Redireciona para o dashboard
    router.push("/dashboard")
  }

  return (
    <div className="w-full max-w-md">
      {/* Logo e Título */}
      <div className="mb-10 text-center">
        <div className="mx-auto mb-6 flex h-16 w-16 items-center justify-center rounded-2xl bg-foreground">
          <Zap className="h-8 w-8 text-background" />
        </div>
        <h1 className="text-balance text-3xl font-bold tracking-tight text-foreground">
          JVA Montagens
        </h1>
        <p className="mt-2 text-muted-foreground">
          Acesse o painel de gestão
        </p>
      </div>

      {/* Formulário */}
      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="space-y-2">
          <Label htmlFor="email" className="text-sm font-medium text-foreground">
            E-mail
          </Label>
          <div className="relative">
            <Mail className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              id="email"
              type="email"
              placeholder="seu@email.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="h-12 pl-10 bg-card border-border placeholder:text-muted-foreground/60 focus-visible:ring-primary"
              required
            />
          </div>
        </div>

        <div className="space-y-2">
          <Label htmlFor="password" className="text-sm font-medium text-foreground">
            Senha
          </Label>
          <div className="relative">
            <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              id="password"
              type={showPassword ? "text" : "password"}
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="h-12 pl-10 pr-10 bg-card border-border placeholder:text-muted-foreground/60 focus-visible:ring-primary"
              required
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
              aria-label={showPassword ? "Ocultar senha" : "Mostrar senha"}
            >
              {showPassword ? (
                <EyeOff className="h-4 w-4" />
              ) : (
                <Eye className="h-4 w-4" />
              )}
            </button>
          </div>
        </div>

        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Checkbox id="remember" />
            <Label
              htmlFor="remember"
              className="text-sm font-normal text-muted-foreground cursor-pointer"
            >
              Lembrar de mim
            </Label>
          </div>
          <a
            href="#"
            className="text-sm font-medium text-primary hover:text-primary/80 transition-colors"
          >
            Esqueceu a senha?
          </a>
        </div>

        <Button
          type="submit"
          className="h-12 w-full text-base font-medium"
          disabled={isLoading}
        >
          {isLoading ? (
            <span className="flex items-center gap-2">
              <span className="h-4 w-4 animate-spin rounded-full border-2 border-primary-foreground border-t-transparent" />
              Entrando...
            </span>
          ) : (
            "Entrar"
          )}
        </Button>
      </form>

      {/* Rodapé */}
      <p className="mt-8 text-center text-sm text-muted-foreground">
        Precisa de acesso?{" "}
        <a
          href="#"
          className="font-medium text-foreground hover:text-foreground/80 transition-colors underline underline-offset-4"
        >
          Fale com o administrador
        </a>
      </p>
    </div>
  )
}
