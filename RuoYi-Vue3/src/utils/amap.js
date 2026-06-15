const AMAP_SCRIPT_ID = 'amap-js-sdk'
const AMAP_PLUGIN_TIMEOUT = 8000
const AMAP_GEOLOCATION_TIMEOUT = 10000
const AMAP_PICKER_CONTROL_PLUGINS = [
  'AMap.ToolBar',
  'AMap.Scale',
  'AMap.ControlBar',
  'AMap.Geolocation',
  'AMap.MapType'
]

let amapLoadPromise

const AMAP_JS_API_KEY = String(import.meta.env.VITE_AMAP_JS_API_KEY || '').trim()
const AMAP_SECURITY_JS_CODE = String(import.meta.env.VITE_AMAP_SECURITY_JS_CODE || '').trim()

function amapKey() {
  return AMAP_JS_API_KEY
}

function amapSecurityJsCode() {
  return AMAP_SECURITY_JS_CODE
}

export function isAmapJsConfigured() {
  return Boolean(amapKey())
}

export function createAmapLngLat(longitude, latitude) {
  if (!window.AMap || longitude === undefined || latitude === undefined || longitude === null || latitude === null) {
    return null
  }
  return new window.AMap.LngLat(Number(longitude), Number(latitude))
}

export function loadAmapJs() {
  if (typeof window === 'undefined') {
    return Promise.reject(new Error('AMAP_JS_BROWSER_ONLY'))
  }
  if (window.AMap) {
    return Promise.resolve(window.AMap)
  }
  if (amapLoadPromise) {
    return amapLoadPromise
  }

  const key = amapKey()
  if (!key) {
    return Promise.reject(new Error('AMAP_JS_KEY_MISSING'))
  }

  const securityJsCode = amapSecurityJsCode()
  if (securityJsCode) {
    window._AMapSecurityConfig = {
      ...(window._AMapSecurityConfig || {}),
      securityJsCode
    }
  }

  amapLoadPromise = new Promise((resolve, reject) => {
    const existed = document.getElementById(AMAP_SCRIPT_ID)
    if (existed) {
      existed.addEventListener('load', () => resolve(window.AMap), { once: true })
      existed.addEventListener('error', () => reject(new Error('AMAP_JS_LOAD_FAILED')), { once: true })
      return
    }

    const script = document.createElement('script')
    const params = new URLSearchParams({
      v: '2.0',
      key
    })
    script.id = AMAP_SCRIPT_ID
    script.async = true
    script.src = `https://webapi.amap.com/maps?${params.toString()}`
    script.onload = () => {
      if (window.AMap) {
        resolve(window.AMap)
      } else {
        reject(new Error('AMAP_JS_LOAD_FAILED'))
      }
    }
    script.onerror = () => {
      amapLoadPromise = null
      reject(new Error('AMAP_JS_LOAD_FAILED'))
    }
    document.head.appendChild(script)
  })

  return amapLoadPromise
}

async function loadAmapPlugins(pluginNames) {
  const AMap = await loadAmapJs()
  const names = Array.isArray(pluginNames) ? pluginNames : [pluginNames]
  await new Promise((resolve, reject) => {
    const timer = window.setTimeout(() => reject(new Error('AMAP_PLUGIN_TIMEOUT')), AMAP_PLUGIN_TIMEOUT)
    AMap.plugin(names, () => {
      window.clearTimeout(timer)
      resolve()
    })
  })
  return AMap
}

async function addAmapPickerControls(map, options = {}) {
  const AMap = await loadAmapPlugins(AMAP_PICKER_CONTROL_PLUGINS)
  const controls = []

  if (AMap.ToolBar) {
    controls.push(new AMap.ToolBar({
      position: { top: '12px', left: '12px' }
    }))
  }
  if (AMap.Scale) {
    controls.push(new AMap.Scale())
  }
  if (AMap.ControlBar) {
    controls.push(new AMap.ControlBar({
      position: { top: '12px', right: '12px' },
      showZoomBar: false,
      showControlButton: true
    }))
  }
  if (AMap.MapType) {
    controls.push(new AMap.MapType({
      position: { top: '54px', right: '12px' }
    }))
  }
  if (AMap.Geolocation) {
    controls.push(new AMap.Geolocation({
      enableHighAccuracy: true,
      timeout: AMAP_GEOLOCATION_TIMEOUT,
      zoomToAccuracy: true,
      position: 'RB',
      offset: [12, 72]
    }))
  }

  controls.forEach(control => map.addControl(control))
  if (options.geolocationOnMap && controls.at(-1)?.getCurrentPosition) {
    controls.at(-1).getCurrentPosition()
  }
  return controls
}

