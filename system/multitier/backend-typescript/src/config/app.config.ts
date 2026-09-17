export const envOrDefault = (name: string, defaultValue: string): string => {
  const value = process.env[name];
  if (value) return value;
  return defaultValue;
};
