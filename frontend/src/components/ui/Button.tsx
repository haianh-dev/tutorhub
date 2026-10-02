import React from 'react';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'info' | 'danger';
  size?: 'md' | 'sm';
  isLoading?: boolean;
  loadingText?: string;
}

const variants = {
  primary: 'bg-yellow',
  secondary: 'bg-paper',
  info: 'bg-blue',
  danger: 'bg-coral',
} as const;

export const Button: React.FC<ButtonProps> = ({
  variant = 'secondary',
  size = 'md',
  isLoading = false,
  loadingText,
  disabled,
  children,
  className = '',
  ...props
}) => {
  const sizeClasses = size === 'sm' ? 'min-h-10 px-4 py-2 text-xs' : 'min-h-12 px-6 py-4 text-sm';
  const isDisabled = disabled || isLoading;

  return (
    <button
      disabled={isDisabled}
      aria-disabled={isDisabled}
      className={`brut-pop font-heading font-bold uppercase text-ink inline-flex items-center justify-center gap-2 text-center select-none ${variants[variant]} ${sizeClasses} ${className}`}
      {...props}
    >
      {isLoading ? (loadingText ?? 'ĐANG XỬ LÝ…') : children}
    </button>
  );
};

export default Button;
