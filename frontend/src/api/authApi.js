import {
  forgotPasswordRequest,
  resendVerificationRequest,
  resetPasswordRequest,
} from '../modules/auth/authApi.js'

export async function resendOTP(email) {
  return resendVerificationRequest(email)
}

export async function forgotPassword(email) {
  return forgotPasswordRequest(email)
}

export async function resetPassword(email, token, newPassword, confirmPassword = newPassword) {
  return resetPasswordRequest({
    email,
    token,
    newPassword,
    confirmPassword,
  })
}
