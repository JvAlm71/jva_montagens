# JVA Montagens

Sistema completo para gerenciamento da JVA Montagens. 

Backend: JAVA + Spring-Boot

Frontend: Javascript + NextJS

## Backend V1 Financeiro

### Requisitos
- Java 17
- Maven Wrapper (`./mvnw`)
- PostgreSQL em `localhost:5432` com banco `jva_montagens`

### Rodar backend
```bash
cd backend
./mvnw clean compile
./mvnw spring-boot:run
```

### Login de administrador
- Endpoint de login: `POST /auth/login`
- Todos os endpoints do sistema financeiro exigem token Bearer de administrador.
- Endpoint para validar sessao atual: `GET /auth/me`

Exemplo `POST /auth/login`:
```json
{
  "email": "admin@jva.com",
  "password": "123456"
}
```

Resposta:
```json
{
  "accessToken": "TOKEN_AQUI",
  "tokenType": "Bearer",
  "user": {
    "cpf": "12345678901",
    "email": "admin@jva.com",
    "fullName": "Administrador JVA",
    "employeeId": 1,
    "employeeName": "Administrador JVA",
    "role": "ADMINISTRATOR"
  }
}
```

Use o token no header:
`Authorization: Bearer TOKEN_AQUI`

### Cadastrar admin via pgAdmin
Para login funcionar, o usuario precisa existir em `users` e estar vinculado em `funcionarios` com `role = 'ADMINISTRATOR'` e `active = true`.

Exemplo SQL (senha em texto para ambiente local):
```sql
insert into users (cpf, email, password, full_name)
values ('12345678901', 'admin@jva.com', '123456', 'Administrador JVA');

insert into funcionarios (name, pix_key, role, daily_rate, price_per_meter, active, user_cpf)
values ('Administrador JVA', null, 'ADMINISTRATOR', null, null, true, '12345678901');
```

### Fluxo minimo da V1
1. Criar cliente: `POST /clientes`
2. Criar parque: `POST /parks`
3. Criar funcionarios: `POST /funcionarios`
4. Criar competencia financeira: `POST /financial/periods`
5. Lancar servicos: `POST /financial/periods/{id}/services`
6. Lancar pagamentos: `POST /financial/periods/{id}/payments`
7. Consultar resumo: `GET /financial/periods/{id}/summary`

### Endpoints principais
- `GET /financial/status`
- `GET /financial/periods`
- `GET /financial/periods/{periodId}`
- `GET /financial/periods/{periodId}/services`
- `GET /financial/periods/{periodId}/payments`
- `POST /financial/periods`
- `POST /financial/periods/{periodId}/services`
- `POST /financial/periods/{periodId}/payments`
- `GET /financial/periods/{periodId}/summary`
- `GET /financial/parks/{parkId}/overview`
- `GET /clientes`
- `GET /funcionarios`
- `GET /parks`

### Exemplo rapido de competencia
```json
{
  "parkId": 1,
  "year": 2026,
  "month": 2,
  "jvaPricePerMeter": 115.00,
  "leaderPricePerMeter": 20.00,
  "taxRate": 6.8,
  "carRentalValue": 0
}
```

`taxRate` aceita `6.8` (percentual) ou `0.068` (decimal).

`cpf` e `cnpj` sao recebidos como string e normalizados para apenas digitos.
