import React, { useState } from 'react';
import {
  Users,
  UserCheck,
  Share2,
  Edit2,
  Archive,
  AlertTriangle,
  ArrowRight,
  BookOpen,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { Card } from '../../../components/ui/Card';
import { Badge } from '../../../components/ui/Badge';
import { Button } from '../../../components/ui/Button';
import type { Classroom } from '../../../types';
import { EditClassModal } from './EditClassModal';
import { ClassInviteModal } from './ClassInviteModal';

interface ClassCardProps {
  classroom: Classroom;
  onArchive: (id: number) => void;
  isArchiving?: boolean;
}

export const ClassCard: React.FC<ClassCardProps> = ({
  classroom,
  onArchive,
  isArchiving = false,
}) => {
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [isInviteOpen, setIsInviteOpen] = useState(false);

  const isOneOnOne = classroom.classType === 'ONE_ON_ONE';
  const isArchived = classroom.status === 'ARCHIVED';
  const hasWarnings = classroom.warnings && classroom.warnings.length > 0;

  return (
    <>
      <Card
        className={`flex flex-col justify-between transition-transform hover:-translate-y-1 ${
          isArchived ? 'opacity-75 bg-ink/5' : ''
        }`}
      >
        <div className="space-y-4">
          {/* Header với tags */}
          <div className="flex items-start justify-between gap-2">
            <div className="flex flex-wrap items-center gap-1.5">
              <Badge
                variant={isOneOnOne ? 'yellow' : 'blue'}
                icon={
                  isOneOnOne ? (
                    <UserCheck className="w-3.5 h-3.5" />
                  ) : (
                    <Users className="w-3.5 h-3.5 text-white" />
                  )
                }
              >
                <span className={isOneOnOne ? 'text-ink' : 'text-white'}>
                  {isOneOnOne ? 'Lớp 1:1' : 'Lớp nhóm'}
                </span>
              </Badge>

              {isArchived ? (
                <Badge variant="coral">Đã lưu trữ</Badge>
              ) : (
                <Badge variant="paper">Đang hoạt động</Badge>
              )}
            </div>

            <span className="font-mono text-xs font-bold text-ink/60">
              #{classroom.id}
            </span>
          </div>

          {/* Tên lớp & Môn học */}
          <div>
            <Link
              to={`/classes/${classroom.id}`}
              className="group inline-flex items-center gap-1.5"
            >
              <h3 className="font-heading font-black text-lg sm:text-xl uppercase tracking-tight text-ink group-hover:underline decoration-2 underline-offset-4">
                {classroom.name}
              </h3>
              <ArrowRight className="w-4 h-4 text-ink opacity-0 group-hover:opacity-100 transition-opacity" />
            </Link>

            <div className="flex items-center gap-2 mt-1 text-xs font-heading font-bold text-ink/75 uppercase">
              <BookOpen className="w-3.5 h-3.5 text-ink" />
              <span>{classroom.subject}</span>
            </div>
          </div>

          {/* Mô tả */}
          {classroom.description && (
            <p className="font-body text-xs text-ink/80 line-clamp-2 leading-relaxed">
              {classroom.description}
            </p>
          )}

          {/* Sĩ số học sinh & Warnings */}
          <div className="space-y-2 pt-2 border-t-2 border-ink/20">
            <div className="flex items-center justify-between text-xs font-heading font-bold uppercase">
              <span className="text-ink/70">Sĩ số học sinh:</span>
              <span className="border-2 border-ink px-2 py-0.5 bg-paper text-ink font-mono">
                {classroom.studentCount ?? 0} {isOneOnOne ? '/ 1' : 'học sinh'}
              </span>
            </div>

            {hasWarnings && (
              <div className="flex items-start gap-1.5 p-2 bg-yellow/40 border-2 border-ink text-xs font-body text-ink">
                <AlertTriangle className="w-4 h-4 text-ink shrink-0 mt-0.5" />
                <span className="font-medium leading-tight">
                  {classroom.warnings![0]}
                </span>
              </div>
            )}
          </div>
        </div>

        {/* Action Buttons */}
        <div className="pt-4 mt-4 border-t-3 border-ink flex flex-wrap items-center justify-between gap-2">
          <Link to={`/classes/${classroom.id}`} className="grow sm:grow-0">
            <Button variant="primary" size="sm" className="w-full justify-center">
              Quản lý lớp
              <ArrowRight className="w-3.5 h-3.5 ml-1" />
            </Button>
          </Link>

          {!isArchived && (
            <div className="flex items-center gap-1.5">
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setIsInviteOpen(true)}
                title="Tạo link mời học sinh / phụ huynh"
              >
                <Share2 className="w-3.5 h-3.5" />
                Mời
              </Button>

              <Button
                variant="secondary"
                size="sm"
                onClick={() => setIsEditOpen(true)}
                title="Chỉnh sửa thông tin lớp"
              >
                <Edit2 className="w-3.5 h-3.5" />
              </Button>

              <Button
                variant="secondary"
                size="sm"
                onClick={() => {
                  if (
                    window.confirm(
                      `Bạn có chắc chắn muốn lưu trữ lớp "${classroom.name}"? Lớp sẽ không thể chỉnh sửa sau khi lưu trữ.`,
                    )
                  ) {
                    onArchive(classroom.id);
                  }
                }}
                isLoading={isArchiving}
                title="Lưu trữ lớp"
              >
                <Archive className="w-3.5 h-3.5 text-coral" />
              </Button>
            </div>
          )}
        </div>
      </Card>

      <EditClassModal
        isOpen={isEditOpen}
        onClose={() => setIsEditOpen(false)}
        classroom={classroom}
      />

      <ClassInviteModal
        isOpen={isInviteOpen}
        onClose={() => setIsInviteOpen(false)}
        classId={classroom.id}
        className={classroom.name}
      />
    </>
  );
};
