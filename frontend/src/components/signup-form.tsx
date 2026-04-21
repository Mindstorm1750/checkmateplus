import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { gqlNoJwt } from "@/lib/graphql.ts"
import type { RegisterResponse } from "@/lib/playerServiceTypes/AuthResponses.ts"
import { useJwt } from "@/lib/JwtProvider"
import { Link, useNavigate } from "react-router"
import { useState } from "react"
import { Card, CardContent } from "./ui/card"
import GoogleIcon from "@/assets/icons/google.svg?react"

const REGISTER_WITH_USERNAME = `
  mutation Register($username: String!) {
    register(username: $username) {
      qrCodeUri
      secret
      playerId
    }
  }
`
export function SignupForm({
  className,
  ...props
}: React.ComponentProps<"div">) {
  const navigate = useNavigate()
  const { signOut } = useJwt()
  const [username, setUsername] = useState("")
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)

    try {
      const data = await gqlNoJwt<RegisterResponse>(REGISTER_WITH_USERNAME, {
        username,
      })
      console.log({ data })
      navigate("/confirmTOTP", { state: data.register })
    } catch (err) {
      setError(err instanceof Error ? err.message : "Login failed")
      signOut()
    } finally {
      setLoading(false)
    }
  }
  return (
    <div className={cn("flex flex-col gap-6", className)} {...props}>
      <Card>
        <CardContent>
          <form onSubmit={handleSubmit}>
            <FieldGroup>
              <div className="flex flex-col items-center gap-2 text-center">
                <h1 className="text-xl font-bold">Welcome to Checkmate+</h1>
                <FieldDescription>
                  Already have an account? <Link to="/login">Sign in</Link>
                </FieldDescription>
              </div>
              <Field>
                <FieldLabel htmlFor="username">Username</FieldLabel>
                <Input
                  id="username"
                  type="text"
                  placeholder="Enter your username"
                  required
                  value={username}
                  onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                    setUsername(e.target.value)
                  }}
                />
              </Field>
              {error && <p className="text-sm text-destructive">{error}</p>}
              <Field>
                <Button type="submit" disabled={loading}>
                  Create Account
                </Button>
              </Field>
              <Field className="grid gap-4 sm:grid-cols-1">
                <Button variant="outline" type="button">
                  <GoogleIcon />
                  Continue with Google
                </Button>
              </Field>
            </FieldGroup>
          </form>
        </CardContent>
      </Card>

      <FieldDescription className="px-6 text-center">
        By clicking continue, you agree to our <a href="#">Terms of Service</a>{" "}
        and <a href="#">Privacy Policy</a>.
      </FieldDescription>
    </div>
  )
}