export async function geocodeAddressByAmapJs({ address, city }) {
  const fullAddress = String(address || '').trim()
  if (!fullAddress) {
    throw new Error('AMAP_GEOCODE_ADDRESS_EMPTY')
  }

  const AMap = await loadAmapPlugins(['AMap.Geocoder'])
  const geocoder = new AMap.Geocoder({
    city: String(city || '').trim() || '全国'
  })

  return new Promise((resolve, reject) => {
    geocoder.getLocation(fullAddress, (status, result) => {
      const geocode = result?.geocodes?.[0]
      const location = geocode?.location
      if (status !== 'complete' || result?.info !== 'OK' || !location) {
        const error = new Error(result?.info || 'AMAP_GEOCODE_FAILED')
        error.result = result
        reject(error)
        return
      }

      resolve({
        success: true,
        longitude: String(location.lng),
        latitude: String(location.lat),
        address: geocode.formattedAddress || fullAddress,
        city: geocode.addressComponent?.city || city || '',
        district: geocode.addressComponent?.district || '',
        adcode: geocode.adcode || ''
      })
    })
  })
}

export async function reverseGeocodeByAmapJs({ longitude, latitude, city }) {
  if (longitude === undefined || latitude === undefined || longitude === null || latitude === null) {
    throw new Error('AMAP_REGEOCODE_LOCATION_EMPTY')
  }

  const AMap = await loadAmapPlugins(['AMap.Geocoder'])
  const geocoder = new AMap.Geocoder({
    city: String(city || '').trim() || '全国'
  })
  const lnglat = [Number(longitude), Number(latitude)]

  return new Promise((resolve, reject) => {
    geocoder.getAddress(lnglat, (status, result) => {
      const regeocode = result?.regeocode
      if (status !== 'complete' || result?.info !== 'OK' || !regeocode) {
        const error = new Error(result?.info || 'AMAP_REGEOCODE_FAILED')
        error.result = result
        reject(error)
        return
      }

      const component = regeocode.addressComponent || {}
      resolve({
        success: true,
        longitude: String(longitude),
        latitude: String(latitude),
        address: regeocode.formattedAddress || '',
        city: Array.isArray(component.city) ? component.province : component.city || component.province || '',
        district: component.district || '',
        township: component.township || '',
        street: component.streetNumber?.street || '',
        streetNumber: component.streetNumber?.number || '',
        adcode: component.adcode || ''
      })
    })
  })
}

export async function getCurrentPositionByAmapJs() {
  const AMap = await loadAmapPlugins(['AMap.Geolocation'])
  const geolocation = new AMap.Geolocation({
    enableHighAccuracy: true,
    timeout: AMAP_GEOLOCATION_TIMEOUT,
    zoomToAccuracy: true,
    position: 'RB'
  })

  return new Promise((resolve, reject) => {
    geolocation.getCurrentPosition((status, result) => {
      const position = result?.position
      if (status !== 'complete' || !position) {
        const error = new Error(result?.message || result?.info || 'AMAP_GEOLOCATION_FAILED')
        error.result = result
        reject(error)
        return
      }

      resolve({
        success: true,
        longitude: String(position.lng),
        latitude: String(position.lat),
        accuracy: result.accuracy,
        address: result.formattedAddress || '',
        city: result.addressComponent?.city || result.addressComponent?.province || '',
        district: result.addressComponent?.district || '',
        raw: result
      })
    })
  })
}

export async function createAmapPickerMap(container, options = {}) {
  const AMap = await loadAmapJs()
  const center = options.longitude && options.latitude
    ? [Number(options.longitude), Number(options.latitude)]
    : [116.397428, 39.90923]
  const map = new AMap.Map(container, {
    zoom: options.zoom || 15,
    center,
    resizeEnable: true,
    viewMode: '2D'
  })
  const marker = new AMap.Marker({
    position: center,
    draggable: true,
    cursor: 'move'
  })
  map.add(marker)
  const controls = await addAmapPickerControls(map, options)

  return { AMap, map, marker, controls }
}
