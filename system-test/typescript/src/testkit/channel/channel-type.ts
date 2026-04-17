export const ChannelType = {
    UI: 'ui',
    API: 'api',
} as const;

export type ChannelTypeValue = (typeof ChannelType)[keyof typeof ChannelType];
