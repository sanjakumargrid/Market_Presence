import api from './axios';

export type AnalyticEventType = 'CLICK' | 'APPLY_START';

/**
 * Fire-and-forget analytic event (REQ-JP-05).
 * Errors are swallowed — analytics must never interrupt the user flow.
 */
export const recordEvent = async (
  slug: string,
  eventType: AnalyticEventType,
  channel = 'CAREERS_PORTAL'
): Promise<void> => {
  try {
    await api.post(`/public/jobs/${slug}/events`, { eventType, channel });
  } catch {
    // intentional no-op
  }
};
