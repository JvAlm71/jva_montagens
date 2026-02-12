"use client"

import { FormEvent, useEffect, useMemo, useState } from "react"
import Link from "next/link"
import { BanknoteArrowDown, BanknoteArrowUp, LogOut, Pencil, Plus, RefreshCcw, Trash2, X, Zap } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { useAuthGuard } from "@/hooks/use-auth-guard"
import {
  addPayment,
  addService,
  ApiError,
  createPeriod,
  deletePeriod,
  deletePayment,
  deleteService,
  getPeriodPayments,
  getPeriods,
  getPeriodServices,
  getPeriodSummary,
  getParks,
  updatePeriod,
  updatePayment,
  updateService,
  type FinancialSummary,
  type FinancialStatus,
  type Park,
  type PaymentCategory,
  type PaymentEntry,
  type ServiceEntry,
  type ServiceType,
} from "@/lib/api"

function formatCurrency(value: number) {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
    maximumFractionDigits: 0,
  }).format(value)
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(new Date(value))
}

export default function FinancialPage() {
  const { token, isCheckingAuth, logout } = useAuthGuard()
  const [parks, setParks] = useState<Park[]>([])
  const [periods, setPeriods] = useState<
    {
      id: number
      year: number
      month: number
      status: "OPEN" | "CLOSED"
    }[]
  >([])
  const [summary, setSummary] = useState<FinancialSummary | null>(null)
  const [services, setServices] = useState<ServiceEntry[]>([])
  const [payments, setPayments] = useState<PaymentEntry[]>([])
  const [selectedParkId, setSelectedParkId] = useState("")
  const [selectedPeriodId, setSelectedPeriodId] = useState("")
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  const [periodForm, setPeriodForm] = useState({
    year: new Date().getFullYear(),
    month: new Date().getMonth() + 1,
    jvaPricePerMeter: 115,
    leaderPricePerMeter: 20,
    taxRate: 6.8,
    carRentalValue: 0,
  })

  const [serviceForm, setServiceForm] = useState({
    serviceType: "ASSEMBLY" as ServiceType,
    teamType: "MONTAGEM",
    meters: 0,
    unitPrice: 0,
    notes: "",
    startDate: "",
    endDate: "",
  })

  const [paymentForm, setPaymentForm] = useState({
    paymentDate: new Date().toISOString().slice(0, 10),
    name: "",
    amount: 0,
    category: "OTHER" as PaymentCategory,
    invoiceNumber: "",
    notes: "",
    clientCnpj: "",
  })

  const [editingPeriod, setEditingPeriod] = useState(false)
  const [editPeriodForm, setEditPeriodForm] = useState({
    jvaPricePerMeter: 0,
    leaderPricePerMeter: 0,
    taxRate: 0,
    carRentalValue: 0,
    status: "OPEN" as FinancialStatus,
  })

  const [editingServiceId, setEditingServiceId] = useState<number | null>(null)
  const [editServiceForm, setEditServiceForm] = useState({
    serviceType: "ASSEMBLY" as ServiceType,
    teamType: "",
    meters: 0,
    unitPrice: 0,
    notes: "",
    startDate: "",
    endDate: "",
  })

  const [editingPaymentId, setEditingPaymentId] = useState<number | null>(null)
  const [editPaymentForm, setEditPaymentForm] = useState({
    paymentDate: "",
    name: "",
    amount: 0,
    category: "OTHER" as PaymentCategory,
    invoiceNumber: "",
    notes: "",
  })

  const loadParks = useMemo(
    () => async () => {
      if (!token) return

      const fetchedParks = await getParks(token)
      setParks(fetchedParks)

      if (fetchedParks.length > 0 && !selectedParkId) {
        setSelectedParkId(String(fetchedParks[0].id))
      }
    },
    [token, selectedParkId]
  )

  const loadPeriods = useMemo(
    () => async (parkId: number) => {
      if (!token) return

      const fetched = await getPeriods(token, parkId)
      const mapped = fetched.map((period) => ({
        id: period.id,
        year: period.year,
        month: period.month,
        status: period.status,
      }))

      setPeriods(mapped)

      if (mapped.length > 0) {
        const currentlySelected = selectedPeriodId ? Number(selectedPeriodId) : null
        const exists = currentlySelected && mapped.some((item) => item.id === currentlySelected)
        if (!exists) setSelectedPeriodId(String(mapped[0].id))
      } else {
        setSelectedPeriodId("")
      }
    },
    [token, selectedPeriodId]
  )

  const loadPeriodDetails = useMemo(
    () => async (periodId: number) => {
      if (!token) return

      const [fetchedSummary, fetchedServices, fetchedPayments] = await Promise.all([
        getPeriodSummary(token, periodId),
        getPeriodServices(token, periodId),
        getPeriodPayments(token, periodId),
      ])

      setSummary(fetchedSummary)
      setServices(fetchedServices)
      setPayments(fetchedPayments)
    },
    [token]
  )

  const reloadAll = useMemo(
    () => async () => {
      if (!token) return

      try {
        setLoading(true)
        setError(null)
        setMessage(null)

        await loadParks()

        const parkId = selectedParkId ? Number(selectedParkId) : null
        if (parkId) {
          await loadPeriods(parkId)
        }

        const periodId = selectedPeriodId ? Number(selectedPeriodId) : null
        if (periodId) {
          await loadPeriodDetails(periodId)
        } else {
          setSummary(null)
          setServices([])
          setPayments([])
        }
      } catch (err) {
        if (err instanceof ApiError) {
          setError(err.message)
        } else {
          setError("Nao foi possivel carregar os dados financeiros.")
        }
      } finally {
        setLoading(false)
      }
    },
    [token, loadParks, loadPeriods, loadPeriodDetails, selectedParkId, selectedPeriodId]
  )

  useEffect(() => {
    if (!token) return
    reloadAll()
  }, [token, reloadAll])

  useEffect(() => {
    if (!token) return
    const parkId = selectedParkId ? Number(selectedParkId) : null
    if (!parkId) return

    const load = async () => {
      try {
        setError(null)
        await loadPeriods(parkId)
      } catch (err) {
        if (err instanceof ApiError) setError(err.message)
      }
    }

    load()
  }, [selectedParkId, token, loadPeriods])

  useEffect(() => {
    if (!token) return
    const periodId = selectedPeriodId ? Number(selectedPeriodId) : null
    if (!periodId) return

    const load = async () => {
      try {
        setError(null)
        await loadPeriodDetails(periodId)
      } catch (err) {
        if (err instanceof ApiError) setError(err.message)
      }
    }

    load()
  }, [selectedPeriodId, token, loadPeriodDetails])

  const handleCreatePeriod = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!token || !selectedParkId) return

    try {
      setError(null)
      setMessage(null)

      const created = await createPeriod(token, {
        parkId: Number(selectedParkId),
        year: periodForm.year,
        month: periodForm.month,
        jvaPricePerMeter: periodForm.jvaPricePerMeter,
        leaderPricePerMeter: periodForm.leaderPricePerMeter,
        taxRate: periodForm.taxRate,
        carRentalValue: periodForm.carRentalValue,
      })

      setMessage("Competencia criada com sucesso.")
      await loadPeriods(Number(selectedParkId))
      setSelectedPeriodId(String(created.id))
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError("Falha ao criar competencia.")
    }
  }

  const handleAddService = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!token || !selectedPeriodId) return

    try {
      setError(null)
      setMessage(null)

      await addService(token, Number(selectedPeriodId), {
        serviceType: serviceForm.serviceType,
        teamType: serviceForm.teamType,
        meters: serviceForm.meters,
        unitPrice: serviceForm.unitPrice || undefined,
        notes: serviceForm.notes || undefined,
        startDate: serviceForm.startDate || undefined,
        endDate: serviceForm.endDate || undefined,
      })

      setServiceForm({
        serviceType: "ASSEMBLY",
        teamType: "MONTAGEM",
        meters: 0,
        unitPrice: 0,
        notes: "",
        startDate: "",
        endDate: "",
      })
      setMessage("Servico adicionado com sucesso.")
      await loadPeriodDetails(Number(selectedPeriodId))
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError("Falha ao adicionar servico.")
    }
  }

  const handleAddPayment = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!token || !selectedPeriodId) return

    try {
      setError(null)
      setMessage(null)

      await addPayment(token, Number(selectedPeriodId), {
        paymentDate: paymentForm.paymentDate,
        name: paymentForm.name,
        amount: paymentForm.amount,
        category: paymentForm.category,
        invoiceNumber: paymentForm.invoiceNumber || undefined,
        notes: paymentForm.notes || undefined,
        clientCnpj: paymentForm.clientCnpj || undefined,
      })

      setPaymentForm((prev) => ({
        ...prev,
        name: "",
        amount: 0,
        invoiceNumber: "",
        notes: "",
      }))
      setMessage("Pagamento registrado com sucesso.")
      await loadPeriodDetails(Number(selectedPeriodId))
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError("Falha ao registrar pagamento.")
    }
  }

  const startEditPeriod = () => {
    if (!summary || !selectedPeriodId) return
    const period = periods.find((p) => p.id === Number(selectedPeriodId))
    if (!period) return
    setEditPeriodForm({
      jvaPricePerMeter: 0,
      leaderPricePerMeter: 0,
      taxRate: 0,
      carRentalValue: 0,
      status: period.status,
    })
    // Load actual period data
    const loadForEdit = async () => {
      if (!token) return
      try {
        const { default: fullPeriod } = { default: await (async () => {
          const resp = await fetch(`${process.env.NEXT_PUBLIC_API_URL?.replace(/\/$/, "") || "http://localhost:8080"}/financial/periods/${selectedPeriodId}`, {
            headers: { Authorization: `Bearer ${token}` },
          })
          return resp.json()
        })() }
        setEditPeriodForm({
          jvaPricePerMeter: fullPeriod.jvaPricePerMeter ?? 0,
          leaderPricePerMeter: fullPeriod.leaderPricePerMeter ?? 0,
          taxRate: fullPeriod.taxRate != null ? (fullPeriod.taxRate < 1 ? fullPeriod.taxRate * 100 : fullPeriod.taxRate) : 0,
          carRentalValue: fullPeriod.carRentalValue ?? 0,
          status: fullPeriod.status ?? "OPEN",
        })
      } catch { /* ignore */ }
    }
    loadForEdit()
    setEditingPeriod(true)
  }

  const handleUpdatePeriod = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!token || !selectedPeriodId) return
    try {
      setError(null)
      setMessage(null)
      await updatePeriod(token, Number(selectedPeriodId), {
        jvaPricePerMeter: editPeriodForm.jvaPricePerMeter,
        leaderPricePerMeter: editPeriodForm.leaderPricePerMeter,
        taxRate: editPeriodForm.taxRate,
        carRentalValue: editPeriodForm.carRentalValue,
        status: editPeriodForm.status,
      })
      setEditingPeriod(false)
      setMessage("Competencia atualizada com sucesso.")
      await loadPeriods(Number(selectedParkId))
      await loadPeriodDetails(Number(selectedPeriodId))
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError("Falha ao atualizar competencia.")
    }
  }

  const handleDeletePeriod = async () => {
    if (!token || !selectedPeriodId) return
    if (!confirm("Excluir esta competencia? Todos os servicos e pagamentos associados serao removidos.")) return
    try {
      setError(null)
      setMessage(null)
      await deletePeriod(token, Number(selectedPeriodId))
      setSelectedPeriodId("")
      setSummary(null)
      setServices([])
      setPayments([])
      setMessage("Competencia excluida com sucesso.")
      await loadPeriods(Number(selectedParkId))
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError("Falha ao excluir competencia.")
    }
  }

  const startEditService = (service: ServiceEntry) => {
    setEditingServiceId(service.id)
    setEditServiceForm({
      serviceType: service.serviceType,
      teamType: service.teamType,
      meters: service.meters,
      unitPrice: service.unitPrice,
      notes: service.notes || "",
      startDate: service.startDate || "",
      endDate: service.endDate || "",
    })
  }

  const handleUpdateService = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!token || !editingServiceId) return
    try {
      setError(null)
      setMessage(null)
      await updateService(token, editingServiceId, {
        serviceType: editServiceForm.serviceType,
        teamType: editServiceForm.teamType,
        meters: editServiceForm.meters,
        unitPrice: editServiceForm.unitPrice || undefined,
        notes: editServiceForm.notes || undefined,
        startDate: editServiceForm.startDate || undefined,
        endDate: editServiceForm.endDate || undefined,
      })
      setEditingServiceId(null)
      setMessage("Servico atualizado com sucesso.")
      await loadPeriodDetails(Number(selectedPeriodId))
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError("Falha ao atualizar servico.")
    }
  }

  const handleDeleteService = async (serviceId: number) => {
    if (!token) return
    if (!confirm("Excluir este servico?")) return
    try {
      setError(null)
      setMessage(null)
      await deleteService(token, serviceId)
      setMessage("Servico excluido com sucesso.")
      await loadPeriodDetails(Number(selectedPeriodId))
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError("Falha ao excluir servico.")
    }
  }

  const startEditPayment = (payment: PaymentEntry) => {
    setEditingPaymentId(payment.id)
    setEditPaymentForm({
      paymentDate: payment.paymentDate,
      name: payment.name,
      amount: payment.amount,
      category: payment.category,
      invoiceNumber: payment.invoiceNumber || "",
      notes: payment.notes || "",
    })
  }

  const handleUpdatePayment = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!token || !editingPaymentId) return
    try {
      setError(null)
      setMessage(null)
      await updatePayment(token, editingPaymentId, {
        paymentDate: editPaymentForm.paymentDate,
        name: editPaymentForm.name,
        amount: editPaymentForm.amount,
        category: editPaymentForm.category,
        invoiceNumber: editPaymentForm.invoiceNumber || undefined,
        notes: editPaymentForm.notes || undefined,
      })
      setEditingPaymentId(null)
      setMessage("Pagamento atualizado com sucesso.")
      await loadPeriodDetails(Number(selectedPeriodId))
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError("Falha ao atualizar pagamento.")
    }
  }

  const handleDeletePayment = async (paymentId: number) => {
    if (!token) return
    if (!confirm("Excluir este pagamento?")) return
    try {
      setError(null)
      setMessage(null)
      await deletePayment(token, paymentId)
      setMessage("Pagamento excluido com sucesso.")
      await loadPeriodDetails(Number(selectedPeriodId))
    } catch (err) {
      if (err instanceof ApiError) setError(err.message)
      else setError("Falha ao excluir pagamento.")
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
            <Link href="/parks">
              <Button variant="ghost" size="sm">
                Parques
              </Button>
            </Link>
            <Button variant="ghost" size="sm" className="gap-2" onClick={reloadAll}>
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
          <h1 className="text-2xl font-bold text-foreground">Operacao Financeira</h1>
          <p className="mt-1 text-muted-foreground">
            Gerencie competencias, servicos de montagem e pagamentos.
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

        <div className="mb-6 grid gap-4 md:grid-cols-2">
          <div className="space-y-2">
            <Label htmlFor="park-select">Parque</Label>
            <select
              id="park-select"
              className="h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
              value={selectedParkId}
              onChange={(event) => setSelectedParkId(event.target.value)}
            >
              <option value="">Selecione um parque</option>
              {parks.map((park) => (
                <option key={park.id} value={park.id}>
                  {park.name}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-2">
            <Label htmlFor="period-select">Competencia</Label>
            <select
              id="period-select"
              className="h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
              value={selectedPeriodId}
              onChange={(event) => setSelectedPeriodId(event.target.value)}
            >
              <option value="">Selecione uma competencia</option>
              {periods.map((period) => (
                <option key={period.id} value={period.id}>
                  {String(period.month).padStart(2, "0")}/{period.year} - {period.status}
                </option>
              ))}
            </select>
          </div>
        </div>

        {summary && (
          <div className="mb-8">
            {editingPeriod ? (
              <Card className="mb-4">
                <CardHeader>
                  <CardTitle className="text-base">Editar Competencia</CardTitle>
                </CardHeader>
                <CardContent>
                  <form className="grid gap-3 sm:grid-cols-6 items-end" onSubmit={handleUpdatePeriod}>
                    <div className="space-y-1">
                      <Label>Valor JVA (m2)</Label>
                      <Input type="number" step="0.01" value={editPeriodForm.jvaPricePerMeter} onChange={(e) => setEditPeriodForm((p) => ({ ...p, jvaPricePerMeter: Number(e.target.value) }))} />
                    </div>
                    <div className="space-y-1">
                      <Label>Valor Lider (m2)</Label>
                      <Input type="number" step="0.01" value={editPeriodForm.leaderPricePerMeter} onChange={(e) => setEditPeriodForm((p) => ({ ...p, leaderPricePerMeter: Number(e.target.value) }))} />
                    </div>
                    <div className="space-y-1">
                      <Label>Imposto (%)</Label>
                      <Input type="number" step="0.01" value={editPeriodForm.taxRate} onChange={(e) => setEditPeriodForm((p) => ({ ...p, taxRate: Number(e.target.value) }))} />
                    </div>
                    <div className="space-y-1">
                      <Label>Aluguel carro</Label>
                      <Input type="number" step="0.01" value={editPeriodForm.carRentalValue} onChange={(e) => setEditPeriodForm((p) => ({ ...p, carRentalValue: Number(e.target.value) }))} />
                    </div>
                    <div className="space-y-1">
                      <Label>Status</Label>
                      <select className="h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm" value={editPeriodForm.status} onChange={(e) => setEditPeriodForm((p) => ({ ...p, status: e.target.value as FinancialStatus }))}>
                        <option value="OPEN">ABERTA</option>
                        <option value="CLOSED">FECHADA</option>
                      </select>
                    </div>
                    <div className="flex gap-2">
                      <Button type="submit" size="sm">Salvar</Button>
                      <Button type="button" size="sm" variant="ghost" onClick={() => setEditingPeriod(false)}>
                        <X className="h-4 w-4" />
                      </Button>
                    </div>
                  </form>
                </CardContent>
              </Card>
            ) : (
              <div className="mb-4 flex gap-2 justify-end">
                <Button size="sm" variant="outline" className="gap-2" onClick={startEditPeriod}>
                  <Pencil className="h-4 w-4" />
                  Editar Competencia
                </Button>
                <Button size="sm" variant="outline" className="gap-2 text-red-500 hover:text-red-700" onClick={handleDeletePeriod}>
                  <Trash2 className="h-4 w-4" />
                  Excluir Competencia
                </Button>
              </div>
            )}
            <div className="grid gap-4 md:grid-cols-3">
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm text-muted-foreground">Receita Bruta</CardTitle>
              </CardHeader>
              <CardContent className="text-2xl font-bold text-green-600">
                {formatCurrency(summary.grossRevenue)}
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm text-muted-foreground">Custo Total</CardTitle>
              </CardHeader>
              <CardContent className="text-2xl font-bold text-red-500">
                {formatCurrency(summary.totalCost)}
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm text-muted-foreground">Lucro Liquido</CardTitle>
              </CardHeader>
              <CardContent className="text-2xl font-bold text-foreground">
                {formatCurrency(summary.netRevenue)}
              </CardContent>
            </Card>
            </div>
          </div>
        )}

        <div className="mb-8 grid gap-6 xl:grid-cols-3">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Nova Competencia</CardTitle>
            </CardHeader>
            <CardContent>
              <form className="space-y-3" onSubmit={handleCreatePeriod}>
                <div className="grid gap-3 grid-cols-2">
                  <div className="space-y-1">
                    <Label>Ano</Label>
                    <Input
                      type="number"
                      value={periodForm.year}
                      onChange={(event) =>
                        setPeriodForm((prev) => ({ ...prev, year: Number(event.target.value) }))
                      }
                      required
                    />
                  </div>
                  <div className="space-y-1">
                    <Label>Mes</Label>
                    <Input
                      type="number"
                      min={1}
                      max={12}
                      value={periodForm.month}
                      onChange={(event) =>
                        setPeriodForm((prev) => ({ ...prev, month: Number(event.target.value) }))
                      }
                      required
                    />
                  </div>
                </div>
                <div className="space-y-1">
                  <Label>Valor JVA (m2)</Label>
                  <Input
                    type="number"
                    step="0.01"
                    value={periodForm.jvaPricePerMeter}
                    onChange={(event) =>
                      setPeriodForm((prev) => ({
                        ...prev,
                        jvaPricePerMeter: Number(event.target.value),
                      }))
                    }
                    required
                  />
                </div>
                <div className="space-y-1">
                  <Label>Valor Lider (m2)</Label>
                  <Input
                    type="number"
                    step="0.01"
                    value={periodForm.leaderPricePerMeter}
                    onChange={(event) =>
                      setPeriodForm((prev) => ({
                        ...prev,
                        leaderPricePerMeter: Number(event.target.value),
                      }))
                    }
                    required
                  />
                </div>
                <div className="space-y-1">
                  <Label>Imposto (%)</Label>
                  <Input
                    type="number"
                    step="0.01"
                    value={periodForm.taxRate}
                    onChange={(event) =>
                      setPeriodForm((prev) => ({
                        ...prev,
                        taxRate: Number(event.target.value),
                      }))
                    }
                    required
                  />
                </div>
                <div className="space-y-1">
                  <Label>Aluguel carro</Label>
                  <Input
                    type="number"
                    step="0.01"
                    value={periodForm.carRentalValue}
                    onChange={(event) =>
                      setPeriodForm((prev) => ({
                        ...prev,
                        carRentalValue: Number(event.target.value),
                      }))
                    }
                  />
                </div>
                <Button type="submit" className="w-full gap-2" disabled={!selectedParkId}>
                  <Plus className="h-4 w-4" />
                  Criar Competencia
                </Button>
              </form>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Registrar Servico</CardTitle>
            </CardHeader>
            <CardContent>
              <form className="space-y-3" onSubmit={handleAddService}>
                <div className="space-y-1">
                  <Label>Tipo de servico</Label>
                  <select
                    className="h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    value={serviceForm.serviceType}
                    onChange={(event) =>
                      setServiceForm((prev) => ({
                        ...prev,
                        serviceType: event.target.value as ServiceType,
                      }))
                    }
                  >
                    <option value="ASSEMBLY">MONTAGEM</option>
                    <option value="DISASSEMBLY">DESMONTAGEM</option>
                    <option value="MAINTENANCE">MANUTENCAO</option>
                    <option value="OTHER">OUTRO</option>
                  </select>
                </div>
                <div className="space-y-1">
                  <Label>Equipe</Label>
                  <Input
                    value={serviceForm.teamType}
                    onChange={(event) =>
                      setServiceForm((prev) => ({ ...prev, teamType: event.target.value }))
                    }
                    required
                  />
                </div>
                <div className="grid gap-3 grid-cols-2">
                  <div className="space-y-1">
                    <Label>Metros</Label>
                    <Input
                      type="number"
                      step="0.01"
                      value={serviceForm.meters}
                      onChange={(event) =>
                        setServiceForm((prev) => ({ ...prev, meters: Number(event.target.value) }))
                      }
                      required
                    />
                  </div>
                  <div className="space-y-1">
                    <Label>Valor unitario</Label>
                    <Input
                      type="number"
                      step="0.01"
                      value={serviceForm.unitPrice}
                      onChange={(event) =>
                        setServiceForm((prev) => ({
                          ...prev,
                          unitPrice: Number(event.target.value),
                        }))
                      }
                    />
                  </div>
                </div>
                <div className="grid gap-3 grid-cols-2">
                  <div className="space-y-1">
                    <Label>Data inicio</Label>
                    <Input
                      type="date"
                      value={serviceForm.startDate}
                      onChange={(event) =>
                        setServiceForm((prev) => ({ ...prev, startDate: event.target.value }))
                      }
                    />
                  </div>
                  <div className="space-y-1">
                    <Label>Data fim</Label>
                    <Input
                      type="date"
                      value={serviceForm.endDate}
                      onChange={(event) =>
                        setServiceForm((prev) => ({ ...prev, endDate: event.target.value }))
                      }
                    />
                  </div>
                </div>
                <div className="space-y-1">
                  <Label>Observacao</Label>
                  <Input
                    value={serviceForm.notes}
                    onChange={(event) =>
                      setServiceForm((prev) => ({ ...prev, notes: event.target.value }))
                    }
                  />
                </div>
                <Button type="submit" className="w-full gap-2" disabled={!selectedPeriodId}>
                  <BanknoteArrowUp className="h-4 w-4" />
                  Adicionar Servico
                </Button>
              </form>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Registrar Pagamento</CardTitle>
            </CardHeader>
            <CardContent>
              <form className="space-y-3" onSubmit={handleAddPayment}>
                <div className="space-y-1">
                  <Label>Data</Label>
                  <Input
                    type="date"
                    value={paymentForm.paymentDate}
                    onChange={(event) =>
                      setPaymentForm((prev) => ({ ...prev, paymentDate: event.target.value }))
                    }
                    required
                  />
                </div>
                <div className="space-y-1">
                  <Label>Nome</Label>
                  <Input
                    value={paymentForm.name}
                    onChange={(event) =>
                      setPaymentForm((prev) => ({ ...prev, name: event.target.value }))
                    }
                    required
                  />
                </div>
                <div className="grid gap-3 grid-cols-2">
                  <div className="space-y-1">
                    <Label>Valor</Label>
                    <Input
                      type="number"
                      step="0.01"
                      value={paymentForm.amount}
                      onChange={(event) =>
                        setPaymentForm((prev) => ({ ...prev, amount: Number(event.target.value) }))
                      }
                      required
                    />
                  </div>
                  <div className="space-y-1">
                    <Label>Categoria</Label>
                    <select
                      className="h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                      value={paymentForm.category}
                      onChange={(event) =>
                        setPaymentForm((prev) => ({
                          ...prev,
                          category: event.target.value as PaymentCategory,
                        }))
                      }
                    >
                      <option value="OTHER">OUTRO</option>
                      <option value="EMPLOYEE_HELPER">AJUDANTE</option>
                      <option value="EMPLOYEE_LEADER">LIDER</option>
                      <option value="TAX">IMPOSTO</option>
                      <option value="CAR_RENTAL">ALUGUEL_CARRO</option>
                    </select>
                  </div>
                </div>
                <div className="space-y-1">
                  <Label>Numero da nota</Label>
                  <Input
                    value={paymentForm.invoiceNumber}
                    onChange={(event) =>
                      setPaymentForm((prev) => ({ ...prev, invoiceNumber: event.target.value }))
                    }
                  />
                </div>
                <div className="space-y-1">
                  <Label>CNPJ do cliente (opcional)</Label>
                  <Input
                    value={paymentForm.clientCnpj}
                    onChange={(event) =>
                      setPaymentForm((prev) => ({ ...prev, clientCnpj: event.target.value }))
                    }
                  />
                </div>
                <div className="space-y-1">
                  <Label>Observacao</Label>
                  <Input
                    value={paymentForm.notes}
                    onChange={(event) =>
                      setPaymentForm((prev) => ({ ...prev, notes: event.target.value }))
                    }
                  />
                </div>
                <Button type="submit" className="w-full gap-2" disabled={!selectedPeriodId}>
                  <BanknoteArrowDown className="h-4 w-4" />
                  Registrar Pagamento
                </Button>
              </form>
            </CardContent>
          </Card>
        </div>

        <div className="grid gap-6 xl:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Servicos da Competencia</CardTitle>
            </CardHeader>
            <CardContent>
              {loading ? (
                <p className="text-sm text-muted-foreground">Carregando servicos...</p>
              ) : services.length === 0 ? (
                <p className="text-sm text-muted-foreground">Nenhum servico registrado.</p>
              ) : (
                <div className="space-y-3">
                  {services.map((service) => (
                    <div key={service.id} className="rounded-md border border-border p-3">
                      {editingServiceId === service.id ? (
                        <form className="space-y-2" onSubmit={handleUpdateService}>
                          <div className="grid gap-2 grid-cols-2">
                            <select className="h-9 rounded-md border border-input bg-background px-2 text-sm" value={editServiceForm.serviceType} onChange={(e) => setEditServiceForm((p) => ({ ...p, serviceType: e.target.value as ServiceType }))}>
                              <option value="ASSEMBLY">MONTAGEM</option>
                              <option value="DISASSEMBLY">DESMONTAGEM</option>
                              <option value="MAINTENANCE">MANUTENCAO</option>
                              <option value="OTHER">OUTRO</option>
                            </select>
                            <Input className="h-9" value={editServiceForm.teamType} onChange={(e) => setEditServiceForm((p) => ({ ...p, teamType: e.target.value }))} placeholder="Equipe" />
                          </div>
                          <div className="grid gap-2 grid-cols-2">
                            <Input className="h-9" type="number" step="0.01" value={editServiceForm.meters} onChange={(e) => setEditServiceForm((p) => ({ ...p, meters: Number(e.target.value) }))} placeholder="Metros" />
                            <Input className="h-9" type="number" step="0.01" value={editServiceForm.unitPrice} onChange={(e) => setEditServiceForm((p) => ({ ...p, unitPrice: Number(e.target.value) }))} placeholder="Valor un." />
                          </div>
                          <div className="grid gap-2 grid-cols-2">
                            <Input className="h-9" type="date" value={editServiceForm.startDate} onChange={(e) => setEditServiceForm((p) => ({ ...p, startDate: e.target.value }))} />
                            <Input className="h-9" type="date" value={editServiceForm.endDate} onChange={(e) => setEditServiceForm((p) => ({ ...p, endDate: e.target.value }))} />
                          </div>
                          <Input className="h-9" value={editServiceForm.notes} onChange={(e) => setEditServiceForm((p) => ({ ...p, notes: e.target.value }))} placeholder="Observacao" />
                          <div className="flex gap-2">
                            <Button type="submit" size="sm">Salvar</Button>
                            <Button type="button" size="sm" variant="ghost" onClick={() => setEditingServiceId(null)}>
                              <X className="h-4 w-4" />
                            </Button>
                          </div>
                        </form>
                      ) : (
                        <div className="flex items-start justify-between">
                          <div>
                            <p className="font-medium text-foreground">
                              {service.teamType} - {service.serviceType}
                            </p>
                            <p className="text-sm text-muted-foreground">
                              {service.meters} m2 | {formatCurrency(service.grossValue)}
                            </p>
                            {(service.startDate || service.endDate) && (
                              <p className="text-xs text-muted-foreground">
                                {service.startDate ? formatDate(service.startDate) : "--"} ate{" "}
                                {service.endDate ? formatDate(service.endDate) : "--"}
                              </p>
                            )}
                          </div>
                          <div className="flex gap-1 shrink-0">
                            <Button size="sm" variant="ghost" onClick={() => startEditService(service)} title="Editar">
                              <Pencil className="h-4 w-4" />
                            </Button>
                            <Button size="sm" variant="ghost" className="text-red-500 hover:text-red-700" onClick={() => handleDeleteService(service.id)} title="Excluir">
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

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Pagamentos da Competencia</CardTitle>
            </CardHeader>
            <CardContent>
              {loading ? (
                <p className="text-sm text-muted-foreground">Carregando pagamentos...</p>
              ) : payments.length === 0 ? (
                <p className="text-sm text-muted-foreground">Nenhum pagamento registrado.</p>
              ) : (
                <div className="space-y-3">
                  {payments.map((payment) => (
                    <div key={payment.id} className="rounded-md border border-border p-3">
                      {editingPaymentId === payment.id ? (
                        <form className="space-y-2" onSubmit={handleUpdatePayment}>
                          <div className="grid gap-2 grid-cols-2">
                            <Input className="h-9" type="date" value={editPaymentForm.paymentDate} onChange={(e) => setEditPaymentForm((p) => ({ ...p, paymentDate: e.target.value }))} required />
                            <Input className="h-9" value={editPaymentForm.name} onChange={(e) => setEditPaymentForm((p) => ({ ...p, name: e.target.value }))} placeholder="Nome" required />
                          </div>
                          <div className="grid gap-2 grid-cols-2">
                            <Input className="h-9" type="number" step="0.01" value={editPaymentForm.amount} onChange={(e) => setEditPaymentForm((p) => ({ ...p, amount: Number(e.target.value) }))} required />
                            <select className="h-9 rounded-md border border-input bg-background px-2 text-sm" value={editPaymentForm.category} onChange={(e) => setEditPaymentForm((p) => ({ ...p, category: e.target.value as PaymentCategory }))}>
                              <option value="OTHER">OUTRO</option>
                              <option value="EMPLOYEE_HELPER">AJUDANTE</option>
                              <option value="EMPLOYEE_LEADER">LIDER</option>
                              <option value="TAX">IMPOSTO</option>
                              <option value="CAR_RENTAL">ALUGUEL_CARRO</option>
                            </select>
                          </div>
                          <Input className="h-9" value={editPaymentForm.invoiceNumber} onChange={(e) => setEditPaymentForm((p) => ({ ...p, invoiceNumber: e.target.value }))} placeholder="Numero da nota" />
                          <Input className="h-9" value={editPaymentForm.notes} onChange={(e) => setEditPaymentForm((p) => ({ ...p, notes: e.target.value }))} placeholder="Observacao" />
                          <div className="flex gap-2">
                            <Button type="submit" size="sm">Salvar</Button>
                            <Button type="button" size="sm" variant="ghost" onClick={() => setEditingPaymentId(null)}>
                              <X className="h-4 w-4" />
                            </Button>
                          </div>
                        </form>
                      ) : (
                        <div className="flex items-start justify-between">
                          <div>
                            <p className="font-medium text-foreground">
                              {payment.name} - {payment.category}
                            </p>
                            <p className="text-sm text-muted-foreground">
                              {formatDate(payment.paymentDate)} | {formatCurrency(payment.amount)}
                            </p>
                            {payment.notes && (
                              <p className="text-xs text-muted-foreground">{payment.notes}</p>
                            )}
                          </div>
                          <div className="flex gap-1 shrink-0">
                            <Button size="sm" variant="ghost" onClick={() => startEditPayment(payment)} title="Editar">
                              <Pencil className="h-4 w-4" />
                            </Button>
                            <Button size="sm" variant="ghost" className="text-red-500 hover:text-red-700" onClick={() => handleDeletePayment(payment.id)} title="Excluir">
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
      </main>
    </div>
  )
}
