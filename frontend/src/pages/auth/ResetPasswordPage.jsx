import { zodResolver } from '@hookform/resolvers/zod'
import { gooeyToast } from 'goey-toast'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

import { Button } from '../../components/ui/Button.jsx'
import { IonIcon } from '../../components/ui/IonIcon.jsx'
import { getApiErrorMessage } from '../../lib/apiClient.js'
import { useAuthStore } from '../../modules/auth/authStore.js'
import { normalizeAuthEmail } from '../../modules/auth/registerPayload.js'
import { AuthShell } from './AuthShell.jsx'

const resetSchema = z.object({
  email: z.string().trim().min(1, 'Vui lòng nhập email').email('Email không hợp lệ'),
  otp: z.string().trim().min(1, 'Vui lòng nhập OTP'),
  newPassword: z.string().min(1, 'Vui lòng nhập mật khẩu mới'),
})

export function ResetPasswordPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const resetPassword = useAuthStore((state) => state.resetPassword)
  const {
    formState: { errors, isSubmitting },
    handleSubmit,
    register,
  } = useForm({
    resolver: zodResolver(resetSchema),
    defaultValues: {
      email: searchParams.get('email') || '',
      otp: '',
      newPassword: '',
    },
  })

  const onSubmit = async (values) => {
    try {
      await resetPassword({
        email: normalizeAuthEmail(values.email),
        otp: values.otp.trim(),
        newPassword: values.newPassword,
        confirmPassword: values.newPassword,
      })
      gooeyToast.success('Mật khẩu đã được đặt lại')
      navigate('/login', { replace: true })
    } catch (error) {
      gooeyToast.error(getApiErrorMessage(error))
    }
  }

  return (
    <AuthShell
      description="Dùng OTP trong email để đặt mật khẩu mới. Form không dùng kiểm tra độ mạnh hay đối chiếu lặp lại mật khẩu."
      eyebrow="Reset"
      title="Đặt lại mật khẩu bằng OTP"
    >
      <div className="mb-6">
        <p className="text-xs font-extrabold uppercase tracking-[0.08em] text-[#1f8a70]">
          Mật khẩu mới
        </p>
        <h2 className="mt-1 text-2xl font-extrabold tracking-normal">Đặt lại mật khẩu</h2>
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
          OTP
          <span className="flex min-h-12 items-center gap-3 rounded-lg border border-[#dbe4e0] bg-white px-3 focus-within:border-[#1f8a70] focus-within:ring-4 focus-within:ring-[#1f8a70]/10">
            <IonIcon className="text-xl text-[#65736f]" name="key" />
            <input
              className="w-full border-0 bg-transparent text-base tracking-[0.18em] outline-none"
              inputMode="numeric"
              placeholder="123456"
              {...register('otp')}
            />
          </span>
          {errors.otp ? (
            <span className="text-sm font-bold text-[#b42318]">{errors.otp.message}</span>
          ) : null}
        </label>

        <label className="grid gap-2 text-sm font-bold">
          Mật khẩu mới
          <span className="flex min-h-12 items-center gap-3 rounded-lg border border-[#dbe4e0] bg-white px-3 focus-within:border-[#1f8a70] focus-within:ring-4 focus-within:ring-[#1f8a70]/10">
            <IonIcon className="text-xl text-[#65736f]" name="lock-closed" />
            <input
              autoComplete="new-password"
              className="w-full border-0 bg-transparent text-base outline-none"
              placeholder="Nhập mật khẩu mới"
              type="password"
              {...register('newPassword')}
            />
          </span>
          {errors.newPassword ? (
            <span className="text-sm font-bold text-[#b42318]">
              {errors.newPassword.message}
            </span>
          ) : null}
        </label>

        <Button disabled={isSubmitting} size="lg" type="submit">
          <IonIcon className="text-xl" name="refresh-circle" />
          {isSubmitting ? 'Đang đặt lại...' : 'Đặt lại mật khẩu'}
        </Button>
      </form>

      <div className="mt-5 flex flex-wrap justify-between gap-3 text-sm">
        <Link className="font-bold text-[#1f8a70]" to="/forgot-password">
          Gửi lại OTP
        </Link>
        <Link className="font-bold text-[#1f8a70]" to="/login">
          Quay lại đăng nhập
        </Link>
      </div>
    </AuthShell>
  )
}
