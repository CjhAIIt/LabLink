export function downloadCsv(fileName, rows) {
  const csvContent = `\uFEFF${rows.map((row) => row.map(escapeCsvCell).join(',')).join('\r\n')}`
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  downloadBlob(fileName, blob)
}

export function downloadBlob(fileName, data, contentType = 'application/octet-stream') {
  const blob = data instanceof Blob ? data : new Blob([data], { type: contentType })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

function escapeCsvCell(value) {
  const normalized = value == null ? '' : String(value)
  if (/[",\r\n]/.test(normalized)) {
    return `"${normalized.replace(/"/g, '""')}"`
  }
  return normalized
}
