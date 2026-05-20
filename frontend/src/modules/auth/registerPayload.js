export function normalizeAuthEmail(email) {
  return typeof email === 'string' ? email.trim().toLowerCase() : ''
}

function optionalText(value) {
  const normalized = typeof value === 'string' ? value.trim() : ''
  return normalized || undefined
}

function splitDisplayName(value) {
  const normalized = optionalText(value) || ''
  const parts = normalized.split(/\s+/).filter(Boolean)

  if (parts.length <= 1) {
    return {
      firstName: parts[0] || 'Nguoi dung',
      lastName: undefined,
    }
  }

  return {
    firstName: parts.slice(0, -1).join(' '),
    lastName: parts.at(-1),
  }
}

function profilePayload(values, prefix = '') {
  const field = (name) => values[`${prefix}${name}`]
  const fallbackName = prefix ? {} : splitDisplayName(values.username || values.fullName)

  return {
    firstName: optionalText(field('FirstName')) ?? optionalText(values.firstName) ?? fallbackName.firstName,
    lastName: optionalText(field('LastName')) ?? optionalText(values.lastName) ?? fallbackName.lastName,
    phone: optionalText(field('Phone')) ?? optionalText(values.phone),
    dateOfBirth: optionalText(field('DateOfBirth')) ?? optionalText(values.dateOfBirth),
    gender: optionalText(field('Gender')) ?? optionalText(values.gender),
    address: optionalText(field('Address')) ?? optionalText(values.address),
  }
}

export function buildRegisterPayload(values) {
  const caregiver = profilePayload(values)
  const payload = {
    email: normalizeAuthEmail(values.email),
    password: values.password,
    confirmPassword: values.confirmPassword || values.password,
    caregiver,
  }

  if (values.includeElderly) {
    payload.elderly = {
      ...profilePayload(values, 'elderly'),
      caregiverTitle: optionalText(values.caregiverTitle),
      elderlyTitle: optionalText(values.elderlyTitle),
      permissionLevel: values.permissionLevel || 'MANAGE_ALL',
    }
  }

  Object.values(payload).forEach((section) => {
    if (section && typeof section === 'object') {
      Object.keys(section).forEach((key) => {
        if (section[key] === undefined) {
          delete section[key]
        }
      })
    }
  })

  return payload
}
