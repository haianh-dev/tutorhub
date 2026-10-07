import { z } from 'zod';

const notBlank = (msg: string) =>
  z.string().min(1, msg).refine((v) => v.trim().length > 0, msg);

export const createClassSchema = z.object({
  name: notBlank('Tên lớp không được để trống').max(100, 'Tên lớp tối đa 100 ký tự'),
  subject: notBlank('Môn học không được để trống').max(100, 'Môn học tối đa 100 ký tự'),
  classType: z.enum(['ONE_ON_ONE', 'GROUP'], {
    error: 'Vui lòng chọn loại lớp học',
  }),
  description: z.string().max(2000, 'Mô tả tối đa 2000 ký tự').optional(),
});

export type CreateClassFormValues = z.infer<typeof createClassSchema>;

export const updateClassSchema = z.object({
  name: notBlank('Tên lớp không được để trống').max(100, 'Tên lớp tối đa 100 ký tự'),
  subject: notBlank('Môn học không được để trống').max(100, 'Môn học tối đa 100 ký tự'),
  classType: z.enum(['ONE_ON_ONE', 'GROUP'], {
    error: 'Vui lòng chọn loại lớp học',
  }),
  description: z.string().max(2000, 'Mô tả tối đa 2000 ký tự').optional(),
});

export type UpdateClassFormValues = z.infer<typeof updateClassSchema>;

export const enrollStudentSchema = z.object({
  studentId: z
    .number({ error: 'Mã học sinh phải là số nguyên' })
    .int('Mã học sinh phải là số nguyên')
    .positive('Mã học sinh phải lớn hơn 0'),
});

export type EnrollStudentFormValues = z.infer<typeof enrollStudentSchema>;

export const createClassInvitationSchema = z.object({
  role: z.enum(['STUDENT', 'PARENT']),
  email: z
    .string()
    .trim()
    .email('Email không đúng định dạng')
    .or(z.literal(''))
    .optional(),
  studentId: z.number().int().positive().optional(),
});

export type CreateClassInvitationFormValues = z.infer<
  typeof createClassInvitationSchema
>;
