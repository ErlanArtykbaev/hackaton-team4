import { useCallback, useRef, useState } from 'react'

/**
 * A short beep through Web Audio, no sound files. Browsers allow audio only after a tap,
 * so the cook turns it on once with a button; `play(true)` is the double, higher alarm.
 */
export function useChime() {
  const context = useRef<AudioContext | null>(null)
  const [enabled, setEnabled] = useState(false)

  const toggle = useCallback(() => {
    if (!context.current) context.current = new AudioContext()
    void context.current.resume()
    setEnabled((on) => !on)
  }, [])

  const play = useCallback(
    (alarm: boolean) => {
      const audio = context.current
      if (!enabled || !audio) return
      const beeps = alarm ? [0, 0.25] : [0]
      for (const start of beeps) {
        const oscillator = audio.createOscillator()
        const gain = audio.createGain()
        oscillator.frequency.value = alarm ? 1046 : 784
        gain.gain.setValueAtTime(0.2, audio.currentTime + start)
        gain.gain.exponentialRampToValueAtTime(0.001, audio.currentTime + start + 0.2)
        oscillator.connect(gain).connect(audio.destination)
        oscillator.start(audio.currentTime + start)
        oscillator.stop(audio.currentTime + start + 0.2)
      }
    },
    [enabled],
  )

  return { enabled, toggle, play }
}
