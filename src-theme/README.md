# LiquidBounce Default Theme

This directory contains the source code of LiquidBounce's default theme built with [Svelte](https://svelte.dev/).

## Development

### Prerequisites
- [Node.js](https://nodejs.org/en) (latest or stable version)

### Setup

1. **Install dependencies**
   ```bash
   npm install
   ```

2. **Configure development mode**
    - Set `IN_DEV` to `true` in [host.ts](https://github.com/CCBlueX/LiquidBounce/blob/nextgen/src-theme/src/integration/host.ts)

3. **Launch the client**

4. **Start development server**
   ```bash
   npm run dev
   ```

5. **Set theme in client**
    - `.client theme set http://localhost:5173/`

**Important**: Do not commit changes made to `host.ts` and `NettyServer.kt` when pushing to the repository.

## Building for Production

To build the theme for production use, follow these steps:

1. **Ensure development mode is disabled**
    - Set `IN_DEV` to `false` in [host.ts](https://github.com/CCBlueX/LiquidBounce/blob/nextgen/src-theme/src/integration/host.ts)

2. **Build the theme**
   ```bash
   npm run build
   ```

3. **Locate build output**
    - The production build will be generated in the `dist` folder
    - This folder contains all the optimized and minified theme files

4. **Deploy to themes directory**
    - Copy the entire `dist` folder to your themes directory
    - You can open the themes directory by typing `.client theme browse` in the client
    - Rename the `dist` folder to your preferred theme name

5. **Apply the theme**
   ```
   .client theme set <your-theme-name>
   ```

## Marketplace Publishing

1. **Get API token**
    - Go to [https://liquidbounce.net/account](https://liquidbounce.net/account)
    - Generate your API token

2. **Get marketplace item ID**
    - Navigate to [https://liquidbounce.net/marketplace](https://liquidbounce.net/marketplace)
    - Go to your item page
    - Copy the ID from the "Item ID" field on the right side

3. **Configure repository**
    - Add `API_TOKEN` as a repository secret in Settings → Secrets and variables → Actions
    - Update `MARKETPLACE_ITEM_ID` and `ZIP_NAME` in the workflow file with your values

4. **Enable publishing**
    - Uncomment the GitHub release and marketplace upload sections in `.github/workflows/build.yml`
