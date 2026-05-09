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
import { useJwt } from "@/lib/JwtProvider"
import { useLoginWithTotp } from "@/hooks/useAuthMutations"

export function LoginForm({
  className,
  ...props
}: React.ComponentProps<"div">) {
  const navigate = useNavigate()
  const { setAuth, signOut } = useJwt()
  const [username, setUsername] = useState("")
  const [code, setCode] = useState("")
  const loginMutation = useLoginWithTotp()
  const error = loginMutation.error ? loginMutation.error.message : null

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    loginMutation.mutate(
      { username, code },
      {
        onSuccess: (identity) => {
          setAuth(identity)
          navigate("/")
        },
        onError: () => {
          void signOut()
        },
      }
    )
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
                <Button type="submit" disabled={loginMutation.isPending}>
                  {loginMutation.isPending ? "Logging in..." : "Login"}
                </Button>
                {/*<Button variant="outline" type="button">*/}
                {/*  <GoogleIcon />*/}
                {/*  Login with Google*/}
                {/*</Button>*/}
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
