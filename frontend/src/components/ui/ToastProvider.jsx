import { GooeyToaster } from 'goey-toast'

export function ToastProvider() {
  return (
    <GooeyToaster
      closeButton="top-right"
      gap={12}
      offset="clamp(12px, 3vw, 24px)"
      position="top-center"
      preset="smooth"
      showProgress={false}
      swipeToDismiss
      theme="light"
    />
  )
}
