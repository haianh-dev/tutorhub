import React from 'react';

export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: 'paper' | 'cream' | 'yellow' | 'blue' | 'coral';
  interactive?: boolean;
}

const bgVariants = {
  paper: 'bg-paper',
  cream: 'bg-cream',
  yellow: 'bg-yellow',
  blue: 'bg-blue',
  coral: 'bg-coral',
} as const;

export const Card: React.FC<CardProps> = ({
  variant = 'paper',
  interactive = false,
  className = '',
  children,
  ...props
}) => {
  const boxClass = interactive ? 'brut-pop' : 'brut-box';

  return (
    <div
      className={`${boxClass} ${bgVariants[variant]} p-6 flex flex-col gap-4 text-ink ${className}`}
      {...props}
    >
      {children}
    </div>
  );
};

export default Card;
