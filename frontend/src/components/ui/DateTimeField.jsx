import { format, isValid, setHours, setMinutes } from 'date-fns'
import { CalendarDays } from 'lucide-react'
import { useMemo, useState } from 'react'
import { DayPicker } from 'react-day-picker'
import 'react-day-picker/style.css'

import { Button } from './Button.jsx'

function toDate(value) {
  if (!value) {
    return undefined
  }

  const date = value instanceof Date ? value : new Date(value)
  return isValid(date) ? date : undefined
}

function formatTime(date) {
  return date ? format(date, 'HH:mm') : '08:00'
}

export function DateTimeField({ label, value, onChange }) {
  const [open, setOpen] = useState(false)
  const selectedDate = useMemo(() => toDate(value), [value])
  const displayValue = selectedDate ? format(selectedDate, 'dd/MM/yyyy HH:mm') : 'Chon ngay gio'

  const updateDate = (date) => {
    if (!date) {
      return
    }

    const baseDate = selectedDate ?? new Date()
    onChange?.(setMinutes(setHours(date, baseDate.getHours()), baseDate.getMinutes()))
  }

  const updateTime = (event) => {
    const [hours, minutes] = event.target.value.split(':').map(Number)
    const baseDate = selectedDate ?? new Date()
    onChange?.(setMinutes(setHours(baseDate, hours), minutes))
  }

  return (
    <div className="field date-time-field">
      <label>{label}</label>
      <Button
        aria-expanded={open}
        className="date-time-trigger"
        type="button"
        variant="secondary"
        onClick={() => setOpen((current) => !current)}
      >
        <CalendarDays size={18} />
        <span>{displayValue}</span>
      </Button>
      {open ? (
        <div className="date-time-popover">
          <DayPicker mode="single" selected={selectedDate} onSelect={updateDate} />
          <label className="time-input">
            Gio nhac
            <input type="time" value={formatTime(selectedDate)} onChange={updateTime} />
          </label>
        </div>
      ) : null}
    </div>
  )
}
