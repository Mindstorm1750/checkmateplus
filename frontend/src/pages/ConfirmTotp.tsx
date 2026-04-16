import { useState } from "react"
import { useLocation, useNavigate } from "react-router"
import { QRCodeSVG } from "qrcode.react"
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
import type { ConfirmTotpResponse } from "@/lib/playerServiceTypes/AuthResponses"
import { setLocalStorage } from "@/lib/local-storage.helpers"
import Layout from "@/components/Layout"

const CONFIRM_TOTP = `
  mutation ConfirmTotp($code: String!) {
    confirmTotp(code: $code) {
      jwt
      playerId
      displayName
      refreshToken
    }
  }
`

export default function ConfirmTotp() {
  const location = useLocation()
  const navigate = useNavigate()
  const { qrCodeUri, secret } = location.state

  const [code, setCode] = useState("")
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  if (!qrCodeUri) {
    return (
      <div className="flex min-h-svh w-full items-center justify-center p-6">
        <Card className="w-full max-w-md">
          <CardContent className="pt-6 text-center">
            <p>No registration data found.</p>
            <Button className="mt-4" onClick={() => navigate("/register")}>
              Go to Register
            </Button>
          </CardContent>
        </Card>
      </div>
    )
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)

    try {
      const data = await gqlNoJwt<ConfirmTotpResponse>(CONFIRM_TOTP, { code })
      const { jwt, playerId, displayName } = data.confirmTotp
      console.log({ data })
      setLocalStorage("jwt", jwt)
      setLocalStorage("playerId", playerId)
      setLocalStorage("displayName", displayName)
      setLocalStorage("isGuest", "false")
      navigate("/")
    } catch (err) {
      console.log(err)
      setError(err instanceof Error ? err.message : "Verification failed")
    } finally {
      setLoading(false)
    }
  }

  return (
    <Layout>
      <div className="flex w-full items-center justify-center md:p-4">
        <Card className="w-full max-w-md">
          <CardHeader className="text-center">
            <CardTitle>Set up your authenticator</CardTitle>
            <CardDescription>
              Scan the QR code with your authenticator app, then enter the
              6-digit code to verify
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex flex-col items-center gap-6">
              <div className="rounded-lg bg-white p-4">
                <QRCodeSVG value={qrCodeUri} size={200} />
              </div>

              <details className="w-full text-sm">
                <summary className="cursor-pointer text-muted-foreground">
                  Can't scan? Enter this key manually
                </summary>
                <code className="mt-2 block rounded bg-muted p-2 text-xs break-all">
                  {secret}
                </code>
              </details>

              <form onSubmit={handleSubmit} className="w-full">
                <FieldGroup>
                  <Field>
                    <FieldLabel htmlFor="totp-code">
                      Authenticator Code
                    </FieldLabel>
                    <Input
                      id="totp-code"
                      type="text"
                      inputMode="numeric"
                      pattern="[0-9]{6}"
                      maxLength={6}
                      placeholder="000000"
                      autoComplete="one-time-code"
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
                      {loading ? "Verifying..." : "Verify & Complete Setup"}
                    </Button>
                  </Field>
                </FieldGroup>
              </form>
            </div>
          </CardContent>
        </Card>
      </div>
    </Layout>
  )
}
