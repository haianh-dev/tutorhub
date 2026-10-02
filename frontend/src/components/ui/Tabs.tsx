import React from 'react';

export interface TabItem {
  id: string;
  label: string;
  badge?: string | number;
}

export interface TabsProps {
  tabs: TabItem[];
  activeTab: string;
  onChange: (tabId: string) => void;
  className?: string;
}

export const Tabs: React.FC<TabsProps> = ({
  tabs,
  activeTab,
  onChange,
  className = '',
}) => {
  return (
    <div
      role="tablist"
      className={`inline-flex flex-wrap border-3 border-ink bg-paper select-none ${className}`}
    >
      {tabs.map((tab) => {
        const isActive = activeTab === tab.id;
        return (
          <button
            key={tab.id}
            role="tab"
            type="button"
            aria-selected={isActive}
            onClick={() => onChange(tab.id)}
            className={`min-h-12 px-5 py-3 font-heading text-xs sm:text-sm font-bold uppercase transition-colors flex items-center gap-2 border-r-3 border-b-3 last:border-r-0 border-ink cursor-pointer focus-visible:outline-3 focus-visible:outline-ink focus-visible:outline-offset-[-3px] ${
              isActive
                ? 'bg-yellow text-ink shadow-none'
                : 'bg-paper text-ink hover:bg-cream'
            }`}
          >
            <span>{tab.label}</span>
            {tab.badge !== undefined && (
              <span className="border-2 border-ink bg-paper px-1.5 py-0.2 text-[10px] font-bold">
                {tab.badge}
              </span>
            )}
          </button>
        );
      })}
    </div>
  );
};

export default Tabs;
