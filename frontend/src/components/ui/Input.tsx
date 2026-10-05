import React, { useId } from 'react';

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helperText?: string;
  required?: boolean;
  icon?: React.ReactNode;
  rightAdornment?: React.ReactNode;
}

export const Input: React.FC<InputProps> = ({
  id: customId,
  label,
  error,
  helperText,
  required,
  icon,
  rightAdornment,
  className = '',
  style,
  ...props
}) => {
  const generatedId = useId();
  const inputId = customId || generatedId;
  const errorId = `${inputId}-error`;
  const helperId = `${inputId}-helper`;

  return (
    <div className="flex flex-col gap-1.5 w-full text-ink">
      {label && (
        <label
          htmlFor={inputId}
          className="font-heading font-bold text-xs uppercase tracking-wide flex items-center gap-1 select-none"
        >
          <span>{label}</span>
          {required && <span className="text-coral">*</span>}
        </label>
      )}

      <div className="relative w-full">
        {icon && (
          <div
            aria-hidden="true"
            className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 flex items-center justify-center pointer-events-none text-ink/70"
          >
            {icon}
          </div>
        )}

        {rightAdornment && (
          <div
            aria-hidden="true"
            className="absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none"
          >
            {rightAdornment}
          </div>
        )}

        <input
          id={inputId}
          required={required}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? errorId : helperText ? helperId : undefined}
          className={`w-full min-h-12 bg-paper border-3 border-ink text-ink text-sm placeholder:text-ink/40 transition-none focus-visible:outline-3 focus-visible:outline-ink focus-visible:outline-offset-2 disabled:opacity-50 disabled:cursor-not-allowed ${
            icon ? 'pl-10 pr-4' : 'pl-4 pr-4'
          } ${rightAdornment ? 'pr-12' : ''} py-3 ${className}`}
          style={{ fontFamily: 'system-ui', ...style }}
          {...props}
        />
      </div>

      {error && (
        <div
          id={errorId}
          className="border-3 border-ink bg-coral px-3 py-1.5 font-body text-xs font-bold text-ink mt-1 flex items-center gap-1.5"
        >
          <span>LỖI:</span>
          <span>{error}</span>
        </div>
      )}

      {!error && helperText && (
        <div id={helperId} className="font-body text-xs text-ink/70 mt-0.5">
          {helperText}
        </div>
      )}
    </div>
  );
};

export default Input;
