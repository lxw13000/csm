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

export function beep() {
  try {
    if (!audioCtx) unlockAudio()
    if (!audioCtx) return
    const osc = audioCtx.createOscillator()
    const gain = audioCtx.createGain()
    osc.connect(gain)
    gain.connect(audioCtx.destination)
    osc.frequency.value = 760
    gain.gain.value = 0.06
    osc.start()
    setTimeout(() => osc.stop(), 140)
  } catch {
    /* ignore */
  }
}
