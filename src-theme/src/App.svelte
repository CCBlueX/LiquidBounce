<script>
    import Router, { link } from "svelte-spa-router";
    import { routes } from "./routes.js";
</script>

<main>
    <Router {routes}/>
</main>
