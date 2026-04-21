export const ChannelType = {
    UI: 'UI',
    API: 'API',
} as const;

export type ChannelTypeValue = (typeof ChannelType)[keyof typeof ChannelType];
