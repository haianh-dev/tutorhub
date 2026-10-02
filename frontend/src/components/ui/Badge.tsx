import React from 'react';

export interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  variant?: 'paper' | 'yellow' | 'blue' | 'coral';
  icon?: React.ReactNode;
}

const variants = {
  paper: 'bg-paper',
  yellow: 'bg-yellow',
  blue: 'bg-blue',
  coral: 'bg-coral',
} as const;

export const Badge: React.FC<BadgeProps> = ({
  variant = 'paper',
  icon,
  className = '',
  children,
  ...props
}) => {
  return (
    <span
      className={`inline-flex items-center gap-1.5 border-2 border-ink px-2 py-1 text-xs font-heading font-bold uppercase text-ink select-none ${variants[variant]} ${className}`}
      {...props}
    >
      {icon && <span className="shrink-0">{icon}</span>}
      <span>{children}</span>
    </span>
  );
};

export default Badge;
