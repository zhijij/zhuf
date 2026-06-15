import { defineConfig, loadEnv } from 'vite'
import path from 'path'
import createVitePlugins from './vite/plugins'

const workspaceEnvDir = path.resolve(__dirname, '..')
const amapEnvKeys = ['VITE_AMAP_JS_API_KEY', 'VITE_AMAP_SECURITY_JS_CODE']

function manualChunks(id) {
  if (!id.includes('node_modules')) return
  if (id.includes('element-plus') || id.includes('@element-plus')) return 'vendor-element'
  if (id.includes('echarts')) return 'vendor-echarts'
  if (id.includes('quill') || id.includes('@vueup/vue-quill')) return 'vendor-editor'
  if (id.includes('vue') || id.includes('pinia') || id.includes('vue-router') || id.includes('@vueuse')) return 'vendor-vue'
  if (id.includes('axios') || id.includes('js-cookie') || id.includes('nprogress') || id.includes('file-saver')) return 'vendor-utils'
  return 'vendor'
}

const baseUrl = 'http://localhost:8080' // 后端接口

function pickNonEmpty(value) {
  return String(value || '').trim()
}

function loadMergedEnv(mode) {
  const rootEnv = loadEnv(mode, workspaceEnvDir, '')
  const appEnv = loadEnv(mode, process.cwd(), '')
  const env = { ...rootEnv, ...appEnv }

  amapEnvKeys.forEach(key => {
    const appValue = pickNonEmpty(appEnv[key])
    const rootValue = pickNonEmpty(rootEnv[key])
    env[key] = appValue || rootValue
    if (env[key]) {
      process.env[key] = env[key]
    }
  })

  return env
}

// https://vitejs.dev/config/
export default defineConfig(({ mode, command }) => {
  const env = loadMergedEnv(mode)
  const { VITE_APP_ENV } = env
  return {
    define: {
      'import.meta.env.VITE_AMAP_JS_API_KEY': JSON.stringify(env.VITE_AMAP_JS_API_KEY || ''),
      'import.meta.env.VITE_AMAP_SECURITY_JS_CODE': JSON.stringify(env.VITE_AMAP_SECURITY_JS_CODE || '')
    },
    // 部署生产环境和开发环境下的URL。
    // 默认情况下，vite 会假设你的应用是被部署在一个域名的根路径上
    // 例如 https://www.ruoyi.vip/。如果应用被部署在一个子路径上，你就需要用这个选项指定这个子路径。例如，如果你的应用被部署在 https://www.ruoyi.vip/admin/，则设置 baseUrl 为 /admin/。
    base: VITE_APP_ENV === 'production' ? '/' : '/',
    plugins: createVitePlugins(env, command === 'build'),
    resolve: {
      // https://cn.vitejs.dev/config/#resolve-alias
      alias: {
        // 设置路径
        '~': path.resolve(__dirname, './'),
        // 设置别名
        '@': path.resolve(__dirname, './src')
      },
      // https://cn.vitejs.dev/config/#resolve-extensions
      extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue']
    },
    // 打包配置
    build: {
      // https://vite.dev/config/build-options.html
      sourcemap: command === 'build' ? false : 'inline',
      outDir: 'dist',
      assetsDir: 'assets',
      chunkSizeWarningLimit: 2000,
      rollupOptions: {
        output: {
          manualChunks,
          chunkFileNames: 'static/js/[name]-[hash].js',
          entryFileNames: 'static/js/[name]-[hash].js',
          assetFileNames: 'static/[ext]/[name]-[hash].[ext]'
        }
      }
    },
    // vite 相关配置
    server: {
      port: 80,
      host: true,
      open: true,
      proxy: {
        // https://cn.vitejs.dev/config/#server-proxy
        '/dev-api': {
          target: baseUrl,
          changeOrigin: true,
          rewrite: (p) => p.replace(/^\/dev-api/, '')
        },
         // springdoc proxy
         '^/v3/api-docs/(.*)': {
          target: baseUrl,
          changeOrigin: true,
        }
      }
    },
    css: {
      postcss: {
        plugins: [
          {
            postcssPlugin: 'internal:charset-removal',
            AtRule: {
              charset: (atRule) => {
                if (atRule.name === 'charset') {
                  atRule.remove()
                }
              }
            }
          }
        ]
      }
    }
  }
})
