// 日期范围工具：统一生成 YYYY-MM-DD 及「当天 / 本周」默认范围（供工作台、统计分析等使用）

/** 格式化为本地 YYYY-MM-DD。 */
export function fmtDate(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** 当天范围 [今天, 今天]。 */
export function todayRange(): [string, string] {
  const t = fmtDate(new Date())
  return [t, t]
}

/** 本周范围 [周一, 周日]（按周一为一周起始）。 */
export function weekRange(): [string, string] {
  const now = new Date()
  const diffToMon = (now.getDay() + 6) % 7 // 距本周一的天数（getDay: 0=周日）
  const mon = new Date(now)
  mon.setDate(now.getDate() - diffToMon)
  const sun = new Date(mon)
  sun.setDate(mon.getDate() + 6)
  return [fmtDate(mon), fmtDate(sun)]
}
