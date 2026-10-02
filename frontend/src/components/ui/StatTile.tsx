import React from 'react';

export interface StatItem {
  value: string | number;
  label: string;
  variant?: 'paper' | 'blue' | 'yellow' | 'coral';
  subtext?: string;
}

export interface StatTileGroupProps {
  stats: StatItem[];
  className?: string;
}

const bgVariants = {
  paper: 'bg-paper',
  blue: 'bg-blue',
  yellow: 'bg-yellow',
  coral: 'bg-coral',
} as const;

export const StatTileGroup: React.FC<StatTileGroupProps> = ({ stats, className = '' }) => {
  return (
    <div
      className={`brut-box grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 bg-ink gap-[3px] border-3 border-ink select-none ${className}`}
    >
      {stats.map((stat, idx) => {
        // Fallback cycling variant if not provided: paper -> blue -> paper -> yellow
        const defaultVariants: Array<'paper' | 'blue' | 'yellow'> = ['paper', 'blue', 'paper', 'yellow'];
        const chosenVariant = stat.variant ?? defaultVariants[idx % defaultVariants.length];

        return (
          <div
            key={stat.label}
            className={`${bgVariants[chosenVariant]} p-6 flex flex-col justify-between gap-3 text-ink min-h-28`}
          >
            <div className="font-heading font-black text-3xl sm:text-4xl tracking-tight [font-variant-numeric:tabular-nums]">
              {stat.value}
            </div>
            <div>
              <div className="font-heading font-bold text-xs uppercase tracking-wide text-ink">
                {stat.label}
              </div>
              {stat.subtext && (
                <div className="font-body text-xs text-ink/75 mt-1 font-medium">
                  {stat.subtext}
                </div>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
};

export default StatTileGroup;
