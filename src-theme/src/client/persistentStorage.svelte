<script context="module">
    import {getPersistentStorageItems, setPersistentStorageItems} from './api.svelte';

    let loadedOnce = false;

    // As soon this file is loaded, we want to load all the data from the persistent storage on the API to the
    // local storage of the browser. This is done to avoid having to make a request to the API every time we
    // want to access the data.

    // We also want to make sure that the data is always up-to-date, so we will write the data to the API
    // every time we change the local storage.

    export async function insertPersistentData() {
        // Array of {key, value} objects
        const items = await getPersistentStorageItems();

        for (const item of items) {
            localStorage.setItem(item.key, item.value);
        }
        loadedOnce = true;
    }

    export async function updatePersistentData() {
        if (!loadedOnce) {
            return;
        }

        const items = [];

        for (let i = 0; i < localStorage.length; i++) {
            const key = localStorage.key(i);
            const value = localStorage.getItem(key);

            items.push({
                "key": key,
                "value": value
            });
        }

        await setPersistentStorageItems(items);
    }

    window.addEventListener('storage', updatePersistentData);
</script>
