// 页面在线提醒：声音 + 浏览器通知 + 标题闪烁（呼应 xuqiu.md 4.2 / 4.4）

let audioCtx: AudioContext | null = null

/** 首次需在用户手势中调用以解锁移动端音频自动播放策略。 */
export function unlockAudio() {
  try {
    const Ctx = window.AudioContext || (window as any).webkitAudioContext
    audioCtx = audioCtx || new Ctx()
    if (audioCtx.state === 'suspended') audioCtx.resume()
  } catch {
    /* ignore */
  }
}

/**
 * 提示音：连续播放 3 声短促「叮」，比单声更易察觉。
 * 用 AudioContext 音频时钟精确排程（不用 setTimeout，避免后台标签页被节流而丢音/错拍）；
 * 每声用增益包络做淡入淡出，避免爆音，峰值音量较此前明显加大。
 */
export function beep() {
  try {
    if (!audioCtx) unlockAudio()
    if (!audioCtx) return
    const beepCount = 3      // 连响次数
    const tone = 880         // 频率(Hz)
    const dur = 0.18         // 单声时长(秒)
    const gap = 0.10         // 声音间隔(秒)
    const peak = 0.3         // 峰值音量(0~1)
    const start0 = audioCtx.currentTime
    for (let i = 0; i < beepCount; i++) {
      const osc = audioCtx.createOscillator()
      const gain = audioCtx.createGain()
      osc.connect(gain)
      gain.connect(audioCtx.destination)
      osc.frequency.value = tone
      const t = start0 + i * (dur + gap)
      gain.gain.setValueAtTime(0.0001, t)
      gain.gain.exponentialRampToValueAtTime(peak, t + 0.02)
      gain.gain.exponentialRampToValueAtTime(0.0001, t + dur)
      osc.start(t)
      osc.stop(t + dur)
    }
  } catch {
    /* ignore */
  }
}

export function requestNotifyPermission() {
  if ('Notification' in window && Notification.permission === 'default') {
    Notification.requestPermission()
  }
}

export function browserNotify(title: string, body: string) {
  if ('Notification' in window && Notification.permission === 'granted') {
    try {
      new Notification(title, { body })
    } catch {
      /* ignore */
    }
  }
}

let flashTimer: number | undefined
const originalTitle = document.title

export function startTitleFlash(text: string) {
  if (flashTimer) return
  let on = false
  flashTimer = window.setInterval(() => {
    document.title = on ? originalTitle : text
    on = !on
  }, 800)
}

export function stopTitleFlash() {
  if (flashTimer) {
    clearInterval(flashTimer)
    flashTimer = undefined
    document.title = originalTitle
  }
}

/** 收到新工单/新消息时统一提醒。 */
export function notifyNew(title: string, body: string, sound: boolean) {
  if (sound) beep()
  browserNotify(title, body)
  if (document.hidden) startTitleFlash('【新消息】' + title)
}
