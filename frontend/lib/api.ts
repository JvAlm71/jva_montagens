export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL?.replace(/\/$/, "") || "http://localhost:8080"

type RequestOptions = {
  method?: "GET" | "POST" | "PUT" | "DELETE"
  token?: string
  body?: unknown
}

export class ApiError extends Error {
  status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = "ApiError"
    this.status = status
  }
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = "GET", token, body } = options

  const headers = new Headers()
  headers.set("Content-Type", "application/json")
  if (token) headers.set("Authorization", `Bearer ${token}`)

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
    cache: "no-store",
  })

  if (!response.ok) {
    throw await parseApiError(response)
  }

  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

async function parseApiError(response: Response): Promise<ApiError> {
  let message = "Erro inesperado ao processar requisicao."
  const clonedResponse = response.clone()
  try {
    const payload = (await clonedResponse.json()) as {
      message?: string
      detail?: string
      error?: string
      title?: string
    }
    message =
      payload.message ||
      payload.detail ||
      payload.error ||
      payload.title ||
      message
  } catch {
    try {
      const text = await response.text()
      if (text && text.trim()) message = text.trim()
    } catch {
      // ignore body parse failures
    }
  }
  return new ApiError(message, response.status)
}

export type AuthUser = {
  cpf: string
  email: string
  fullName: string
  employeeId: number
  employeeName: string
  role: string
}

export type LoginResponse = {
  accessToken: string
  tokenType: string
  user: AuthUser
}

export type Client = {
  cnpj: string
  name: string
  contactPhone?: string | null
  email?: string | null
}

export type Park = {
  id: number
  name: string
  city?: string | null
  state?: string | null
  client: Client
}

export type JobRole = "ADMINISTRATOR" | "LEADER" | "ASSEMBLER"

export type Employee = {
  id: number
  name: string
  pixKey?: string | null
  govEmail?: string | null
  govPassword?: string | null
  cpf?: string | null
  role: JobRole
  dailyRate?: number | null
  pricePerMeter?: number | null
  active: boolean
  user?: {
    cpf: string
    fullName: string
    email: string
  } | null
}

export type FinancialStatus = "OPEN" | "CLOSED"

export type ParkPeriodSummary = {
  periodId: number
  year: number
  month: number
  status: FinancialStatus
  inflow: number
  outflow: number
  balance: number
  marginPercent: number
  totalServices: number
  totalPayments: number
}

export type ParkFinancialOverview = {
  parkId: number
  parkName: string
  totalPeriods: number
  totalInflow: number
  totalOutflow: number
  totalBalance: number
  periods: ParkPeriodSummary[]
}

export type CarRentalPeriodTotal = {
  periodId: number
  parkId: number
  parkName: string
  year: number
  month: number
  value: number
}

export type CarRentalMonthTotal = {
  year: number
  month: number
  total: number
}

export type CarRentalYearTotal = {
  year: number
  total: number
}

export type CarRentalSummary = {
  parkId?: number | null
  parkName?: string | null
  totalAllTime: number
  currentYearTotal: number
  annualTotals: CarRentalYearTotal[]
  monthlyTotals: CarRentalMonthTotal[]
  periodTotals: CarRentalPeriodTotal[]
}

export type FinancialPeriod = {
  id: number
  year: number
  month: number
  jvaPricePerMeter: number
  leaderPricePerMeter: number
  taxRate: number
  carRentalValue: number
  status: FinancialStatus
  park: Park
}

export type FinancialSummary = {
  financialId: number
  totalServices: number
  totalPayments: number
  totalMeters: number
  grossRevenue: number
  helpersCost: number
  leaderCost: number
  leaderEarnings: {
    leaderId: number
    leaderName: string
    totalMeters: number
    rateUsed: number
    totalEarnings: number
  }[]
  taxValue: number
  carRentalValue: number
  clientPaymentsReceived: number
  clientBalancePending: number
  additionalPayments: number
  totalCost: number
  netRevenue: number
  marginPercent: number
}

export type PaymentCategory =
  | "CLIENT_PAYMENT"
  | "EMPLOYEE_HELPER"
  | "EMPLOYEE_LEADER"
  | "TAX"
  | "CAR_RENTAL"
  | "OTHER"

export type PaymentEntry = {
  id: number
  paymentDate: string
  name: string
  invoiceNumber?: string | null
  amount: number
  category: PaymentCategory
  notes?: string | null
  employee?: Employee | null
  client?: Client | null
  hasReceipt?: boolean
  receiptFileName?: string | null
  receiptContentType?: string | null
  receiptSize?: number | null
}

export type ServiceType = "ASSEMBLY" | "DISASSEMBLY" | "MAINTENANCE" | "OTHER"

