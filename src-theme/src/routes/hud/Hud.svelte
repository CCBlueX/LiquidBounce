<script>
    import TabGui from "./tabgui/TabGui.svelte";
    import Watermark from "./watermark/Watermark.svelte";
    import ArrayList from "./arraylist/ArrayList.svelte";
    import Notifications from "./notification/Notifications.svelte";

    import { listen } from "../../client/ws.svelte";
    import { getModules, toggleModule } from "../../client/api.svelte";
</script>

<main>
    <ArrayList getModules={getModules} listen={listen} />
    <Watermark/>
    <TabGui getModules={getModules} toggleModule={toggleModule} listen={listen} />
    <Notifications listen={listen} />
</main>
