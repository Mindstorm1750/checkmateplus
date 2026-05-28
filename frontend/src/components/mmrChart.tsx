import { Area, AreaChart, CartesianGrid, XAxis, YAxis } from "recharts"

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import {
  type ChartConfig,
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
} from "@/components/ui/chart"

const placeholderData = [
  { date: "Jan 02", mmr: 1200 },
  { date: "Jan 09", mmr: 1235 },
  { date: "Jan 16", mmr: 1218 },
  { date: "Jan 23", mmr: 1264 },
  { date: "Jan 30", mmr: 1290 },
  { date: "Feb 06", mmr: 1272 },
  { date: "Feb 13", mmr: 1310 },
  { date: "Feb 20", mmr: 1355 },
  { date: "Feb 27", mmr: 1340 },
  { date: "Mar 06", mmr: 1388 },
  { date: "Mar 13", mmr: 1420 },
  { date: "Mar 20", mmr: 1402 },
  { date: "Mar 27", mmr: 1455 },
  { date: "Apr 03", mmr: 1490 },
  { date: "Apr 10", mmr: 1472 },
  { date: "Apr 17", mmr: 1518 },
  { date: "Apr 24", mmr: 1545 },
  { date: "May 01", mmr: 1580 },
]

const chartConfig = {
  mmr: {
    label: "Rating",
    color: "var(--color-brand-accent)",
  },
} satisfies ChartConfig

export function MmrChart() {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Rating over time</CardTitle>
        <CardDescription>Last 18 weeks</CardDescription>
      </CardHeader>
      <CardContent>
        <ChartContainer config={chartConfig} className="h-65 w-full">
          <AreaChart
            data={placeholderData}
            margin={{ left: 8, right: 8, top: 8, bottom: 0 }}
          >
            <defs>
              <linearGradient id="mmrFill" x1="0" y1="0" x2="0" y2="1">
                <stop
                  offset="0%"
                  stopColor="var(--color-mmr)"
                  stopOpacity={0.45}
                />
                <stop
                  offset="100%"
                  stopColor="var(--color-mmr)"
                  stopOpacity={0.02}
                />
              </linearGradient>
            </defs>
            <CartesianGrid vertical={false} strokeDasharray="3 3" />
            <XAxis
              dataKey="date"
              tickLine={false}
              axisLine={false}
              tickMargin={8}
              minTickGap={24}
            />
            <YAxis
              domain={["dataMin - 30", "dataMax + 30"]}
              tickLine={false}
              axisLine={false}
              tickMargin={8}
              width={36}
            />
            <ChartTooltip
              cursor={false}
              content={<ChartTooltipContent indicator="line" />}
            />
            <Area
              type="monotone"
              dataKey="mmr"
              stroke="var(--color-mmr)"
              strokeWidth={2}
              fill="url(#mmrFill)"
              isAnimationActive
              animationBegin={150}
              animationDuration={1400}
              animationEasing="ease-out"
              activeDot={{ r: 4 }}
            />
          </AreaChart>
        </ChartContainer>
      </CardContent>
    </Card>
  )
}