export type ServiceEntry = {
  id: number
  serviceType: ServiceType
  teamType: string
  leader?: Employee | null
  meters: number
  unitPrice: number
  grossValue: number
  notes?: string | null
  startDate?: string | null
  endDate?: string | null
  days?: number | null
}

export async function loginAdmin(email: string, password: string): Promise<LoginResponse> {
  return request<LoginResponse>("/auth/login", {
    method: "POST",
    body: { email, password },
  })
}

export async function getCurrentAdmin(token: string): Promise<AuthUser> {
  return request<AuthUser>("/auth/me", { token })
}

export async function getEmployees(
  token: string,
  options?: { role?: JobRole; onlyActive?: boolean }
): Promise<Employee[]> {
  const params = new URLSearchParams()
  if (options?.role) params.set("role", options.role)
  if (options?.onlyActive !== undefined) params.set("onlyActive", String(options.onlyActive))
  const query = params.size > 0 ? `?${params.toString()}` : ""
  return request<Employee[]>(`/funcionarios${query}`, { token })
}

export async function getEmployeeById(token: string, employeeId: number): Promise<Employee> {
  return request<Employee>(`/funcionarios/${employeeId}`, { token })
}

export async function createEmployee(
  token: string,
  payload: {
    name: string
    pixKey?: string
    govEmail?: string
    govPassword?: string
    cpf?: string
    role: JobRole
    dailyRate?: number
    pricePerMeter?: number
    active?: boolean
  }
): Promise<Employee> {
  return request<Employee>("/funcionarios", {
    method: "POST",
    token,
    body: payload,
  })
}

export async function updateEmployee(
  token: string,
  employeeId: number,
  payload: {
    name?: string
    pixKey?: string
    govEmail?: string
    govPassword?: string
    cpf?: string
    role?: JobRole
    dailyRate?: number
    pricePerMeter?: number
    active?: boolean
  }
): Promise<Employee> {
  return request<Employee>(`/funcionarios/${employeeId}`, {
    method: "PUT",
    token,
    body: payload,
  })
}

export async function getClients(token: string): Promise<Client[]> {
  return request<Client[]>("/clientes", { token })
}

export async function createClient(
  token: string,
  payload: { cnpj: string; name: string; contactPhone?: string; email?: string }
): Promise<Client> {
  return request<Client>("/clientes", {
    method: "POST",
    token,
    body: payload,
  })
}

export async function updateClient(
  token: string,
  cnpj: string,
  payload: { name?: string; contactPhone?: string; email?: string }
): Promise<Client> {
  return request<Client>(`/clientes/${encodeURIComponent(cnpj)}`, {
    method: "PUT",
    token,
    body: payload,
  })
}

export async function deleteClient(token: string, cnpj: string): Promise<void> {
  return request<void>(`/clientes/${encodeURIComponent(cnpj)}`, {
    method: "DELETE",
    token,
  })
}

export async function getParks(token: string, clientCnpj?: string): Promise<Park[]> {
  const query = clientCnpj ? `?clientCnpj=${encodeURIComponent(clientCnpj)}` : ""
  return request<Park[]>(`/parks${query}`, { token })
}

export async function createPark(
  token: string,
  payload: { name: string; city?: string; state?: string; clientCnpj: string }
): Promise<Park> {
  return request<Park>("/parks", {
    method: "POST",
    token,
    body: payload,
  })
}

export async function updatePark(
  token: string,
  parkId: number,
  payload: { name?: string; city?: string; state?: string; clientCnpj?: string }
): Promise<Park> {
  return request<Park>(`/parks/${parkId}`, {
    method: "PUT",
    token,
    body: payload,
  })
}

export async function deletePark(token: string, parkId: number): Promise<void> {
  return request<void>(`/parks/${parkId}`, {
    method: "DELETE",
    token,
  })
}

export async function getParkOverview(token: string, parkId: number): Promise<ParkFinancialOverview> {
  return request<ParkFinancialOverview>(`/financial/parks/${parkId}/overview`, { token })
}

export async function getPeriods(token: string, parkId?: number): Promise<FinancialPeriod[]> {
  const query = parkId ? `?parkId=${parkId}` : ""
  return request<FinancialPeriod[]>(`/financial/periods${query}`, { token })
}

export async function getPeriod(token: string, periodId: number): Promise<FinancialPeriod> {
  return request<FinancialPeriod>(`/financial/periods/${periodId}`, { token })
}

export async function createPeriod(
  token: string,
  payload: {
    parkId: number
    year: number
    month: number
    jvaPricePerMeter: number
    leaderPricePerMeter: number
    taxRate: number
    carRentalValue: number
  }
): Promise<FinancialPeriod> {
  return request<FinancialPeriod>("/financial/periods", {
    method: "POST",
    token,
    body: payload,
  })
}

