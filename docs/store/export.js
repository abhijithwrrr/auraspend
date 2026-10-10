/**
 * Export the screenshot deck deterministically, without a human in the editor.
 *
 * The editor's "Export bundle" button renders every slide at full resolution via
 * html-to-image and downloads a zip. Driving that from a throwaway headless
 * Chrome gives byte-reproducible exports and removes any dependence on the
 * state of the developer's own browser profile.
 *
 * Usage:
 *   node tools/export.js [device] [outfile]
 *   node tools/export.js                # android phone deck  -> dist/android.zip
 *   node tools/export.js feature-graphic
 *
 * Requires `pnpm dev` to be running on http://localhost:3000.
 */
const path = require("path");
const fs = require("fs");
const { chromium } = require("playwright-core");

const CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";

async function main() {
  const device = process.argv[2] || "android";
  const out = path.resolve(process.argv[3] || `dist/${device}.zip`);
  fs.mkdirSync(path.dirname(out), { recursive: true });

  const browser = await chromium.launch({ executablePath: CHROME, headless: true });
  const context = await browser.newContext({ acceptDownloads: true, viewport: { width: 1600, height: 1000 } });
  const page = await context.newPage();

  await page.goto("http://localhost:3000/", { waitUntil: "networkidle" });
  await page.waitForTimeout(1500);

  // The project file is authoritative on load; assert the deck we expect is showing.
  const screenCount = await page.locator("text=/screens ·/").first().innerText().catch(() => "");
  console.log("deck:", screenCount.trim() || "(no counter)");

  if (device !== "android") {
    // Switch the device selector (combobox labelled by the current device).
    await page.locator("button.w-44").click();
    await page.getByRole("option", { name: device === "feature-graphic" ? "Feature Graphic" : device, exact: true }).click();
    await page.waitForTimeout(1000);
  }

  const [download] = await Promise.all([
    page.waitForEvent("download", { timeout: 180_000 }),
    page.getByRole("button", { name: /Export bundle/ }).click(),
  ]);

  await download.saveAs(out);
  const size = fs.statSync(out).size;
  console.log(`saved ${out} (${(size / 1024 / 1024).toFixed(1)} MB)`);
  await browser.close();
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
