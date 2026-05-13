import { zodResolver } from '@hookform/resolvers/zod'
import { gooeyToast } from 'goey-toast'
import { Link, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

import { Button } from '../../components/ui/Button.jsx'
import { IonIcon } from '../../components/ui/IonIcon.jsx'
import { getApiErrorMessage } from '../../lib/apiClient.js'
import { useAuthStore } from '../../modules/auth/authStore.js'
import { normalizeAuthEmail } from '../../modules/auth/registerPayload.js'
import { AuthShell } from './AuthShell.jsx'

const loginSchema = z.object({
  email: z.string().trim().min(1, 'Vui lòng nhập email').email('Email không hợp lệ'),
  password: z.string().min(1, 'Vui lòng nhập mật khẩu'),
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
      const profiles = await login({
        email: normalizeAuthEmail(values.email),
        password: values.password,
      })
      gooeyToast.success('Đăng nhập thành công')
      navigate('/profiles', {
        replace: true,
        state: { profileCount: profiles.length },
      })
    } catch (error) {
      gooeyToast.error(getApiErrorMessage(error))
    }
  }

  return (
    <AuthShell
      description="Đăng nhập để chọn hồ sơ caregiver, elderly hoặc admin trước khi truy cập các khu vực được bảo vệ."
      eyebrow="Đăng nhập"
      title="Tiếp tục chăm sóc thuốc theo đúng hồ sơ"
    >
      <div className="mb-6">
        <p className="text-xs font-extrabold uppercase tracking-[0.08em] text-[#1f8a70]">
          Tài khoản
        </p>
        <h2 className="mt-1 text-2xl font-extrabold tracking-normal">Đăng nhập</h2>
      </div>

      <form className="grid gap-4" onSubmit={handleSubmit(onSubmit)}>
        <label className="grid gap-2 text-sm font-bold">
          Email
          <span className="flex min-h-12 items-center gap-3 rounded-lg border border-[#dbe4e0] bg-white px-3 focus-within:border-[#1f8a70] focus-within:ring-4 focus-within:ring-[#1f8a70]/10">
            <IonIcon className="text-xl text-[#65736f]" name="mail" />
            <input
              autoComplete="email"
              className="w-full border-0 bg-transparent text-base outline-none"
              placeholder="caregiver@example.com"
              type="email"
              {...register('email')}
            />
          </span>
          {errors.email ? (
            <span className="text-sm font-bold text-[#b42318]">{errors.email.message}</span>
          ) : null}
        </label>

        <label className="grid gap-2 text-sm font-bold">
          Mật khẩu
          <span className="flex min-h-12 items-center gap-3 rounded-lg border border-[#dbe4e0] bg-white px-3 focus-within:border-[#1f8a70] focus-within:ring-4 focus-within:ring-[#1f8a70]/10">
            <IonIcon className="text-xl text-[#65736f]" name="lock-closed" />
            <input
              autoComplete="current-password"
              className="w-full border-0 bg-transparent text-base outline-none"
              placeholder="Nhập mật khẩu"
              type="password"
              {...register('password')}
            />
          </span>
          {errors.password ? (
            <span className="text-sm font-bold text-[#b42318]">
              {errors.password.message}
            </span>
          ) : null}
        </label>

        <div className="flex items-center justify-between gap-3 text-sm">
          <Link className="font-bold text-[#1f8a70]" to="/forgot-password">
            Quên mật khẩu?
          </Link>
          <Link className="font-bold text-[#1f8a70]" to="/register">
            Tạo tài khoản
          </Link>
        </div>

        <Button disabled={isSubmitting} size="lg" type="submit">
          <IonIcon className="text-xl" name="log-in" />
          {isSubmitting ? 'Đang xử lý...' : 'Đăng nhập'}
        </Button>
      </form>
    </AuthShell>
  )
}