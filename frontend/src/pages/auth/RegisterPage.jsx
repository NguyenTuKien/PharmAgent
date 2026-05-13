import { zodResolver } from '@hookform/resolvers/zod'
import { gooeyToast } from 'goey-toast'
import { Link, useNavigate } from 'react-router-dom'
import { useForm, useWatch } from 'react-hook-form'
import { z } from 'zod'

import { Button } from '../../components/ui/Button.jsx'
import { IonIcon } from '../../components/ui/IonIcon.jsx'
import { getApiErrorMessage } from '../../lib/apiClient.js'
import { useAuthStore } from '../../modules/auth/authStore.js'
import { buildSignupPayload } from '../../modules/auth/registerPayload.js'
import { AuthShell } from './AuthShell.jsx'

const requiredText = (message) => z.string().trim().min(1, message)

const registerSchema = z.object({
  email: requiredText('Vui lòng nhập email').email('Email không hợp lệ'),
  password: requiredText('Vui lòng nhập mật khẩu'),
  firstName: requiredText('Vui lòng nhập tên'),
  lastName: requiredText('Vui lòng nhập họ'),
  phone: requiredText('Vui lòng nhập số điện thoại'),
  dateOfBirth: requiredText('Vui lòng chọn ngày sinh'),
  gender: requiredText('Vui lòng chọn giới tính'),
  address: z.string().optional(),
  includeElderly: z.boolean().optional(),
  elderlyFirstName: z.string().optional(),
  elderlyLastName: z.string().optional(),
  elderlyPhone: z.string().optional(),
  elderlyDateOfBirth: z.string().optional(),
  elderlyGender: z.string().optional(),
  elderlyAddress: z.string().optional(),
  caregiverTitle: z.string().optional(),
  elderlyTitle: z.string().optional(),
  permissionLevel: z.string().optional(),
})

function Field({ children, error, icon, label }) {
  return (
    <label className="grid gap-2 text-sm font-bold">
      {label}
      <span className="flex min-h-12 items-center gap-3 rounded-lg border border-[#dbe4e0] bg-white px-3 focus-within:border-[#1f8a70] focus-within:ring-4 focus-within:ring-[#1f8a70]/10">
        {icon ? <IonIcon className="text-xl text-[#65736f]" name={icon} /> : null}
        {children}
      </span>
      {error ? <span className="text-sm font-bold text-[#b42318]">{error}</span> : null}
    </label>
  )
}

