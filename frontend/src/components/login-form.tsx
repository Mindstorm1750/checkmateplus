import { useState } from "react"
import { Link, useNavigate } from "react-router"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { gqlNoJwt } from "@/lib/graphql"
import type { TotpLoginResponse } from "@/lib/playerServiceTypes/AuthResponses"
import { setLocalStorage } from "@/lib/local-storage.helpers.ts"
import GoogleIcon from "@/assets/icons/google.svg?react"

const LOGIN_WITH_TOTP = `
  mutation LoginWithTotp($username: String!, $code: String!) {
    loginWithTotp(username: $username, code: $code) {
      jwt
      playerId
      displayName
    }
  }
`

export function LoginForm({
  className,
  ...props
}: React.ComponentProps<"div">) {
  const navigate = useNavigate()
  const [username, setUsername] = useState("")
  const [code, setCode] = useState("")
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)

    try {
      const data = await gqlNoJwt<TotpLoginResponse>(LOGIN_WITH_TOTP, {
        username,
        code,
      })
      const { jwt, playerId, displayName } = data.loginWithTotp
      setLocalStorage("jwt", jwt)
      setLocalStorage("playerId", playerId)
      setLocalStorage("displayName", displayName)
      setLocalStorage("isGuest", "false")
      navigate("/")
    } catch (err) {
      setError(err instanceof Error ? err.message : "Login failed")
      setLocalStorage("isGuest", "true")
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className={cn("flex flex-col gap-6", className)} {...props}>
      <Card>
        <CardHeader>
          <CardTitle>Login to your account</CardTitle>
          <CardDescription>
            Enter your username and authenticator code
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit}>
            <FieldGroup>
              <Field>
                <FieldLabel htmlFor="username">Username</FieldLabel>
                <Input
                  id="username"
                  type="text"
                  placeholder="username"
                  required
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                />
              </Field>
              <Field>
                <FieldLabel htmlFor="totp-code">Authenticator Code</FieldLabel>
                <Input
                  id="totp-code"
                  type="text"
                  inputMode="numeric"
                  pattern="[0-9]{6}"
                  maxLength={6}
                  placeholder="000000"
                  required
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                />
                <FieldDescription>
                  6-digit code from your authenticator app
                </FieldDescription>
              </Field>
              {error && <p className="text-sm text-destructive">{error}</p>}
              <Field>
                <Button type="submit" disabled={loading}>
                  {loading ? "Logging in..." : "Login"}
                </Button>
                <Button variant="outline" type="button">
                  <GoogleIcon />
                  Login with Google
                </Button>
                <FieldDescription className="text-center">
                  Don&apos;t have an account?{" "}
                  <Link to="/register">Sign up</Link>
                </FieldDescription>
              </Field>
            </FieldGroup>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
