# LiquidBounce Default Theme

This repository contains the source code for the LiquidBounce Default Theme, built using [Svelte](https://svelte.dev/). 
It is intended for development, customization, and distribution through the LiquidBounce theme system and marketplace.

---

## Getting Started with a Custom Theme

If you want to create your own LiquidBounce theme, the recommended starting point is to **fork the official default theme repository**:

https://github.com/CCBlueX/LiquidBounce-Theme

Forking this repository gives you a fully working baseline with proper project structure, build configuration, and marketplace integration already set up. From there, you can modify the UI, styles, and components to create your own custom theme while staying compatible with LiquidBounce’s theming system.

---

## Development

### Prerequisites

Ensure the following software is installed before proceeding:

- Node.js (latest LTS or stable release recommended)

### Setup

1. **Install dependencies**

    ```bash
    npm install
    ```

2. **Launch LiquidBounce**

    Start the LiquidBounce client before running the development server.

3. **Start the development server**

    ```bash
    npm run dev
    ```

4. **Set the theme in the client**

    Use the live development URL provided by the dev server (for example, http://localhost:5173/):

    ```
    .client theme set <live-url>
    ```


5. **Open the theme in a browser**

    Run the following command in the client and select the UI you want to open in your system browser:

    ```
    .client integration menu
    ```

### Building for Production

Follow these steps to create a production-ready build of the theme.

1. **Build the theme**

    ```bash
    npm run build
    ```

2. **Locate the build output**

    - The production build is generated in the `dist` directory

    - This directory contains the optimized and minified theme files

3. **Deploy to the themes directory**

    - Copy the entire dist directory into your LiquidBounce themes directory

    - You can open the themes directory by running:
   
    ```
    .client theme browse
    ```

    - Rename the dist folder to your desired theme name

4. **Apply the theme**

    ```
    .client theme set <your-theme-name>
    ```

## Marketplace Publishing

To publish the theme to the LiquidBounce Marketplace, complete the following steps.

1. **Generate an API token**

    - Visit: https://liquidbounce.net/account

    - Generate an API token from your account settings

2. **Obtain the marketplace item ID**

    - Navigate to: https://liquidbounce.net/marketplace

    - Open your marketplace item page

    - Copy the value from the Item ID field displayed on the right side

3. **Configure the repository**

    - Add `API_TOKEN` as a repository secret under Settings → Secrets and variables → Actions

    - Update the following values in `.github/workflows/build.yml`:

      - `MARKETPLACE_ITEM_ID`

      - `ZIP_NAME`

4. **Enable publishing**

    - Uncomment the GitHub release and marketplace upload steps in:
   
    ```
    .github/workflows/build.yml
    ```

Once configured, the workflow will automatically build and publish the theme when triggered.
