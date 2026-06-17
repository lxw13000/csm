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

export function beep() {
  try {
    if (!audioCtx) unlockAudio()
    if (!audioCtx) return
    const osc = audioCtx.createOscillator()
    const gain = audioCtx.createGain()
    osc.connect(gain)
    gain.connect(audioCtx.destination)
    osc.frequency.value = 880
    gain.gain.value = 0.08
    osc.start()
    setTimeout(() => osc.stop(), 160)
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
