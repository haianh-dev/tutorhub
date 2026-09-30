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
    <div className="bg-white rounded-xl border border-slate-200/80 p-8 text-center max-w-lg mx-auto shadow-sm my-12">
      <div className="w-14 h-14 bg-amber-50 text-amber-600 rounded-2xl flex items-center justify-center mx-auto mb-4 border border-amber-100">
        <Construction className="w-7 h-7" />
      </div>
      <h2 className="text-xl font-bold text-slate-800">{title}</h2>
      <div className="inline-block mt-2 px-2.5 py-1 text-xs font-semibold rounded-full bg-slate-100 text-slate-600">
        {phase}
      </div>
      <p className="text-sm text-slate-500 mt-3">{description}</p>
      <div className="mt-6">
        <Link
          to="/"
          className="inline-flex items-center px-4 py-2 text-sm font-medium rounded-lg bg-indigo-50 text-indigo-700 hover:bg-indigo-100 transition-colors"
        >
          ← Quay lại Tổng quan
        </Link>
      </div>
    </div>
  );
};

export default PlaceholderPage;
