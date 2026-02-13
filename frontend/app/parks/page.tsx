"use client"

import { FormEvent, useEffect, useMemo, useState } from "react"
import Link from "next/link"
import { Building2, LogOut, Pencil, Plus, RefreshCcw, Trash2, X, Zap } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { useAuthGuard } from "@/hooks/use-auth-guard"
import {
  ApiError,
  createClient,
  createPark,
  deleteClient,
  deletePark,
  getClients,
  getParkOverview,
  getParks,
  updateClient,
  updatePark,
  type Client,
  type Park,
  type ParkFinancialOverview,
} from "@/lib/api"

function formatCurrency(value: number) {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
    maximumFractionDigits: 0,
  }).format(value)
}

export default function ParksPage() {
  const { token, isCheckingAuth, logout } = useAuthGuard()
  const [clients, setClients] = useState<Client[]>([])
  const [parks, setParks] = useState<Park[]>([])
  const [overviews, setOverviews] = useState<Record<number, ParkFinancialOverview>>({})
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const [clientForm, setClientForm] = useState({
    cnpj: "",
    name: "",
    contactPhone: "",
    email: "",
  })

  const [parkForm, setParkForm] = useState({
    name: "",
    city: "",
    state: "",
    clientCnpj: "",
  })

  const [editingClient, setEditingClient] = useState<Client | null>(null)
  const [editClientForm, setEditClientForm] = useState({ name: "", contactPhone: "", email: "" })

  const [editingPark, setEditingPark] = useState<Park | null>(null)
  const [editParkForm, setEditParkForm] = useState({ name: "", city: "", state: "", clientCnpj: "" })

  const loadData = useMemo(
    () => async () => {
      if (!token) return

      try {
        setLoading(true)
        setError(null)
        setMessage(null)

        const [fetchedClients, fetchedParks] = await Promise.all([
          getClients(token),
          getParks(token),
        ])

        const fetchedOverviews = await Promise.all(
          fetchedParks.map((park) => getParkOverview(token, park.id))
        )

        const overviewMap: Record<number, ParkFinancialOverview> = {}
        for (const overview of fetchedOverviews) {
          overviewMap[overview.parkId] = overview
        }

        setClients(fetchedClients)
        setParks(fetchedParks)
        setOverviews(overviewMap)

        if (!parkForm.clientCnpj && fetchedClients.length > 0) {
          setParkForm((prev) => ({ ...prev, clientCnpj: fetchedClients[0].cnpj }))
        }
      } catch (err) {
        if (err instanceof ApiError) {
          setError(err.message)
        } else {
          setError("Nao foi possivel carregar clientes e parques.")
        }
      } finally {
        setLoading(false)
      }
    },
    [token, parkForm.clientCnpj]
  )

  useEffect(() => {
    if (!token) return
    loadData()
  }, [token, loadData])

  const handleCreateClient = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!token) return

    try {
      setError(null)
      setMessage(null)
      await createClient(token, {
        cnpj: clientForm.cnpj,
        name: clientForm.name,
        contactPhone: clientForm.contactPhone || undefined,
        email: clientForm.email || undefined,
      })
      setClientForm({ cnpj: "", name: "", contactPhone: "", email: "" })
      setMessage("Cliente cadastrado com sucesso.")
      await loadData()
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
      } else {
        setError("Falha ao cadastrar cliente.")
      }
    }
  }

  const handleCreatePark = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!token) return

    try {
      setError(null)
      setMessage(null)
      await createPark(token, {
        name: parkForm.name,
        city: parkForm.city || undefined,
        state: parkForm.state || undefined,
        clientCnpj: parkForm.clientCnpj,
      })
      setParkForm((prev) => ({ ...prev, name: "", city: "", state: "" }))
      setMessage("Parque cadastrado com sucesso.")
      await loadData()
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
      } else {
        setError("Falha ao cadastrar parque.")
      }
    }
  }

  const startEditClient = (client: Client) => {
    setEditingClient(client)
    setEditClientForm({
      name: client.name,
      contactPhone: client.contactPhone || "",
      email: client.email || "",
    })
  }

  const handleUpdateClient = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!token || !editingClient) return
    try {
      setError(null)
      setMessage(null)
      await updateClient(token, editingClient.cnpj, {
        name: editClientForm.name,
        contactPhone: editClientForm.contactPhone || undefined,
        email: editClientForm.email || undefined,
      })
      setEditingClient(null)
      setMessage("Cliente atualizado com sucesso.")
      await loadData()
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError("Falha ao atualizar cliente.")
    }
  }

  const handleDeleteClient = async (cnpj: string) => {
    if (!token) return
    if (!confirm("Excluir este cliente? Todos os parques e dados associados serao removidos.")) return
    try {
      setError(null)
      setMessage(null)
      await deleteClient(token, cnpj)
      setMessage("Cliente excluido com sucesso.")
      await loadData()
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError("Falha ao excluir cliente.")
    }
  }

  const startEditPark = (park: Park) => {
    setEditingPark(park)
    setEditParkForm({
      name: park.name,
      city: park.city || "",
      state: park.state || "",
      clientCnpj: park.client?.cnpj || "",
    })
  }

  const handleUpdatePark = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!token || !editingPark) return
    try {
      setError(null)
      setMessage(null)
      await updatePark(token, editingPark.id, {
        name: editParkForm.name,
        city: editParkForm.city || undefined,
        state: editParkForm.state || undefined,
        clientCnpj: editParkForm.clientCnpj || undefined,
      })
      setEditingPark(null)
      setMessage("Parque atualizado com sucesso.")
      await loadData()
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError("Falha ao atualizar parque.")
    }
  }

  const handleDeletePark = async (parkId: number) => {
    if (!token) return
    if (!confirm("Excluir este parque? Todas as competencias e dados associados serao removidos.")) return
    try {
      setError(null)
      setMessage(null)
      await deletePark(token, parkId)
      setMessage("Parque excluido com sucesso.")
      await loadData()
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError("Falha ao excluir parque.")
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
            <span className="text-lg font-bold tracking-tight text-foreground">
              JVA Montagens
            </span>
          </div>
          <div className="flex items-center gap-2">
            <Link href="/dashboard">
              <Button variant="ghost" size="sm">
                Dashboard
              </Button>
            </Link>
            <Link href="/financial">
              <Button variant="ghost" size="sm">
                Financeiro
              </Button>
            </Link>
            <Link href="/employees">
              <Button variant="ghost" size="sm">
                Funcionarios
              </Button>
            </Link>
            <Button variant="ghost" size="sm" className="gap-2" onClick={loadData}>
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
          <h1 className="text-2xl font-bold text-foreground">Gestao de Parques</h1>
          <p className="mt-1 text-muted-foreground">
            Cadastre clientes, registre parques e acompanhe os totais por parque.
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

        <div className="mb-8 grid gap-6 lg:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Cadastrar Cliente</CardTitle>
            </CardHeader>
            <CardContent>
              <form className="space-y-4" onSubmit={handleCreateClient}>
                <div className="space-y-2">
                  <Label htmlFor="client-cnpj">CNPJ</Label>
                  <Input
                    id="client-cnpj"
                    value={clientForm.cnpj}
                    onChange={(event) =>
                      setClientForm((prev) => ({ ...prev, cnpj: event.target.value }))
                    }
                    placeholder="00.000.000/0000-00"
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="client-name">Nome</Label>
                  <Input
                    id="client-name"
                    value={clientForm.name}
                    onChange={(event) =>
                      setClientForm((prev) => ({ ...prev, name: event.target.value }))
                    }
                    placeholder="Nome do cliente"
                    required
                  />
                </div>
                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="space-y-2">
                    <Label htmlFor="client-phone">Telefone</Label>
                    <Input
                      id="client-phone"
                      value={clientForm.contactPhone}
                      onChange={(event) =>
                        setClientForm((prev) => ({ ...prev, contactPhone: event.target.value }))
                      }
                      placeholder="(00) 00000-0000"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="client-email">E-mail</Label>
                    <Input
                      id="client-email"
                      type="email"
                      value={clientForm.email}
                      onChange={(event) =>
                        setClientForm((prev) => ({ ...prev, email: event.target.value }))
                      }
                      placeholder="cliente@email.com"
                    />
                  </div>
                </div>
                <Button type="submit" className="gap-2">
                  <Plus className="h-4 w-4" />
                  Cadastrar Cliente
                </Button>
              </form>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Clientes Cadastrados</CardTitle>
            </CardHeader>
            <CardContent>
              {clients.length === 0 ? (
                <p className="text-sm text-muted-foreground">Nenhum cliente cadastrado.</p>
              ) : (
                <div className="space-y-3">
                  {clients.map((client) => (
                    <div key={client.cnpj} className="rounded-md border border-border p-3">
                      {editingClient?.cnpj === client.cnpj ? (
                        <form className="space-y-2" onSubmit={handleUpdateClient}>
                          <Input
                            value={editClientForm.name}
                            onChange={(e) => setEditClientForm((p) => ({ ...p, name: e.target.value }))}
                            placeholder="Nome"
                            required
                          />
                          <div className="grid gap-2 grid-cols-2">
                            <Input
                              value={editClientForm.contactPhone}
                              onChange={(e) => setEditClientForm((p) => ({ ...p, contactPhone: e.target.value }))}
                              placeholder="Telefone"
                            />
                            <Input
                              type="email"
                              value={editClientForm.email}
                              onChange={(e) => setEditClientForm((p) => ({ ...p, email: e.target.value }))}
                              placeholder="E-mail"
                            />
                          </div>
                          <div className="flex gap-2">
                            <Button type="submit" size="sm">Salvar</Button>
                            <Button type="button" size="sm" variant="ghost" onClick={() => setEditingClient(null)}>
                              <X className="h-4 w-4" />
                            </Button>
                          </div>
                        </form>
                      ) : (
                        <div className="flex items-center justify-between">
                          <div>
                            <p className="font-medium text-foreground">{client.name}</p>
                            <p className="text-xs text-muted-foreground">
                              CNPJ: {client.cnpj}
                              {client.contactPhone ? ` | Tel: ${client.contactPhone}` : ""}
                              {client.email ? ` | ${client.email}` : ""}
                            </p>
                          </div>
                          <div className="flex gap-1">
                            <Button size="sm" variant="ghost" onClick={() => startEditClient(client)} title="Editar">
                              <Pencil className="h-4 w-4" />
                            </Button>
                            <Button size="sm" variant="ghost" className="text-red-500 hover:text-red-700" onClick={() => handleDeleteClient(client.cnpj)} title="Excluir">
                              <Trash2 className="h-4 w-4" />
                            </Button>
                          </div>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        <div className="mb-8">
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Cadastrar Parque</CardTitle>
            </CardHeader>
            <CardContent>
              <form className="grid gap-4 sm:grid-cols-5 items-end" onSubmit={handleCreatePark}>
                <div className="space-y-2">
                  <Label htmlFor="park-name">Nome do parque</Label>
                  <Input
                    id="park-name"
                    value={parkForm.name}
                    onChange={(event) =>
                      setParkForm((prev) => ({ ...prev, name: event.target.value }))
                    }
                    placeholder="Parque Trampolim ABC"
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="park-city">Cidade</Label>
                  <Input
                    id="park-city"
                    value={parkForm.city}
                    onChange={(event) =>
                      setParkForm((prev) => ({ ...prev, city: event.target.value }))
                    }
                    placeholder="Cidade"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="park-state">UF</Label>
                  <Input
                    id="park-state"
                    maxLength={2}
                    value={parkForm.state}
                    onChange={(event) =>
                      setParkForm((prev) => ({ ...prev, state: event.target.value.toUpperCase() }))
                    }
                    placeholder="SP"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="park-client">Cliente</Label>
                  <select
                    id="park-client"
                    className="h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    value={parkForm.clientCnpj}
                    onChange={(event) =>
                      setParkForm((prev) => ({ ...prev, clientCnpj: event.target.value }))
                    }
                    required
                  >
                    <option value="">Selecione um cliente</option>
                    {clients.map((client) => (
                      <option key={client.cnpj} value={client.cnpj}>
                        {client.name} ({client.cnpj})
                      </option>
                    ))}
                  </select>
                </div>
                <Button type="submit" className="gap-2">
                  <Plus className="h-4 w-4" />
                  Cadastrar
                </Button>
              </form>
            </CardContent>
          </Card>
        </div>

        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Parques Cadastrados</CardTitle>
          </CardHeader>
          <CardContent>
            {loading ? (
              <p className="text-sm text-muted-foreground">Carregando parques...</p>
            ) : parks.length === 0 ? (
              <p className="text-sm text-muted-foreground">Nenhum parque cadastrado.</p>
            ) : (
              <div className="space-y-4">
                {parks.map((park) => {
                  const overview = overviews[park.id]
                  return (
                    <div
                      key={park.id}
                      className="rounded-lg border border-border p-4"
                    >
                      {editingPark?.id === park.id ? (
                        <form className="space-y-3" onSubmit={handleUpdatePark}>
                          <div className="grid gap-3 sm:grid-cols-4">
                            <Input
                              value={editParkForm.name}
                              onChange={(e) => setEditParkForm((p) => ({ ...p, name: e.target.value }))}
                              placeholder="Nome"
                              required
                            />
                            <Input
                              value={editParkForm.city}
                              onChange={(e) => setEditParkForm((p) => ({ ...p, city: e.target.value }))}
                              placeholder="Cidade"
                            />
                            <Input
                              maxLength={2}
                              value={editParkForm.state}
                              onChange={(e) => setEditParkForm((p) => ({ ...p, state: e.target.value.toUpperCase() }))}
                              placeholder="UF"
                            />
                            <select
                              className="h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                              value={editParkForm.clientCnpj}
                              onChange={(e) => setEditParkForm((p) => ({ ...p, clientCnpj: e.target.value }))}
                            >
                              {clients.map((c) => (
                                <option key={c.cnpj} value={c.cnpj}>{c.name}</option>
                              ))}
                            </select>
                          </div>
                          <div className="flex gap-2">
                            <Button type="submit" size="sm">Salvar</Button>
                            <Button type="button" size="sm" variant="ghost" onClick={() => setEditingPark(null)}>
                              <X className="h-4 w-4" />
                            </Button>
                          </div>
                        </form>
                      ) : (
                        <>
                          <div className="mb-2 flex items-center justify-between">
                            <div className="flex items-center gap-2">
                              <Building2 className="h-4 w-4 text-muted-foreground" />
                              <p className="font-semibold text-foreground">
                                {park.name} {park.city ? `- ${park.city}/${park.state || ""}` : ""}
                              </p>
                            </div>
                            <div className="flex gap-1">
                              <Button size="sm" variant="ghost" onClick={() => startEditPark(park)} title="Editar">
                                <Pencil className="h-4 w-4" />
                              </Button>
                              <Button size="sm" variant="ghost" className="text-red-500 hover:text-red-700" onClick={() => handleDeletePark(park.id)} title="Excluir">
                                <Trash2 className="h-4 w-4" />
                              </Button>
                            </div>
                          </div>
                          <p className="text-sm text-muted-foreground">
                            Cliente: {park.client?.name || "Nao informado"}
                          </p>
                          <div className="mt-3 grid gap-3 sm:grid-cols-4">
                            <div>
                              <p className="text-xs uppercase text-muted-foreground">Entrada total</p>
                              <p className="font-semibold text-green-600">
                                {formatCurrency(overview?.totalInflow || 0)}
                              </p>
                            </div>
                            <div>
                              <p className="text-xs uppercase text-muted-foreground">Saida total</p>
                              <p className="font-semibold text-red-500">
                                {formatCurrency(overview?.totalOutflow || 0)}
                              </p>
                            </div>
                            <div>
                              <p className="text-xs uppercase text-muted-foreground">Saldo</p>
                              <p className="font-semibold text-foreground">
                                {formatCurrency(overview?.totalBalance || 0)}
                              </p>
                            </div>
                            <div>
                              <p className="text-xs uppercase text-muted-foreground">Competencias</p>
                              <p className="font-semibold text-foreground">
                                {overview?.totalPeriods || 0}
                              </p>
                            </div>
                          </div>
                        </>
                      )}
                    </div>
                  )
                })}
              </div>
            )}
          </CardContent>
        </Card>
      </main>
    </div>
  )
}
