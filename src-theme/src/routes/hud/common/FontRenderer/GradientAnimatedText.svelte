<script lang="ts">
    export let text = "";
    export let fontSize = "18px";
    export let fontFamily = "Alibaba";
</script>

<svg xmlns="http://www.w3.org/2000/svg" width="100%" height="20px"
     style="display: inline-block;">

    <text x="0" y="50%" dy="0.4em" fill="url(#pattern)" font-family={fontFamily} font-size={fontSize}>{text}</text>
    <defs>

        <linearGradient id="gradient" x1="0%" y1="0%" x2="100%" y2="0">
            <stop offset="0%" stop-color="var(--secondary-color)"/>
            <stop offset="50%" stop-color="var(--primary-color)"/>
            <stop offset="100%" stop-color="var(--secondary-color)"/>
        </linearGradient>

        <pattern id="pattern" x="0" y="0" width="300%" height="100%" patternUnits="userSpaceOnUse">
            <rect x="0" y="0" width="150%" height="100%" fill="url(#gradient)">
                <animate attributeType="XML"
                         attributeName="x"
                         from="0" to="150%"
                         dur="7s"
                         repeatCount="indefinite"/>
            </rect>
            <rect x="-150%" y="0" width="150%" height="100%" fill="url(#gradient)">
                <animate attributeType="XML"
                         attributeName="x"
                         from="-150%" to="0"
                         dur="7s"
                         repeatCount="indefinite"/>
            </rect>
        </pattern>
    </defs>
</svg>
