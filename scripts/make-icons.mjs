/**
 * Builds the application icons from the one drawing we already have.
 *
 * The clef in `frontend/public/favicon.svg` is the browser tab icon; this renders it at the
 * sizes each operating system asks for. Run it when the icon changes, and commit what it
 * produces: the results are in the repository because the machines that build the installers
 * have no SVG rasteriser, and asking three of them to grow one to redraw an unchanged icon
 * is a worse trade than a few files.
 *
 *     node scripts/make-icons.mjs
 */
import { chromium } from '../frontend/node_modules/@playwright/test/index.mjs'
import { execFileSync } from 'node:child_process'
import { mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs'

const ROOT = new URL('..', import.meta.url).pathname
const SVG = readFileSync(`${ROOT}frontend/public/favicon.svg`, 'utf8')
const OUT = `${ROOT}packaging/icons`
const SIZES = [16, 32, 48, 64, 128, 256, 512, 1024]

mkdirSync(OUT, { recursive: true })
const browser = await chromium.launch()
const page = await browser.newPage()

const png = async (size) => {
  await page.setViewportSize({ width: size, height: size })
  await page.setContent(
    `<style>html,body{margin:0;padding:0}svg{display:block;width:${size}px;height:${size}px}</style>${SVG}`,
  )
  return page.locator('svg').screenshot({ omitBackground: true })
}

const rendered = new Map()
for (const size of SIZES) {
  rendered.set(size, await png(size))
  console.log(`  rendered ${size}px`)
}
await browser.close()

// Linux takes a plain PNG.
writeFileSync(`${OUT}/leading-tone.png`, rendered.get(512))
console.log('  packaging/icons/leading-tone.png')

// Windows takes an .ico. Since Vista it may hold PNGs directly, so it is a small header
// and the images we already have rather than a bitmap encoder.
const inIco = [16, 32, 48, 64, 128, 256]
const header = Buffer.alloc(6 + inIco.length * 16)
header.writeUInt16LE(0, 0)
header.writeUInt16LE(1, 2)
header.writeUInt16LE(inIco.length, 4)
let offset = header.length
const images = []
inIco.forEach((size, i) => {
  const data = rendered.get(size)
  const at = 6 + i * 16
  header.writeUInt8(size >= 256 ? 0 : size, at)
  header.writeUInt8(size >= 256 ? 0 : size, at + 1)
  header.writeUInt8(0, at + 2)
  header.writeUInt8(0, at + 3)
  header.writeUInt16LE(1, at + 4)
  header.writeUInt16LE(32, at + 6)
  header.writeUInt32LE(data.length, at + 8)
  header.writeUInt32LE(offset, at + 12)
  offset += data.length
  images.push(data)
})
writeFileSync(`${OUT}/leading-tone.ico`, Buffer.concat([header, ...images]))
console.log('  packaging/icons/leading-tone.ico')

// macOS takes an .icns, which iconutil builds from a folder of named sizes.
if (process.platform === 'darwin') {
  const set = `${OUT}/leading-tone.iconset`
  rmSync(set, { recursive: true, force: true })
  mkdirSync(set)
  for (const size of [16, 32, 128, 256, 512]) {
    writeFileSync(`${set}/icon_${size}x${size}.png`, rendered.get(size))
    const retina = rendered.get(size * 2)
    if (retina) {
      writeFileSync(`${set}/icon_${size}x${size}@2x.png`, retina)
    }
  }
  execFileSync('iconutil', ['-c', 'icns', set, '-o', `${OUT}/leading-tone.icns`])
  rmSync(set, { recursive: true, force: true })
  console.log('  packaging/icons/leading-tone.icns')
} else {
  console.log('  (.icns needs macOS; the committed one is still current)')
}
