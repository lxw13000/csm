// 用户端收到客服/机器人新消息时的轻提示音

let audioCtx: AudioContext | null = null

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
 * 提示音：连续播放 3 声短促提示音，比单声更易察觉。
 * 用 AudioContext 音频时钟精确排程（不用 setTimeout，避免后台标签页被节流而丢音/错拍）；
 * 每声用增益包络做淡入淡出，避免爆音，峰值音量较此前明显加大。
 */
export function beep() {
  try {
    if (!audioCtx) unlockAudio()
    if (!audioCtx) return
    const beepCount = 3      // 连响次数
    const tone = 760         // 频率(Hz)
    const dur = 0.16         // 单声时长(秒)
    const gap = 0.10         // 声音间隔(秒)
    const peak = 0.25        // 峰值音量(0~1)
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
