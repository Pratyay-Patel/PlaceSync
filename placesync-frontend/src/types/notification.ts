export type NotificationType =
  | 'APPLICATION_UPDATE'
  | 'INTERVIEW_SCHEDULED'
  | 'OFFER_RELEASED'
  | 'SYSTEM';

export interface Notification {
  id: string;
  type: NotificationType;
  title: string;
  body: string;
  referenceId?: string;
  referenceType?: string;
  isRead: boolean;
  readAt?: string;
  createdAt: string;
}
