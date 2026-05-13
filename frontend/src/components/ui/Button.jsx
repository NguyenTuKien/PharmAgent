export function Button({
  children,
  className = '',
  variant = 'primary',
  size = 'md',
  type = 'button',
  ...props
}) {
  return (
    <button className={`btn btn--${variant} btn--${size} ${className}`} type={type} {...props}>
      {children}
    </button>
  )
}