export function RegisterPage() {
  const navigate = useNavigate()
  const registerAccount = useAuthStore((state) => state.registerAccount)
  const {
    formState: { errors, isSubmitting },
    handleSubmit,
    control,
    register,
  } = useForm({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      email: '',
      password: '',
      firstName: '',
      lastName: '',
      phone: '',
      dateOfBirth: '',
      gender: 'MALE',
      address: '',
      includeElderly: false,
      elderlyFirstName: '',
      elderlyLastName: '',
      elderlyPhone: '',
      elderlyDateOfBirth: '',
      elderlyGender: 'FEMALE',
      elderlyAddress: '',
      caregiverTitle: 'Người chăm sóc',
      elderlyTitle: 'Người thân',
      permissionLevel: 'MANAGE_ALL',
    },
  })
  const includeElderly = useWatch({ control, name: 'includeElderly' })

  const onSubmit = async (values) => {
    try {
      const response = await registerAccount(buildSignupPayload(values))
      gooeyToast.success(response.message || 'Vui lòng kiểm tra email để kích hoạt tài khoản')
      navigate(`/verify-email?email=${encodeURIComponent(response.email || values.email)}`, {
        replace: true,
      })
    } catch (error) {
      gooeyToast.error(getApiErrorMessage(error))
    }
  }

  return (
    <AuthShell
      description="Tạo tài khoản caregiver. Sau khi đăng ký, PharmAgent gửi OTP về email để xác nhận email là chính chủ trước khi cho đăng nhập."
      eyebrow="Đăng ký"
      title="Tạo tài khoản và xác minh email"
    >
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <p className="text-xs font-extrabold uppercase tracking-[0.08em] text-[#1f8a70]">
            Tài khoản mới
          </p>
          <h2 className="mt-1 text-2xl font-extrabold tracking-normal">Đăng ký</h2>
        </div>
        <Link className="text-sm font-bold text-[#1f8a70]" to="/login">
          Đã có tài khoản
        </Link>
      </div>

      <form className="grid gap-4" onSubmit={handleSubmit(onSubmit)}>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field error={errors.email?.message} icon="mail" label="Email">
            <input
              autoComplete="email"
              className="w-full border-0 bg-transparent text-base outline-none"
              placeholder="caregiver@example.com"
              type="email"
              {...register('email')}
            />
          </Field>
          <Field error={errors.password?.message} icon="lock-closed" label="Mật khẩu">
            <input
              autoComplete="new-password"
              className="w-full border-0 bg-transparent text-base outline-none"
              placeholder="Nhập mật khẩu"
              type="password"
              {...register('password')}
            />
          </Field>
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <Field error={errors.lastName?.message} icon="person" label="Họ">
            <input
              className="w-full border-0 bg-transparent text-base outline-none"
              placeholder="Nguyễn"
              {...register('lastName')}
            />
          </Field>
          <Field error={errors.firstName?.message} icon="person" label="Tên">
            <input
              className="w-full border-0 bg-transparent text-base outline-none"
              placeholder="An"
              {...register('firstName')}
            />
          </Field>
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <Field error={errors.phone?.message} icon="call" label="Số điện thoại">
            <input
              autoComplete="tel"
              className="w-full border-0 bg-transparent text-base outline-none"
              placeholder="0912345678"
              {...register('phone')}
            />
          </Field>
          <Field error={errors.dateOfBirth?.message} icon="calendar" label="Ngày sinh">
            <input
              className="w-full border-0 bg-transparent text-base outline-none"
              type="date"
              {...register('dateOfBirth')}
            />
          </Field>
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <Field error={errors.gender?.message} icon="male-female" label="Giới tính">
            <select
              className="w-full border-0 bg-transparent text-base outline-none"
              {...register('gender')}
            >
              <option value="MALE">Nam</option>
              <option value="FEMALE">Nữ</option>
              <option value="OTHER">Khác</option>
            </select>
          </Field>
          <Field icon="location" label="Địa chỉ">
            <input
              className="w-full border-0 bg-transparent text-base outline-none"
              placeholder="Không bắt buộc"
              {...register('address')}
            />
          </Field>
        </div>

        <label className="flex items-center gap-3 rounded-lg border border-[#dbe4e0] p-3 text-sm font-bold">
          <input className="size-4 accent-[#1f8a70]" type="checkbox" {...register('includeElderly')} />
          Tạo thêm hồ sơ người thân cần chăm sóc
        </label>

        {includeElderly ? (
          <div className="grid gap-4 rounded-lg border border-[#dbe4e0] bg-[#f8fbfa] p-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <Field icon="person" label="Họ người thân">
                <input
                  className="w-full border-0 bg-transparent text-base outline-none"
                  placeholder="Trần"
                  {...register('elderlyLastName')}
                />
              </Field>
              <Field icon="person" label="Tên người thân">
                <input
                  className="w-full border-0 bg-transparent text-base outline-none"
                  placeholder="Bình"
                  {...register('elderlyFirstName')}
                />
              </Field>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field icon="call" label="Số điện thoại người thân">
                <input
                  className="w-full border-0 bg-transparent text-base outline-none"
                  placeholder="0987654321"
                  {...register('elderlyPhone')}
                />
              </Field>
              <Field icon="calendar" label="Ngày sinh người thân">
                <input
                  className="w-full border-0 bg-transparent text-base outline-none"
                  type="date"
                  {...register('elderlyDateOfBirth')}
                />
              </Field>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field icon="male-female" label="Giới tính người thân">
                <select
                  className="w-full border-0 bg-transparent text-base outline-none"
                  {...register('elderlyGender')}
                >
                  <option value="MALE">Nam</option>
                  <option value="FEMALE">Nữ</option>
                  <option value="OTHER">Khác</option>
                </select>
              </Field>
              <Field icon="shield-checkmark" label="Quyền chăm sóc">
                <select
                  className="w-full border-0 bg-transparent text-base outline-none"
                  {...register('permissionLevel')}
                >
                  <option value="MANAGE_ALL">Quản lý toàn bộ</option>
                  <option value="VIEW">Chỉ xem</option>
                </select>
              </Field>
            </div>
          </div>
        ) : null}

        <Button disabled={isSubmitting} size="lg" type="submit">
          <IonIcon className="text-xl" name="person-add" />
          {isSubmitting ? 'Đang tạo...' : 'Tạo tài khoản'}
        </Button>
      </form>
    </AuthShell>
  )
}
