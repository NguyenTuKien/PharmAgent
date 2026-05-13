import { zodResolver } from '@hookform/resolvers/zod'
import { gooeyToast } from 'goey-toast'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useCallback, useEffect, useRef } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

import { Button } from '../../components/ui/Button.jsx'
import { IonIcon } from '../../components/ui/IonIcon.jsx'
import { getApiErrorMessage } from '../../lib/apiClient.js'
import { useAuthStore } from '../../modules/auth/authStore.js'
import { normalizeAuthEmail } from '../../modules/auth/registerPayload.js'
import { AuthShell } from './AuthShell.jsx'

const verifySchema = z.object({
  email: z.string().trim().min(1, 'Vui lòng nhập email').email('Email không hợp lệ'),
  otp: z.string().trim().min(1, 'Vui lòng nhập OTP'),
})

export function VerifyEmailPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const autoVerifiedRef = useRef(false)
  const resendVerification = useAuthStore((state) => state.resendVerification)
  const verifyEmail = useAuthStore((state) => state.verifyEmail)
  const {
    formState: { errors, isSubmitting },
    getValues,
    handleSubmit,
    register,
    setValue,
  } = useForm({
    resolver: zodResolver(verifySchema),
    defaultValues: {
      email: searchParams.get('email') || '',
      otp: searchParams.get('otp') || '',
    },
  })

  const onSubmit = useCallback(async (values) => {
    try {
      const response = await verifyEmail({
        email: normalizeAuthEmail(values.email),
        otp: values.otp.trim(),
      })
      gooeyToast.success(response.message || 'Email đã được xác minh')
      navigate('/login', { replace: true })
    } catch (error) {
      gooeyToast.error(getApiErrorMessage(error))
    }
  }, [navigate, verifyEmail])

  useEffect(() => {
    const email = searchParams.get('email')
    const otp = searchParams.get('otp')
    if (autoVerifiedRef.current || !email || !otp) {
      return
    }

    autoVerifiedRef.current = true
    setValue('email', email)
    setValue('otp', otp)
    onSubmit({ email, otp })
  }, [onSubmit, searchParams, setValue])

  const handleResend = async () => {
    const email = normalizeAuthEmail(getValues('email'))
    if (!email) {
      gooeyToast.error('Vui lòng nhập email để gửi lại OTP')
      return
    }

    try {
      const response = await resendVerification(email)
      gooeyToast.success(response.message || 'Đã gửi lại OTP xác minh')
    } catch (error) {
      gooeyToast.error(getApiErrorMessage(error))
    }
  }

  return (
    <AuthShell
      description="Nhập OTP từ email hoặc mở link trong email để kích hoạt tài khoản PharmAgent."
      eyebrow="Xác minh"
      title="Kích hoạt tài khoản bằng email"
    >
      <div className="mb-6">
        <p className="text-xs font-extrabold uppercase tracking-[0.08em] text-[#1f8a70]">
          OTP email
        </p>
        <h2 className="mt-1 text-2xl font-extrabold tracking-normal">Xác minh email</h2>
      </div>

      <form className="grid gap-4" onSubmit={handleSubmit(onSubmit)}>
        <label className="grid gap-2 text-sm font-bold">
          Email
          <span className="flex min-h-12 items-center gap-3 rounded-lg border border-[#dbe4e0] bg-white px-3 focus-within:border-[#1f8a70] focus-within:ring-4 focus-within:ring-[#1f8a70]/10">
            <IonIcon className="text-xl text-[#65736f]" name="mail" />
            <input
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
          Mã OTP
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

        <Button disabled={isSubmitting} size="lg" type="submit">
          <IonIcon className="text-xl" name="checkmark-circle" />
          {isSubmitting ? 'Đang xác minh...' : 'Xác minh email'}
        </Button>
      </form>

      <div className="mt-5 flex flex-wrap items-center justify-between gap-3 text-sm">
        <button className="font-bold text-[#1f8a70]" type="button" onClick={handleResend}>
          Gửi lại OTP
        </button>
        <Link className="font-bold text-[#1f8a70]" to="/login">
          Quay lại đăng nhập
        </Link>
      </div>
    </AuthShell>
  )
}
