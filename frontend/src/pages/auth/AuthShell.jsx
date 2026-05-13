import { Link } from 'react-router-dom'

import { IonIcon } from '../../components/ui/IonIcon.jsx'

export function AuthShell({ children, eyebrow, title, description }) {
  return (
    <main className="min-h-screen bg-[#f6f8fb] text-[#16211f]">
      <div className="mx-auto grid min-h-screen w-full max-w-6xl grid-cols-1 gap-8 px-5 py-8 lg:grid-cols-[0.95fr_1.05fr] lg:px-8">
        <section className="flex flex-col justify-between rounded-lg border border-[#dbe4e0] bg-white p-6 shadow-[0_18px_44px_rgb(22_33_31/0.08)] lg:p-8">
          <Link className="flex items-center gap-3" to="/login">
            <span className="grid size-11 place-items-center rounded-lg bg-[#1f8a70] text-white">
              <IonIcon className="text-2xl" name="medical" />
            </span>
            <span>
              <strong className="block text-lg leading-tight">PharmAgent</strong>
              <span className="block text-sm text-[#65736f]">Medication care</span>
            </span>
          </Link>

          <div className="py-12 lg:py-16">
            <p className="mb-3 text-xs font-extrabold uppercase tracking-[0.08em] text-[#1f8a70]">
              {eyebrow}
            </p>
            <h1 className="max-w-xl text-4xl font-extrabold leading-[1.05] tracking-normal sm:text-5xl">
              {title}
            </h1>
            <p className="mt-5 max-w-lg text-base leading-7 text-[#65736f]">{description}</p>
          </div>

          <div className="grid gap-3 text-sm text-[#65736f]">
            <div className="flex items-center gap-2">
              <IonIcon className="text-lg text-[#1f8a70]" name="shield-checkmark" />
              <span>JWT theo tài khoản, profile và vai trò.</span>
            </div>
            <div className="flex items-center gap-2">
              <IonIcon className="text-lg text-[#1f8a70]" name="mail" />
              <span>Xác minh email bằng OTP trước khi đăng nhập.</span>
            </div>
          </div>
        </section>

        <section className="flex items-center justify-center">
          <div className="w-full max-w-xl rounded-lg border border-[#dbe4e0] bg-white p-5 shadow-[0_18px_44px_rgb(22_33_31/0.08)] sm:p-7">
            {children}
          </div>
        </section>
      </div>
    </main>
  )
}
