import { LoginForm } from "@/components/login-form"

export default function LoginPage() {
  return (
    <main className="min-h-screen flex">
      {/* Painel Esquerdo - Imagem/Branding */}
      <div className="hidden lg:flex lg:w-1/2 bg-foreground relative overflow-hidden">
        <div className="absolute inset-0 bg-[url('/grid.svg')] opacity-10" />
        <div className="relative z-10 flex flex-col justify-between p-12 text-primary-foreground">
          <div>
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary-foreground/10 backdrop-blur-sm">
                <svg
                  className="h-6 w-6"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" />
                </svg>
              </div>
              <span className="text-lg font-bold tracking-tight">JVA Montagens</span>
            </div>
          </div>

          <div className="max-w-md">
            <blockquote className="space-y-4">
              <p className="text-3xl font-bold leading-snug text-balance">
                Especialistas em Trampolim Parks
              </p>
              <p className="text-lg text-primary-foreground/80 leading-relaxed">
                Construindo parques de diversão com qualidade, segurança e inovação em cada projeto.
              </p>
              <footer className="text-primary-foreground/70 pt-4">
                <p className="font-semibold text-primary-foreground">JVA Montagens</p>
                <p className="text-sm">Referência em trampolim parks no Brasil</p>
              </footer>
            </blockquote>
          </div>

          <div className="flex items-center gap-6 text-sm text-primary-foreground/70 font-medium">
            <span>Trampolim Parks</span>
            <span className="h-1 w-1 rounded-full bg-primary-foreground/40" />
            <span>Instalação</span>
            <span className="h-1 w-1 rounded-full bg-primary-foreground/40" />
            <span>Manutenção</span>
          </div>
        </div>

        {/* Elemento decorativo */}
        <div className="absolute -bottom-32 -right-32 h-96 w-96 rounded-full bg-primary-foreground/5" />
        <div className="absolute -top-20 -right-20 h-64 w-64 rounded-full bg-primary-foreground/5" />
      </div>

      {/* Painel Direito - Formulário */}
      <div className="flex w-full lg:w-1/2 items-center justify-center p-8 bg-background">
        <LoginForm />
      </div>
    </main>
  )
}
