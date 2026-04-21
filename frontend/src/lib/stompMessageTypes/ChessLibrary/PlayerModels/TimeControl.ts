export type TimeControl =
  | "BULLET_1_0"
  | "BULLET_2_1"
  | "BLITZ_3_0"
  | "BLITZ_5_0"
  | "RAPID_10_0"
  | "RAPID_15_10"
  | "CLASSICAL_30_0"

export const TimeControlToStringMap: Map<TimeControl, string> = new Map([
  ["BULLET_1_0", "1+0"],
  ["BULLET_2_1", "2+1"],
  ["BLITZ_3_0", "3+0"],
  ["BLITZ_5_0", "5+0"],
  ["RAPID_10_0", "10+0"],
  ["RAPID_15_10", "15+10"],
  ["CLASSICAL_30_0", "30+0"],
])
