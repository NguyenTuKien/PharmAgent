import { X } from 'lucide-react'

import { Button } from './Button.jsx'

export function ConfirmDialog({
  confirmLabel = 'Xac nhan',
  description,
  open,
  title,
  onConfirm,
  onOpenChange,
}) {
  if (!open) {
    return null
  }

  const close = () => onOpenChange?.(false)

  return (
    <>
      <div className="modal-overlay" onClick={close} />
      <div aria-modal="true" className="modal-panel" role="dialog">
        <div className="modal-header">
          <h2>{title}</h2>
          <button aria-label="Dong" className="icon-button" type="button" onClick={close}>
            <X size={18} />
          </button>
        </div>
        {description ? <p>{description}</p> : null}
        <div className="modal-actions">
          <Button variant="ghost" onClick={close}>
            Huy
          </Button>
          <Button
            variant="danger"
            onClick={() => {
              onConfirm?.()
              close()
            }}
          >
            {confirmLabel}
          </Button>
        </div>
      </div>
    </>
  )
}
