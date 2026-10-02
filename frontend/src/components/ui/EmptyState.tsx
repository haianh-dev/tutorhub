import React from 'react';

export interface EmptyStateProps {
  title: string;
  description: string;
  actionText?: string;
  onAction?: () => void;
  icon?: React.ReactNode;
  className?: string;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  title,
  description,
  actionText,
  onAction,
  icon,
  className = '',
}) => {
  return (
    <div
      className={`border-3 border-dashed border-ink bg-paper p-8 sm:p-12 text-center flex flex-col items-center justify-center gap-4 text-ink ${className}`}
    >
      {icon && (
        <div className="w-16 h-16 border-3 border-ink bg-yellow flex items-center justify-center font-heading font-black text-2xl mb-1 text-ink select-none">
          {icon}
        </div>
      )}

      <h3 className="font-heading font-black uppercase text-lg sm:text-xl tracking-wide text-ink">
        {title}
      </h3>

      <p className="font-body text-sm text-ink/80 max-w-md leading-relaxed">
        {description}
      </p>

      {actionText && onAction && (
        <div className="pt-2">
          <button
            type="button"
            onClick={onAction}
            className="brut-pop min-h-12 px-6 py-4 font-heading text-sm font-bold uppercase text-ink bg-yellow cursor-pointer"
          >
            {actionText}
          </button>
        </div>
      )}
    </div>
  );
};

export default EmptyState;
