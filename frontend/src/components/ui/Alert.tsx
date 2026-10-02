import React from 'react';

export interface AlertProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: 'info' | 'warning' | 'error';
  title?: string;
  icon?: React.ReactNode;
}

const variants = {
  info: {
    bg: 'bg-blue',
    defaultIcon: 'i',
  },
  warning: {
    bg: 'bg-yellow',
    defaultIcon: '!',
  },
  error: {
    bg: 'bg-coral',
    defaultIcon: '✕',
  },
} as const;

export const Alert: React.FC<AlertProps> = ({
  variant = 'info',
  title,
  icon,
  className = '',
  children,
  ...props
}) => {
  const config = variants[variant];
  const displayIcon = icon ?? config.defaultIcon;

  return (
    <div
      role={variant === 'error' ? 'alert' : 'status'}
      className={`brut-box ${config.bg} p-4 flex items-start gap-4 text-ink ${className}`}
      {...props}
    >
      <div className="w-10 h-10 shrink-0 border-3 border-ink bg-paper flex items-center justify-center font-heading font-black text-lg select-none text-ink">
        {displayIcon}
      </div>
      <div className="flex-1 min-w-0 py-0.5">
        {title && (
          <h4 className="font-heading font-bold uppercase text-sm mb-1 text-ink">
            {title}
          </h4>
        )}
        <div className="text-sm font-body text-ink break-words leading-relaxed">
          {children}
        </div>
      </div>
    </div>
  );
};

export default Alert;
