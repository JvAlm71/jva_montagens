"use client"

import { FormEvent, useEffect, useMemo, useState } from "react"
import Link from "next/link"
import { LogOut, Pencil, Plus, RefreshCcw, X, Zap } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { useAuthGuard } from "@/hooks/use-auth-guard"
import {
  ApiError,
  createEmployee,
  getEmployees,
  updateEmployee,
  type Employee,
  type JobRole,
} from "@/lib/api"

function formatCurrency(value?: number | null) {
  if (value == null) return "-"
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
    maximumFractionDigits: 2,
  }).format(value)
}

const ROLE_LABELS: Record<JobRole, string> = {
  ASSEMBLER: "Montador",
  LEADER: "Líder",
  ADMINISTRATOR: "Administrador",
}

function emptyEmployeeForm() {
  return {
    name: "",
    role: "ASSEMBLER" as JobRole,
    pixKey: "",
    govEmail: "",
    govPassword: "",
    dailyRate: "",
    pricePerMeter: "",
    userCpf: "",
    active: true,
  }
}

function parseOptionalNumber(value: string): number | undefined {
  if (!value.trim()) return undefined
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}

export default function EmployeesPage() {
  const { token, isCheckingAuth, logout } = useAuthGuard()
  const [employees, setEmployees] = useState<Employee[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  const [form, setForm] = useState(emptyEmployeeForm())
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editForm, setEditForm] = useState(emptyEmployeeForm())

  const loadEmployees = useMemo(
    () => async () => {
      if (!token) return
      try {
        setLoading(true)
        setError(null)
        const fetched = await getEmployees(token)
        setEmployees(fetched)
      } catch (err) {
        if (err instanceof ApiError) setError(err.message)
        else setError("Nao foi possivel carregar funcionarios.")
      } finally {
        setLoading(false)
      }
    },
    [token]
  )

  useEffect(() => {
    if (!token) return
    loadEmployees()
  }, [token, loadEmployees])

  const handleCreate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!token) return

    try {
      setError(null)
      setMessage(null)
      await createEmployee(token, {
        name: form.name,
        role: form.role,
        pixKey: form.pixKey || undefined,
        govEmail: form.govEmail || undefined,
        govPassword: form.govPassword || undefined,
        dailyRate: parseOptionalNumber(form.dailyRate),
        pricePerMeter: parseOptionalNumber(form.pricePerMeter),
        userCpf: form.userCpf || undefined,
        active: form.active,
      })
      setForm(emptyEmployeeForm())
      setMessage("Funcionario cadastrado com sucesso.")
      await loadEmployees()
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError("Falha ao cadastrar funcionario.")
    }
  }

  const startEdit = (employee: Employee) => {
    setEditingId(employee.id)
    setEditForm({
      name: employee.name,
      role: employee.role,
      pixKey: employee.pixKey || "",
      govEmail: employee.govEmail || "",
      govPassword: employee.govPassword || "",
      dailyRate: employee.dailyRate != null ? String(employee.dailyRate) : "",
      pricePerMeter: employee.pricePerMeter != null ? String(employee.pricePerMeter) : "",
      userCpf: employee.user?.cpf || "",
      active: employee.active,
    })
  }

  const handleUpdate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!token || !editingId) return

    try {
      setError(null)
      setMessage(null)
      await updateEmployee(token, editingId, {
        name: editForm.name,
        role: editForm.role,
        pixKey: editForm.pixKey || undefined,
        govEmail: editForm.govEmail || undefined,
        govPassword: editForm.govPassword || undefined,
        dailyRate: parseOptionalNumber(editForm.dailyRate),
        pricePerMeter: parseOptionalNumber(editForm.pricePerMeter),
        userCpf: editForm.userCpf,
        active: editForm.active,
      })
      setEditingId(null)
      setMessage("Funcionario atualizado com sucesso.")
      await loadEmployees()
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError("Falha ao atualizar funcionario.")
    }
  }

  if (isCheckingAuth) {
    return (
      <div className="flex min-h-screen items-center justify-center text-muted-foreground">
        Validando sessao...
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-background">
      <header className="border-b border-border bg-card">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-foreground">
              <Zap className="h-5 w-5 text-background" />
            </div>
            <span className="text-lg font-bold tracking-tight text-foreground">JVA Montagens</span>
          </div>
          <div className="flex items-center gap-2">
            <Link href="/dashboard">
              <Button variant="ghost" size="sm">Dashboard</Button>
            </Link>
            <Link href="/parks">
              <Button variant="ghost" size="sm">Parques</Button>
            </Link>
            <Link href="/financial">
              <Button variant="ghost" size="sm">Financeiro</Button>
            </Link>
            <Button variant="ghost" size="sm" className="gap-2" onClick={loadEmployees}>
              <RefreshCcw className="h-4 w-4" />
              Atualizar
            </Button>
            <Button variant="ghost" size="sm" className="gap-2 text-muted-foreground" onClick={logout}>
              <LogOut className="h-4 w-4" />
              Sair
            </Button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-foreground">Funcionarios</h1>
          <p className="mt-1 text-muted-foreground">
            Cadastre e gerencie dados operacionais e credenciais do gov/MEI.
          </p>
        </div>

        {error && (
          <Card className="mb-6 border-red-200">
            <CardContent className="py-4 text-sm font-medium text-red-600">{error}</CardContent>
          </Card>
        )}

        {message && (
          <Card className="mb-6 border-green-200">
            <CardContent className="py-4 text-sm font-medium text-green-700">{message}</CardContent>
          </Card>
        )}

        <Card className="mb-6">
          <CardHeader>
            <CardTitle className="text-base">Novo Funcionario</CardTitle>
          </CardHeader>
          <CardContent>
            <form className="grid gap-3 md:grid-cols-2 lg:grid-cols-4" onSubmit={handleCreate}>
              <div className="space-y-1 lg:col-span-2">
                <Label>Nome</Label>
                <Input value={form.name} onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))} required />
              </div>
              <div className="space-y-1">
                <Label>Funcao</Label>
                <select className="h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm" value={form.role} onChange={(e) => setForm((p) => ({ ...p, role: e.target.value as JobRole }))}>
                  <option value="ASSEMBLER">Montador</option>
                  <option value="LEADER">Líder</option>
                  <option value="ADMINISTRATOR">Administrador</option>
                </select>
              </div>
              <div className="space-y-1">
                <Label>Ativo</Label>
                <select className="h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm" value={form.active ? "true" : "false"} onChange={(e) => setForm((p) => ({ ...p, active: e.target.value === "true" }))}>
                  <option value="true">SIM</option>
                  <option value="false">NAO</option>
                </select>
              </div>
              <div className="space-y-1">
                <Label>PIX</Label>
                <Input value={form.pixKey} onChange={(e) => setForm((p) => ({ ...p, pixKey: e.target.value }))} />
              </div>
              <div className="space-y-1">
                <Label>Diaria (ajudante)</Label>
                <Input type="number" step="0.01" value={form.dailyRate} onChange={(e) => setForm((p) => ({ ...p, dailyRate: e.target.value }))} required={form.role === "ASSEMBLER"} />
              </div>
              <div className="space-y-1">
                <Label>Preco m2 (lider)</Label>
                <Input type="number" step="0.01" value={form.pricePerMeter} onChange={(e) => setForm((p) => ({ ...p, pricePerMeter: e.target.value }))} required={form.role === "LEADER"} />
              </div>
              <div className="space-y-1">
                <Label>CPF Usuario (opcional)</Label>
                <Input value={form.userCpf} onChange={(e) => setForm((p) => ({ ...p, userCpf: e.target.value }))} placeholder="00000000000" />
              </div>
              <div className="space-y-1 lg:col-span-2">
                <Label>Email Gov</Label>
                <Input type="email" value={form.govEmail} onChange={(e) => setForm((p) => ({ ...p, govEmail: e.target.value }))} />
              </div>
              <div className="space-y-1 lg:col-span-2">
                <Label>Senha Gov</Label>
                <Input value={form.govPassword} onChange={(e) => setForm((p) => ({ ...p, govPassword: e.target.value }))} />
              </div>
              <div className="lg:col-span-4">
                <Button type="submit" className="w-full gap-2">
                  <Plus className="h-4 w-4" />
                  Cadastrar Funcionario
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Funcionarios Cadastrados</CardTitle>
          </CardHeader>
          <CardContent>
            {loading ? (
              <p className="text-sm text-muted-foreground">Carregando funcionarios...</p>
            ) : employees.length === 0 ? (
              <p className="text-sm text-muted-foreground">Nenhum funcionario cadastrado.</p>
            ) : (
              <div className="space-y-3">
                {employees.map((employee) => (
                  <div key={employee.id} className="rounded-md border border-border p-3">
                    {editingId === employee.id ? (
                      <form className="grid gap-2 md:grid-cols-2" onSubmit={handleUpdate}>
                        <Input value={editForm.name} onChange={(e) => setEditForm((p) => ({ ...p, name: e.target.value }))} placeholder="Nome" required />
                        <select className="h-9 rounded-md border border-input bg-background px-2 text-sm" value={editForm.role} onChange={(e) => setEditForm((p) => ({ ...p, role: e.target.value as JobRole }))}>
                          <option value="ASSEMBLER">Montador</option>
                          <option value="LEADER">Líder</option>
                          <option value="ADMINISTRATOR">Administrador</option>
                        </select>
                        <Input value={editForm.pixKey} onChange={(e) => setEditForm((p) => ({ ...p, pixKey: e.target.value }))} placeholder="PIX" />
                        <Input type="number" step="0.01" value={editForm.dailyRate} onChange={(e) => setEditForm((p) => ({ ...p, dailyRate: e.target.value }))} placeholder="Diaria" required={editForm.role === "ASSEMBLER"} />
                        <Input type="number" step="0.01" value={editForm.pricePerMeter} onChange={(e) => setEditForm((p) => ({ ...p, pricePerMeter: e.target.value }))} placeholder="Preco m2" required={editForm.role === "LEADER"} />
                        <Input value={editForm.userCpf} onChange={(e) => setEditForm((p) => ({ ...p, userCpf: e.target.value }))} placeholder="CPF usuario (vazio remove)" />
                        <Input type="email" value={editForm.govEmail} onChange={(e) => setEditForm((p) => ({ ...p, govEmail: e.target.value }))} placeholder="Email gov" />
                        <Input value={editForm.govPassword} onChange={(e) => setEditForm((p) => ({ ...p, govPassword: e.target.value }))} placeholder="Senha gov" />
                        <select className="h-9 rounded-md border border-input bg-background px-2 text-sm" value={editForm.active ? "true" : "false"} onChange={(e) => setEditForm((p) => ({ ...p, active: e.target.value === "true" }))}>
                          <option value="true">ATIVO</option>
                          <option value="false">INATIVO</option>
                        </select>
                        <div className="flex gap-2 md:col-span-2">
                          <Button type="submit" size="sm">Salvar</Button>
                          <Button type="button" size="sm" variant="ghost" onClick={() => setEditingId(null)}>
                            <X className="h-4 w-4" />
                          </Button>
                        </div>
                      </form>
                    ) : (
                      <div className="flex items-start justify-between gap-3">
                        <div className="space-y-1">
                          <p className="font-medium text-foreground">
                            {employee.name} ({ROLE_LABELS[employee.role] || employee.role})
                          </p>
                          <p className="text-sm text-muted-foreground">ID: {employee.id} | {employee.active ? "Ativo" : "Inativo"}</p>
                          <p className="text-sm text-muted-foreground">PIX: {employee.pixKey || "-"}</p>
                          <p className="text-sm text-muted-foreground">Diaria: {formatCurrency(employee.dailyRate)}</p>
                          <p className="text-sm text-muted-foreground">Preco m2: {formatCurrency(employee.pricePerMeter)}</p>
                          <p className="text-sm text-muted-foreground">Email Gov: {employee.govEmail || "-"}</p>
                          <p className="text-sm text-muted-foreground">Senha Gov: {employee.govPassword || "-"}</p>
                          <p className="text-sm text-muted-foreground">CPF Usuario: {employee.user?.cpf || "-"}</p>
                        </div>
                        <Button size="sm" variant="ghost" onClick={() => startEdit(employee)}>
                          <Pencil className="h-4 w-4" />
                        </Button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </main>
    </div>
  )
}