export async function updatePeriod(
  token: string,
  periodId: number,
  payload: {
    jvaPricePerMeter?: number
    leaderPricePerMeter?: number
    taxRate?: number
    carRentalValue?: number
    status?: FinancialStatus
  }
): Promise<FinancialPeriod> {
  return request<FinancialPeriod>(`/financial/periods/${periodId}`, {
    method: "PUT",
    token,
    body: payload,
  })
}

export async function deletePeriod(token: string, periodId: number): Promise<void> {
  return request<void>(`/financial/periods/${periodId}`, {
    method: "DELETE",
    token,
  })
}

export async function getPeriodSummary(token: string, periodId: number): Promise<FinancialSummary> {
  return request<FinancialSummary>(`/financial/periods/${periodId}/summary`, { token })
}

export async function getCarRentalSummary(token: string, parkId?: number): Promise<CarRentalSummary> {
  const query = parkId ? `?parkId=${parkId}` : ""
  return request<CarRentalSummary>(`/financial/car-rentals/summary${query}`, { token })
}

export async function getPeriodPayments(token: string, periodId: number): Promise<PaymentEntry[]> {
  return request<PaymentEntry[]>(`/financial/periods/${periodId}/payments`, { token })
}

export async function getPeriodServices(token: string, periodId: number): Promise<ServiceEntry[]> {
  return request<ServiceEntry[]>(`/financial/periods/${periodId}/services`, { token })
}

export async function addPayment(
  token: string,
  periodId: number,
  payload: {
    paymentDate: string
    name: string
    invoiceNumber?: string
    amount: number
    category: PaymentCategory
    notes?: string
    employeeId?: number
    clientCnpj?: string
  }
): Promise<PaymentEntry> {
  return request<PaymentEntry>(`/financial/periods/${periodId}/payments`, {
    method: "POST",
    token,
    body: payload,
  })
}

export async function uploadPaymentReceipt(
  token: string,
  paymentId: number,
  file: File
): Promise<PaymentEntry> {
  const formData = new FormData()
  formData.append("file", file)

  const response = await fetch(`${API_BASE_URL}/financial/payments/${paymentId}/receipt`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
    },
    body: formData,
    cache: "no-store",
  })

  if (!response.ok) {
    throw await parseApiError(response)
  }

  return (await response.json()) as PaymentEntry
}

export async function downloadPaymentReceipt(
  token: string,
  paymentId: number
): Promise<{ blob: Blob; fileName: string }> {
  const response = await fetch(`${API_BASE_URL}/financial/payments/${paymentId}/receipt`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`,
    },
    cache: "no-store",
  })

  if (!response.ok) {
    throw await parseApiError(response)
  }

  const blob = await response.blob()
  const contentDisposition = response.headers.get("content-disposition")
  const utf8Match = contentDisposition?.match(/filename\*=UTF-8''([^;]+)/i)
  const asciiMatch = contentDisposition?.match(/filename="?([^"]+)"?/i)
  const fileName = utf8Match?.[1]
    ? decodeURIComponent(utf8Match[1])
    : asciiMatch?.[1] || `comprovante-pagamento-${paymentId}`

  return { blob, fileName }
}

export async function updatePayment(
  token: string,
  paymentId: number,
  payload: {
    paymentDate?: string
    name?: string
    invoiceNumber?: string
    amount?: number
    category?: PaymentCategory
    notes?: string
    employeeId?: number
    clientCnpj?: string
  }
): Promise<PaymentEntry> {
  return request<PaymentEntry>(`/financial/payments/${paymentId}`, {
    method: "PUT",
    token,
    body: payload,
  })
}

export async function deletePayment(token: string, paymentId: number): Promise<void> {
  return request<void>(`/financial/payments/${paymentId}`, {
    method: "DELETE",
    token,
  })
}

export async function addService(
  token: string,
  periodId: number,
  payload: {
    serviceType: ServiceType
    teamType: string
    leaderId?: number
    meters: number
    unitPrice?: number
    grossValue?: number
    notes?: string
    startDate?: string
    endDate?: string
    days?: number
  }
): Promise<ServiceEntry> {
  return request<ServiceEntry>(`/financial/periods/${periodId}/services`, {
    method: "POST",
    token,
    body: payload,
  })
}

export async function updateService(
  token: string,
  serviceId: number,
  payload: {
    serviceType?: ServiceType
    teamType?: string
    leaderId?: number
    meters?: number
    unitPrice?: number
    grossValue?: number
    notes?: string
    startDate?: string
    endDate?: string
    days?: number
  }
): Promise<ServiceEntry> {
  return request<ServiceEntry>(`/financial/services/${serviceId}`, {
    method: "PUT",
    token,
    body: payload,
  })
}

export async function deleteService(token: string, serviceId: number): Promise<void> {
  return request<void>(`/financial/services/${serviceId}`, {
    method: "DELETE",
    token,
  })
}
