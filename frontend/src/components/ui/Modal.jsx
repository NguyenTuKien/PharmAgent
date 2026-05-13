import * as Dialog from '@radix-ui/react-dialog'
import { X } from 'lucide-react'

import { Button } from './Button.jsx'

export function ConfirmDialog({
  open,
  onOpenChange,
  title,
  description,
  confirmLabel = 'Xac nhan',
  cancelLabel = 'Huy',
  onConfirm,
  tone = 'danger',
}) {
  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="modal-overlay" />
        <Dialog.Content className="modal-panel">
          <div className="modal-header">
            <Dialog.Title>{title}</Dialog.Title>
            <Dialog.Close asChild>
              <button aria-label="Dong hop thoai" className="icon-button" type="button">
                <X size={18} />
              </button>
            </Dialog.Close>
          </div>
          <Dialog.Description>{description}</Dialog.Description>
          <div className="modal-actions">
            <Dialog.Close asChild>
              <Button variant="ghost">{cancelLabel}</Button>
            </Dialog.Close>
            <Button
              variant={tone}
              onClick={() => {
                onConfirm?.()
                onOpenChange?.(false)
              }}
            >
              {confirmLabel}
            </Button>
          </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
