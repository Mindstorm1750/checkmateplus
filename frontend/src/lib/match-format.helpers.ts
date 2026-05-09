export function formatDuration(ms: number): string {
  const totalSeconds = Math.floor(ms / 1000)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return minutes > 0 ? `${minutes}m ${seconds}s` : `${seconds}s`
}

export function formatDate(epochMs: number): string {
  return new Date(epochMs).toLocaleString("en-GB", {
    dateStyle: "medium",
    timeStyle: "short",
  })
}
