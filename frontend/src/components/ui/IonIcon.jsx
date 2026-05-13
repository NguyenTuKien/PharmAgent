import { createElement } from 'react'

export function IonIcon({ name, className = '', label, ...props }) {
  return createElement('ion-icon', {
    name,
    class: className,
    'aria-hidden': label ? undefined : 'true',
    'aria-label': label,
    ...props,
  })
}
