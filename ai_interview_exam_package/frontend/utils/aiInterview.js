function getFallbackModule(fallbackModules, index) {
  if (!Array.isArray(fallbackModules) || fallbackModules.length === 0) {
    return {}
  }

  return fallbackModules[index] || fallbackModules[index % fallbackModules.length] || {}
}

export function normalizeInterviewModules(modules, fallbackModules = []) {
  if (!Array.isArray(modules) || modules.length === 0) {
    return Array.isArray(fallbackModules) ? fallbackModules.slice() : []
  }

  return modules.map((module, index) => {
    const fallback = getFallbackModule(fallbackModules, index)
    const name = module?.name || module?.moduleName || fallback.name || fallback.moduleName || ''

    return {
      ...fallback,
      ...module,
      name,
      moduleName: module?.moduleName || name,
      description: module?.description || fallback.description || '',
      icon: module?.icon || fallback.icon || '',
      color: module?.color || fallback.color || '',
    }
  })
}
