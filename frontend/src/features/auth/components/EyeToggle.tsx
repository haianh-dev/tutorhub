import React from 'react';
import { Eye, EyeOff } from 'lucide-react';

export interface EyeToggleProps {
  shown: boolean;
  onToggle: () => void;
}

/**
 * Nút hiện/ẩn mật khẩu dùng chung cho các auth form.
 * Đặt ở ngoài function component để tránh "static-components" warning
 * (React Compiler không tối ưu được component tạo trong render).
 */
export const EyeToggle: React.FC<EyeToggleProps> = ({ shown, onToggle }) => (
  <button
    type="button"
    onClick={onToggle}
    aria-label={shown ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
    className="w-8 h-8 border-3 border-ink bg-paper flex items-center justify-center text-ink hover:bg-cream transition-colors cursor-pointer pointer-events-auto"
  >
    {shown ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
  </button>
);

export default EyeToggle;
