# AuraSpend Charting System

AuraSpend uses **Canvas-based custom drawing** for all charts — no third-party charting libraries are used.

## Charts

### 1. Weekly Bar Chart (Dashboard)

**File**: `ui/home/DashboardScreen.kt`

- Shows spending for the current week (Mon-Sun)
- Each day is a vertical bar proportional to that day's total expense
- Color-coded: bars use the Material 3 primary color
- Axes: Y-axis auto-scales to max daily spend, X-axis shows day labels
- Canvas draw: `drawRect` for bars, `drawText` via `drawIntoCanvas` + `Paint`

### 2. Donut Pie Chart (Analytics)

**File**: `ui/analytics/AnalyticsScreen.kt`

- Category spending breakdown as a donut/ring chart
- Each slice uses the category's assigned color
- Center shows total spending
- Legend below lists categories with color indicator, name, and percentage
- Canvas draw: `drawArc` for slices,`drawCircle` for center hole

### 3. Budget Progress Bars

**File**: `ui/budget/BudgetScreen.kt`

- Horizontal progress bar per budget category
- Color coding:
  - Green: <75% of limit used
  - Orange: 75-90% of limit used
  - Red: >90% of limit used
- Uses standard `LinearProgressIndicator` from Material 3 (not Canvas)

## Canvas Pattern

All Canvas charts follow this structure:

```kotlin
@Composable
fun PieChart(data: List<CategorySpend>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val total = data.sumOf { it.amount }
        var startAngle = -90f
        data.forEach { item ->
            val sweepAngle = (item.amount / total * 360).toFloat()
            drawArc(
                color = item.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = donutWidth)
            )
            startAngle += sweepAngle
        }
    }
}
```
