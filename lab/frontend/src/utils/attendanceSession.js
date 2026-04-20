export function createSessionCountdown(session) {
  let timer = null

  const stop = () => {
    if (timer) {
      window.clearInterval(timer)
      timer = null
    }
  }

  const sync = () => {
    if (!session?.expireTime || session?.status !== 'active') {
      return
    }

    const expireTime = new Date(session.expireTime).getTime()
    if (Number.isNaN(expireTime)) {
      session.remainingSeconds = 0
      session.status = 'expired'
      stop()
      return
    }

    const diffSeconds = Math.max(Math.ceil((expireTime - Date.now()) / 1000), 0)
    session.remainingSeconds = diffSeconds
    if (diffSeconds <= 0) {
      session.remainingSeconds = 0
      session.status = 'expired'
      stop()
    }
  }

  const restart = () => {
    stop()
    if (!session?.expireTime || session?.status !== 'active') {
      return
    }
    sync()
    if (session.status === 'active' && session.remainingSeconds > 0) {
      timer = window.setInterval(sync, 1000)
    }
  }

  return {
    restart,
    stop,
    sync
  }
}
