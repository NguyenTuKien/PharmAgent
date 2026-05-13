import { zodResolver } from '@hookform/resolvers/zod'
import { LockKeyhole, Mail } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { z } from 'zod'

import { Button } from '../components/ui/Button.jsx'
import { getApiErrorMessage } from '../lib/apiClient.js'
import { useAuthStore } from '../modules/auth/authStore.js'

const loginSchema = z.object({
  email: z.string().email('Email khong hop le'),
  password: z.string().min(6, 'Mat khau can toi thieu 6 ky tu'),
})

export function LoginPage() {
  const navigate = useNavigate()
  const login = useAuthStore((state) => state.login)
  const {
    formState: { errors, isSubmitting },
    handleSubmit,
    register,
  } = useForm({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  })

  const onSubmit = async (values) => {
    try {
      const profiles = await login(values)
      toast.success('Dang nhap thanh cong')
      navigate('/profiles', {
        replace: true,
        state: { profileCount: profiles.length },
      })
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-hero">
        <div className="brand auth-brand">
          <span className="brand-mark">P</span>
          <div>
            <strong>PharmAgent</strong>
            <span>Medication care</span>
          </div>
        </div>
        <div>
          <h1>Quan ly cham soc thuoc theo tung ho so</h1>
          <p>
            Dang nhap tai khoan, chon ho so phu hop, sau do frontend se dung access token
            theo role cho cac API va WebSocket tiep theo.
          </p>
        </div>
      </section>

      <section className="auth-card" aria-labelledby="login-title">
        <div>
          <p className="eyebrow">Phase 0</p>
          <h2 id="login-title">Dang nhap</h2>
        </div>
        <form className="form-stack" onSubmit={handleSubmit(onSubmit)}>
          <label className="field">
            Email
            <span className="input-shell">
              <Mail size={18} />
              <input
                autoComplete="email"
                placeholder="caregiver@example.com"
                type="email"
                {...register('email')}
              />
            </span>
            {errors.email ? <span className="field-error">{errors.email.message}</span> : null}
          </label>

          <label className="field">
            Mat khau
            <span className="input-shell">
              <LockKeyhole size={18} />
              <input
                autoComplete="current-password"
                placeholder="Nhap mat khau"
                type="password"
                {...register('password')}
              />
            </span>
            {errors.password ? (
              <span className="field-error">{errors.password.message}</span>
            ) : null}
          </label>

          <Button disabled={isSubmitting} size="lg" type="submit">
            {isSubmitting ? 'Dang xu ly...' : 'Dang nhap'}
          </Button>
        </form>
      </section>
    </main>
  )
}
