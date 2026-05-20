import { gooeyToast } from 'goey-toast'

import { getApiErrorMessage } from './apiClient.js'

const classNames = {
  wrapper: 'pharm-toast',
  content: 'pharm-toast__content',
  header: 'pharm-toast__header',
  title: 'pharm-toast__title',
  icon: 'pharm-toast__icon',
  description: 'pharm-toast__description',
  actionWrapper: 'pharm-toast__action-wrapper',
  actionButton: 'pharm-toast__action-button',
}

const palette = {
  default: {
    fillColor: '#f8fbf9',
    borderColor: '#b8d8cf',
  },
  success: {
    fillColor: '#ecfdf5',
    borderColor: '#34d399',
  },
  error: {
    fillColor: '#fff1f2',
    borderColor: '#fb7185',
  },
  warning: {
    fillColor: '#fff7ed',
    borderColor: '#f59e0b',
  },
  info: {
    fillColor: '#eff6ff',
    borderColor: '#60a5fa',
  },
}

function buildOptions(type, options = {}) {
  const { classNames: customClassNames, timing, ...rest } = options

  return {
    ...palette[type],
    borderWidth: 1.5,
    bounce: 0.18,
    preset: 'smooth',
    showProgress: false,
    showTimestamp: false,
    timing: {
      displayDuration: 4200,
      ...timing,
    },
    ...rest,
    classNames: {
      ...classNames,
      ...customClassNames,
    },
  }
}

function show(type, title, options) {
  const toastOptions = buildOptions(type, options)

  if (type === 'default') {
    return gooeyToast(title, toastOptions)
  }

  return gooeyToast[type](title, toastOptions)
}

export function getToastErrorMessage(error, fallback = 'Đã xảy ra lỗi. Vui lòng thử lại.') {
  const message = getApiErrorMessage(error)
  return message === 'Request failed' ? fallback : message
}

export const notify = {
  message: (title, options) => show('default', title, options),
  success: (title, options) => show('success', title, options),
  error: (title, options) => show('error', title, options),
  warning: (title, options) => show('warning', title, options),
  info: (title, options) => show('info', title, options),
  apiError: (error, fallback, options) =>
    show('error', getToastErrorMessage(error, fallback), options),
}
