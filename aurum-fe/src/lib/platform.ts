export const isMac = typeof navigator !== "undefined" && /Mac|iPhone|iPad/.test(navigator.platform);

export const optionLabel = isMac ? "⌥" : "Alt";
export const commandLabel = isMac ? "⌘" : "Ctrl";
