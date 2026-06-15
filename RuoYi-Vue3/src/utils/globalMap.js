const LEAFLET_SCRIPT_ID = 'leaflet-js'
const LEAFLET_STYLE_ID = 'leaflet-css'
const LEAFLET_VERSION = '1.9.4'
const LEAFLET_ASSETS = [
  {
    css: `https://cdn.bootcdn.net/ajax/libs/leaflet/${LEAFLET_VERSION}/leaflet.css`,
    js: `https://cdn.bootcdn.net/ajax/libs/leaflet/${LEAFLET_VERSION}/leaflet.js`
  },
  {
    css: `https://cdnjs.cloudflare.com/ajax/libs/leaflet/${LEAFLET_VERSION}/leaflet.css`,
    js: `https://cdnjs.cloudflare.com/ajax/libs/leaflet/${LEAFLET_VERSION}/leaflet.js`
  },
  {
    css: `https://cdn.jsdelivr.net/npm/leaflet@${LEAFLET_VERSION}/dist/leaflet.css`,
    js: `https://cdn.jsdelivr.net/npm/leaflet@${LEAFLET_VERSION}/dist/leaflet.js`
  },
  {
    css: `https://unpkg.com/leaflet@${LEAFLET_VERSION}/dist/leaflet.css`,
    js: `https://unpkg.com/leaflet@${LEAFLET_VERSION}/dist/leaflet.js`
  }
]

let leafletLoadPromise

function loadLeafletStyle(href) {
  if (document.getElementById(LEAFLET_STYLE_ID)) {
    return
  }
  const link = document.createElement('link')
  link.id = LEAFLET_STYLE_ID
  link.rel = 'stylesheet'
  link.href = href
  document.head.appendChild(link)
}

function removeLeafletScript() {
  const script = document.getElementById(LEAFLET_SCRIPT_ID)
  if (script) {
    script.remove()
  }
}

function loadLeafletFrom(index) {
  const asset = LEAFLET_ASSETS[index]
  if (!asset) {
    leafletLoadPromise = null
    return Promise.reject(new Error('LEAFLET_LOAD_FAILED'))
  }

  loadLeafletStyle(asset.css)
  return new Promise((resolve, reject) => {
    removeLeafletScript()
    const script = document.createElement('script')
    script.id = LEAFLET_SCRIPT_ID
    script.async = true
    script.src = asset.js
    script.onload = () => {
      if (window.L) {
        resolve(window.L)
      } else {
        loadLeafletFrom(index + 1).then(resolve).catch(reject)
      }
    }
    script.onerror = () => {
      loadLeafletFrom(index + 1).then(resolve).catch(reject)
    }
    document.head.appendChild(script)
  })
}

export function loadLeaflet() {
  if (typeof window === 'undefined') {
    return Promise.reject(new Error('LEAFLET_BROWSER_ONLY'))
  }
  if (window.L) {
    return Promise.resolve(window.L)
  }
  if (leafletLoadPromise) {
    return leafletLoadPromise
  }

  leafletLoadPromise = loadLeafletFrom(0)

  return leafletLoadPromise
}

export function createGlobalLngLat(longitude, latitude) {
  if (longitude === undefined || latitude === undefined || longitude === null || latitude === null) {
    return null
  }
  return {
    lng: Number(longitude),
    lat: Number(latitude)
  }
}

export async function createGlobalPickerMap(container, options = {}) {
  const L = await loadLeaflet()
  const hasCoordinate = options.longitude && options.latitude
  const center = hasCoordinate
    ? [Number(options.latitude), Number(options.longitude)]
    : [20, 0]
  const map = L.map(container, {
    center,
    zoom: options.zoom || (hasCoordinate ? 15 : 2),
    zoomControl: true,
    worldCopyJump: true
  })
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; OpenStreetMap contributors'
  }).addTo(map)

  const marker = L.marker(center, { draggable: true }).addTo(map)
  window.setTimeout(() => map.invalidateSize(), 0)

  return { L, map, marker }
}
