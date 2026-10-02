import React from 'react';
import { Construction } from 'lucide-react';
import { Link } from 'react-router-dom';

interface PlaceholderPageProps {
  title: string;
  description: string;
  phase: string;
}

export const PlaceholderPage: React.FC<PlaceholderPageProps> = ({ title, description, phase }) => {
  return (
    <div className="brut-box bg-paper p-8 text-center max-w-lg mx-auto my-12 border-3 border-ink text-ink flex flex-col items-center gap-4">
      <div className="w-14 h-14 bg-yellow text-ink border-3 border-ink flex items-center justify-center select-none">
        <Construction className="w-7 h-7" />
      </div>

      <h2 className="text-xl font-heading font-black uppercase text-ink">{title}</h2>

      <div className="inline-block border-2 border-ink bg-blue px-3 py-1 text-xs font-heading font-bold uppercase text-ink select-none">
        {phase}
      </div>

      <p className="text-sm font-body text-ink/80 leading-relaxed">{description}</p>

      <div className="mt-2">
        <Link
          to="/"
          className="brut-pop inline-flex items-center min-h-12 px-6 py-3 text-xs sm:text-sm font-heading font-bold uppercase bg-paper border-3 border-ink text-ink"
        >
          ← Quay lại Tổng quan
        </Link>
      </div>
    </div>
  );
};

export default PlaceholderPage;
