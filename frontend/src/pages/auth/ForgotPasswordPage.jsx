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

const forgotSchema = z.object({
  email: z.string().trim().min(1, 'Vui lòng nhập email').email('Email không hợp lệ'),
})

export function ForgotPasswordPage() {
  const navigate = useNavigate()
  const requestPasswordReset = useAuthStore((state) => state.requestPasswordReset)
  const {
    formState: { errors, isSubmitting },
    handleSubmit,
    register,
  } = useForm({
    resolver: zodResolver(forgotSchema),
    defaultValues: {
      email: '',
    },
  })

  const onSubmit = async (values) => {
    const email = normalizeAuthEmail(values.email)
    try {
      const response = await requestPasswordReset(email)
      gooeyToast.success(response.message || 'Nếu email tồn tại, OTP đã được gửi')
      navigate(`/reset-password?email=${encodeURIComponent(email)}`)
    } catch (error) {
      gooeyToast.error(getApiErrorMessage(error))
    }
  }

  return (
    <AuthShell
      description="Nhập email tài khoản để nhận OTP và link mở trang đặt lại mật khẩu."
      eyebrow="Quên mật khẩu"
      title="Khôi phục quyền truy cập tài khoản"
    >
      <div className="mb-6">
        <p className="text-xs font-extrabold uppercase tracking-[0.08em] text-[#1f8a70]">
          Reset password
        </p>
        <h2 className="mt-1 text-2xl font-extrabold tracking-normal">Quên mật khẩu</h2>
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

        <Button disabled={isSubmitting} size="lg" type="submit">
          <IonIcon className="text-xl" name="send" />
          {isSubmitting ? 'Đang gửi...' : 'Gửi OTP'}
        </Button>
      </form>

      <div className="mt-5 text-sm">
        <Link className="font-bold text-[#1f8a70]" to="/login">
          Quay lại đăng nhập
        </Link>
      </div>
    </AuthShell>
  )
}
