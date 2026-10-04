import { z } from 'zod';

const notBlank = (msg: string) =>
  z.string().min(1, msg).refine((v) => v.trim().length > 0, msg);

const emailSchema = notBlank('Email không được để trống').email(
  'Email không đúng định dạng',
);

const passwordSchema = notBlank('Mật khẩu không được để trống').min(
  8,
  'Mật khẩu phải có ít nhất 8 ký tự',
);

const fullNameSchema = notBlank('Họ và tên không được để trống');

const phoneOptional = z.preprocess(
  (v: unknown): string | undefined =>
    typeof v === 'string' && v.trim().length > 0 ? v.trim() : undefined,
  z.string().optional(),
);

export const loginSchema = z.object({
  email: emailSchema,
  password: notBlank('Mật khẩu không được để trống'),
});

export type LoginFormValues = z.infer<typeof loginSchema>;

export const registerTutorSchema = z.object({
  email: emailSchema,
  password: passwordSchema,
  confirmPassword: notBlank('Xác nhận mật khẩu không được để trống'),
  fullName: fullNameSchema,
  phone: phoneOptional,
}).refine((v) => v.password === v.confirmPassword, {
  message: 'Mật khẩu và xác nhận mật khẩu không khớp',
  path: ['confirmPassword'],
});

export type RegisterTutorFormValues = z.infer<typeof registerTutorSchema>;

export const acceptInvitationSchema = z.object({
  token: notBlank('Token lời mời không hợp lệ'),
  password: passwordSchema,
  confirmPassword: notBlank('Xác nhận mật khẩu không được để trống'),
  fullName: fullNameSchema,
  phone: phoneOptional,
}).refine((v) => v.password === v.confirmPassword, {
  message: 'Mật khẩu và xác nhận mật khẩu không khớp',
  path: ['confirmPassword'],
});

export type AcceptInvitationFormValues = z.infer<typeof acceptInvitationSchema>;

export const resetPasswordSchema = z.object({
  token: notBlank('Token đặt lại mật khẩu không hợp lệ'),
  newPassword: passwordSchema,
  confirmPassword: notBlank('Xác nhận mật khẩu không được để trống'),
}).refine((v) => v.newPassword === v.confirmPassword, {
  message: 'Mật khẩu mới và xác nhận mật khẩu không khớp',
  path: ['confirmPassword'],
});

export type ResetPasswordFormValues = z.infer<typeof resetPasswordSchema>;
