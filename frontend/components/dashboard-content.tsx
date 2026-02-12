"use client"

import { ComponentType, useEffect, useMemo, useState } from "react"
import {
  ArrowDownRight,
  ArrowUpRight,
  DollarSign,
  FileText,
  LogOut,
  RefreshCcw,
  TrendingUp,
  Users,
  Zap,
} from "lucide-react"
import Link from "next/link"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { ApiError, getClients, getParkOverview, getParks, type ParkFinancialOverview, type ParkPeriodSummary } from "@/lib/api"
import { useAuthGuard } from "@/hooks/use-auth-guard"

type Trend = "up" | "down"
type TransactionType = "entrada" | "saida"

type StatItem = {
  title: string
  value: string
  change: string
  trend: Trend
  icon: ComponentType<{ className?: string }>
}

type TransactionItem = {
  id: string
  description: string
  type: TransactionType
  value: number
  date: Date
}

type MonthlyProfitItem = {
  year: number
  month: number
  label: string
  inflow: number
  outflow: number
  profit: number
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
    maximumFractionDigits: 0,
  }).format(value)
}

function formatDate(value: Date) {
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  }).format(value)
}

export function DashboardContent() {
  const { token, user, isCheckingAuth, logout } = useAuthGuard()
  const [stats, setStats] = useState<StatItem[]>([])
  const [transactions, setTransactions] = useState<TransactionItem[]>([])
  const [monthlyProfits, setMonthlyProfits] = useState<MonthlyProfitItem[]>([])
  const [isLoadingData, setIsLoadingData] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null)

  const loadDashboard = useMemo(
    () => async () => {
      if (!token) return

      try {
        setIsLoadingData(true)
        setLoadError(null)

        const [parks, clients] = await Promise.all([getParks(token), getClients(token)])
        const overviews = await Promise.all(
          parks.map((park) => getParkOverview(token, park.id))
        )

        const totalInflow = sumBy(overviews, (overview) => overview.totalInflow)
        const totalOutflow = sumBy(overviews, (overview) => overview.totalOutflow)
        const totalBalance = sumBy(overviews, (overview) => overview.totalBalance)
        const totalOpenProjects = overviews.filter((overview) =>
          overview.periods.some((period) => period.status === "OPEN")
        ).length

        const monthlyBreakdown = buildMonthlyProfits(overviews)
        setMonthlyProfits(monthlyBreakdown)

        const monthlyMetrics = calculateMonthlyMetrics(overviews)
        const monthlyProfit = monthlyMetrics.currentMonthProfit
        const monthlyTrend: Trend = monthlyMetrics.change >= 0 ? "up" : "down"
        const monthlyChangeLabel =
          monthlyMetrics.previousMonthProfit === 0
            ? "sem base no mes anterior"
            : `${monthlyMetrics.change >= 0 ? "+" : ""}${monthlyMetrics.change.toFixed(1)}% vs mes anterior`

        setStats([
          {
            title: "Receita Total",
            value: formatCurrency(totalInflow),
            change: `${formatCurrency(totalOutflow)} em saidas`,
            trend: "up",
            icon: DollarSign,
          },
          {
            title: "Projetos Ativos",
            value: String(totalOpenProjects),
            change: `${parks.length} parques cadastrados`,
            trend: "up",
            icon: FileText,
          },
          {
            title: "Clientes",
            value: String(clients.length),
            change: `${clients.filter((client) => client.email).length} com e-mail`,
            trend: "up",
            icon: Users,
          },
          {
            title: "Lucro Total",
            value: formatCurrency(totalBalance),
            change: `${overviews.flatMap((o) => o.periods).length} competencias`,
            trend: totalBalance >= 0 ? "up" : "down",
            icon: TrendingUp,
          },
          {
            title: "Lucro Mensal",
            value: formatCurrency(monthlyProfit),
            change: monthlyChangeLabel,
            trend: monthlyTrend,
            icon: TrendingUp,
          },
        ])

        setTransactions(buildRecentTransactions(overviews))
        setLastUpdate(new Date())
      } catch (error) {
        if (error instanceof ApiError) {
          setLoadError(error.message)
        } else {
          setLoadError("Nao foi possivel carregar os dados do dashboard.")
        }
      } finally {
        setIsLoadingData(false)
      }
    },
    [token]
  )

  useEffect(() => {
    if (!token) return
    loadDashboard()
  }, [token, loadDashboard])

  if (isCheckingAuth) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background text-muted-foreground">
        Validando sessao...
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
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
            <Link href="/financial">
              <Button variant="ghost" size="sm">
                Financeiro
              </Button>
            </Link>
            <Button
              variant="ghost"
              size="sm"
              className="gap-2 text-muted-foreground"
              onClick={loadDashboard}
            >
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

      {/* Main Content */}
      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        {/* Welcome */}
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-foreground">
            Dashboard Financeiro
          </h1>
          <p className="mt-1 text-muted-foreground">
            Visao geral das financas da JVA Montagens
          </p>
          <p className="mt-1 text-sm text-muted-foreground">
            Admin: {user?.employeeName || user?.fullName}
            {lastUpdate ? ` | Atualizado em ${formatDate(lastUpdate)}` : ""}
          </p>
        </div>

        {loadError && (
          <Card className="mb-6 border-red-200">
            <CardContent className="py-4 text-sm font-medium text-red-600">
              {loadError}
            </CardContent>
          </Card>
        )}

        {isLoadingData ? (
          <Card className="mb-6">
            <CardContent className="py-8 text-center text-muted-foreground">
              Carregando indicadores financeiros...
            </CardContent>
          </Card>
        ) : null}

        {/* Stats Grid */}
        <div className="mb-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
          {stats.map((stat) => (
            <Card key={stat.title} className="bg-card">
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">
                  {stat.title}
                </CardTitle>
                <stat.icon className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-foreground">
                  {stat.value}
                </div>
                <div className="mt-1 flex items-center gap-1 text-sm">
                  {stat.trend === "up" ? (
                    <ArrowUpRight className="h-4 w-4 text-green-600" />
                  ) : (
                    <ArrowDownRight className="h-4 w-4 text-red-500" />
                  )}
                  <span
                    className={
                      stat.trend === "up" ? "text-green-600" : "text-red-500"
                    }
                  >
                    {stat.change}
                  </span>
                  <span className="text-muted-foreground" />
                </div>
              </CardContent>
            </Card>
          ))}
        </div>

        {/* Lucro por Mes */}
        {monthlyProfits.length > 0 && (
          <Card className="mb-8 bg-card">
            <CardHeader>
              <CardTitle className="text-lg font-semibold text-foreground">
                Lucro por Mes
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-border text-left text-muted-foreground">
                      <th className="pb-3 font-medium">Competencia</th>
                      <th className="pb-3 font-medium text-right">Receita</th>
                      <th className="pb-3 font-medium text-right">Custos</th>
                      <th className="pb-3 font-medium text-right">Lucro Liquido</th>
                    </tr>
                  </thead>
                  <tbody>
                    {monthlyProfits.map((item) => (
                      <tr key={item.label} className="border-b border-border last:border-0">
                        <td className="py-3 font-medium text-foreground">{item.label}</td>
                        <td className="py-3 text-right text-green-600">{formatCurrency(item.inflow)}</td>
                        <td className="py-3 text-right text-red-500">{formatCurrency(item.outflow)}</td>
                        <td className={`py-3 text-right font-semibold ${item.profit >= 0 ? "text-foreground" : "text-red-500"}`}>
                          {formatCurrency(item.profit)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                  <tfoot>
                    <tr className="border-t-2 border-border">
                      <td className="pt-3 font-semibold text-foreground">Total</td>
                      <td className="pt-3 text-right font-semibold text-green-600">
                        {formatCurrency(monthlyProfits.reduce((s, i) => s + i.inflow, 0))}
                      </td>
                      <td className="pt-3 text-right font-semibold text-red-500">
                        {formatCurrency(monthlyProfits.reduce((s, i) => s + i.outflow, 0))}
                      </td>
                      <td className={`pt-3 text-right font-bold ${monthlyProfits.reduce((s, i) => s + i.profit, 0) >= 0 ? "text-foreground" : "text-red-500"}`}>
                        {formatCurrency(monthlyProfits.reduce((s, i) => s + i.profit, 0))}
                      </td>
                    </tr>
                  </tfoot>
                </table>
              </div>
            </CardContent>
          </Card>
        )}

        {/* Recent Transactions */}
        <Card className="bg-card">
          <CardHeader>
            <CardTitle className="text-lg font-semibold text-foreground">
              Transacoes Recentes
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {transactions.length === 0 && (
                <div className="text-sm text-muted-foreground">
                  Nenhuma transacao encontrada para os parques cadastrados.
                </div>
              )}

              {transactions.map((transaction) => (
                <div
                  key={transaction.id}
                  className="flex items-center justify-between border-b border-border pb-4 last:border-0 last:pb-0"
                >
                  <div className="flex items-center gap-4">
                    <div
                      className={`flex h-10 w-10 items-center justify-center rounded-full ${
                        transaction.type === "entrada"
                          ? "bg-green-100 text-green-600"
                          : "bg-red-100 text-red-500"
                      }`}
                    >
                      {transaction.type === "entrada" ? (
                        <ArrowUpRight className="h-5 w-5" />
                      ) : (
                        <ArrowDownRight className="h-5 w-5" />
                      )}
                    </div>
                    <div>
                      <p className="font-medium text-foreground">
                        {transaction.description}
                      </p>
                      <p className="text-sm text-muted-foreground">
                        {formatDate(transaction.date)}
                      </p>
                    </div>
                  </div>
                  <span
                    className={`font-semibold ${
                      transaction.type === "entrada"
                        ? "text-green-600"
                        : "text-red-500"
                    }`}
                  >
                    {transaction.type === "entrada" ? "+" : "-"}
                    {formatCurrency(transaction.value)}
                  </span>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </main>
    </div>
  )
}

function sumBy<T>(items: T[], selector: (item: T) => number): number {
  return items.reduce((total, item) => total + selector(item), 0)
}

function calculateMonthlyMetrics(overviews: ParkFinancialOverview[]) {
  const now = new Date()
  const currentYear = now.getFullYear()
  const currentMonth = now.getMonth() + 1

  const previousDate = new Date(currentYear, currentMonth - 2, 1)
  const previousYear = previousDate.getFullYear()
  const previousMonth = previousDate.getMonth() + 1

  const allPeriods = overviews.flatMap((overview) => overview.periods)

  const currentMonthProfit = allPeriods
    .filter((period) => period.year === currentYear && period.month === currentMonth)
    .reduce((total, period) => total + period.balance, 0)

  const previousMonthProfit = allPeriods
    .filter((period) => period.year === previousYear && period.month === previousMonth)
    .reduce((total, period) => total + period.balance, 0)

  const change =
    previousMonthProfit === 0
      ? 0
      : ((currentMonthProfit - previousMonthProfit) / Math.abs(previousMonthProfit)) * 100

  return {
    currentMonthProfit,
    previousMonthProfit,
    change,
  }
}

const MONTH_NAMES = [
  "Janeiro", "Fevereiro", "Marco", "Abril", "Maio", "Junho",
  "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro",
]

function buildMonthlyProfits(overviews: ParkFinancialOverview[]): MonthlyProfitItem[] {
  const map = new Map<string, { year: number; month: number; inflow: number; outflow: number }>()

  for (const overview of overviews) {
    for (const period of overview.periods) {
      const key = `${period.year}-${String(period.month).padStart(2, "0")}`
      const existing = map.get(key)
      if (existing) {
        existing.inflow += period.inflow
        existing.outflow += period.outflow
      } else {
        map.set(key, {
          year: period.year,
          month: period.month,
          inflow: period.inflow,
          outflow: period.outflow,
        })
      }
    }
  }

  return Array.from(map.values())
    .sort((a, b) => a.year - b.year || a.month - b.month)
    .map((item) => ({
      year: item.year,
      month: item.month,
      label: `${MONTH_NAMES[item.month - 1]} ${item.year}`,
      inflow: item.inflow,
      outflow: item.outflow,
      profit: item.inflow - item.outflow,
    }))
}

function buildRecentTransactions(overviews: ParkFinancialOverview[]): TransactionItem[] {
  const items: TransactionItem[] = []

  for (const overview of overviews) {
    for (const period of overview.periods) {
      const date = new Date(period.year, period.month - 1, 1)

      if (period.inflow > 0) {
        items.push({
          id: `${overview.parkId}-${period.periodId}-in`,
          description: `${overview.parkName} - Receita ${period.month}/${period.year}`,
          type: "entrada",
          value: period.inflow,
          date,
        })
      }

      if (period.outflow > 0) {
        items.push({
          id: `${overview.parkId}-${period.periodId}-out`,
          description: `${overview.parkName} - Custos ${period.month}/${period.year}`,
          type: "saida",
          value: period.outflow,
          date,
        })
      }
    }
  }

  return items
    .sort((left, right) => right.date.getTime() - left.date.getTime())
    .slice(0, 8)
}
