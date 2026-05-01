import { useEffect, useState, useRef } from "react"
import type { Board, VoiceMove } from "@/types/Chess.type.ts"

export type VoiceMoveButtonProps = {
  matchId: String
  moveCallback: (voiceMove: VoiceMove) => void
  boardState: Board
  whitePerspective?: Boolean
  isInteractable: Boolean
}

export const VoiceMoveButton = ({
  matchId,
  moveCallback,
  isInteractable,
}: VoiceMoveButtonProps) => {
  const [isRecording, setIsRecording] = useState(false)
  const [toastMessage, setToastMessage] = useState<string | null>(null)
  const [isToastVisible, setIsToastVisible] = useState(false)
  const mediaRecorderRef = useRef<MediaRecorder | null>(null)
  const streamRef = useRef<MediaStream | null>(null)
  const audioChunksRef = useRef<BlobPart[]>([])

  useEffect(() => {
    if (!toastMessage) return

    setIsToastVisible(true)

    const fadeTimer = window.setTimeout(() => {
      setIsToastVisible(false)
    }, 4500)

    const removeTimer = window.setTimeout(() => {
      setToastMessage(null)
    }, 5000)

    return () => {
      window.clearTimeout(fadeTimer)
      window.clearTimeout(removeTimer)
    }
  }, [toastMessage])

  const showToast = (message: string) => {
    setToastMessage(message)
    setIsToastVisible(true)
  }

  const startRecording = async () => {
    if (!isInteractable || isRecording) return

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      streamRef.current = stream

      mediaRecorderRef.current = new MediaRecorder(stream, {
        mimeType: "audio/webm",
      })

      mediaRecorderRef.current.ondataavailable = (event: BlobEvent) => {
        if (event.data.size > 0) audioChunksRef.current.push(event.data)
      }

      mediaRecorderRef.current.onstop = async () => {
        try {
          const audioBlob = new Blob(audioChunksRef.current, {
            type: "audio/webm",
          })
          audioChunksRef.current = []

          const picked = await sendAudioToBackend(audioBlob)

          if (picked.error) {
            console.error("Voice move failed:", picked.error)
            showToast(picked.error)
            return
          }

          moveCallback({
            from: picked.from,
            to: picked.to,
            promotion: picked.promotion ?? null,
          } as VoiceMove)
        } catch (error) {
          console.error("Voice move failed:", error)
          showToast(
            error instanceof Error
              ? error.message
              : "Voice move failed. Please try again."
          )
        } finally {
          streamRef.current?.getTracks().forEach((t) => t.stop())
          streamRef.current = null
        }
      }

      mediaRecorderRef.current.start()
      setIsRecording(true)
    } catch (error) {
      console.error("Could not start recording:", error)
      showToast("Microphone access failed. Please check browser permissions.")
    }
  }

  const stopRecording = () => {
    if (!isRecording) return
    mediaRecorderRef.current?.stop()
    setIsRecording(false)
  }

  const sendAudioToBackend = async (blob: Blob) => {
    const formData = new FormData()
    formData.append("audio", blob, "move.webm")
    formData.append("matchId", String(matchId))

    const res = await fetch("http://localhost:9100/api/voice/move", {
      method: "POST",
      body: formData,
    })

    if (!res.ok) {
      const text = await res.text().catch(() => "")
      throw new Error(`Voice move failed: ${res.status} ${text}`)
    }

    return res.json() as Promise<{
      from: string
      to: string
      promotion?: string | null
      transcript?: string
      error?: string
    }>
  }

  return (
    <div className="relative inline-block">
      <button
        disabled={!isInteractable}
        onMouseDown={startRecording}
        onMouseUp={stopRecording}
        onMouseLeave={() => isRecording && stopRecording()}
        className={[
          "inline-flex items-center justify-center",
          "rounded px-4 py-2",
          "text-sm font-medium text-white",
          "transition-colors",
          "min-w-[180px]",
          !isInteractable
            ? "cursor-not-allowed bg-gray-500 opacity-60"
            : isRecording
              ? "bg-red-500"
              : "bg-blue-500",
        ].join(" ")}
        type="button"
      >
        {!isInteractable
          ? "Not your turn"
          : isRecording
            ? "Listening…"
            : "Hold to Speak Move"}
      </button>

      {toastMessage && (
        <div
          className={[
            "absolute top-full z-50 mt-2 w-full rounded-md",
            "bg-red-600 px-3 py-2 text-center text-sm text-white shadow-lg",
            "transition-all duration-500",
            isToastVisible
              ? "translate-y-0 opacity-100"
              : "translate-y-2 opacity-0",
          ].join(" ")}
          role="alert"
          aria-live="polite"
        >
          {toastMessage}
        </div>
      )}
    </div>
  )
}
