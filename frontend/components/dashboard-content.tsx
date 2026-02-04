"use client"

import {
  ArrowDownRight,
  ArrowUpRight,
  DollarSign,
  FileText,
  LogOut,
  TrendingUp,
  Users,
  Zap,
} from "lucide-react"
import Link from "next/link"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"

const stats = [
  {
    title: "Receita Total",
    value: "R$ 485.320",
    change: "+12.5%",
    trend: "up",
    icon: DollarSign,
  },
  {
    title: "Projetos Ativos",
    value: "8",
    change: "+2",
    trend: "up",
    icon: FileText,
  },
  {
    title: "Clientes",
    value: "24",
    change: "+4",
    trend: "up",
    icon: Users,
  },
  {
    title: "Lucro Mensal",
    value: "R$ 72.450",
    change: "-3.2%",
    trend: "down",
    icon: TrendingUp,
  },
]

const recentTransactions = [
  {
    id: 1,
    description: "Trampolim Park - São Paulo",
    type: "entrada",
    value: "R$ 125.000",
    date: "04 Fev 2026",
  },
  {
    id: 2,
    description: "Materiais - Fornecedor ABC",
    type: "saida",
    value: "R$ 32.500",
    date: "03 Fev 2026",
  },
  {
    id: 3,
    description: "Trampolim Park - Curitiba",
    type: "entrada",
    value: "R$ 89.000",
    date: "01 Fev 2026",
  },
  {
    id: 4,
    description: "Folha de Pagamento",
    type: "saida",
    value: "R$ 45.200",
    date: "31 Jan 2026",
  },
  {
    id: 5,
    description: "Manutenção - Cliente RJ",
    type: "entrada",
    value: "R$ 15.800",
    date: "30 Jan 2026",
  },
]

export function DashboardContent() {
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
          <Link href="/">
            <Button variant="ghost" size="sm" className="gap-2 text-muted-foreground">
              <LogOut className="h-4 w-4" />
              Sair
            </Button>
          </Link>
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
            Visão geral das finanças da JVA Montagens
          </p>
        </div>

        {/* Stats Grid */}
        <div className="mb-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
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
                  <span className="text-muted-foreground">vs mês anterior</span>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>

        {/* Recent Transactions */}
        <Card className="bg-card">
          <CardHeader>
            <CardTitle className="text-lg font-semibold text-foreground">
              Transações Recentes
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {recentTransactions.map((transaction) => (
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
                        {transaction.date}
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
                    {transaction.value}
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
