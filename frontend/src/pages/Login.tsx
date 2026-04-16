import { LoginForm } from "@/components/login-form"
import Layout from "@/components/Layout.tsx"

export default function Login() {
  return (
    <Layout>
      <div className="flex w-full items-center justify-center md:p-10">
        <div className="w-full max-w-sm">
          <LoginForm />
        </div>
      </div>
    </Layout>
  )
}
