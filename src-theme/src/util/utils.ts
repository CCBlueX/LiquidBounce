export function portal(node: HTMLElement, target: HTMLElement = document.body) {
    target.appendChild(node);
    return {
        destroy() {
            if (node.parentNode) node.parentNode.removeChild(node);
        }
    };
}

export function clickOutside(node: HTMLElement, callback: (event: MouseEvent) => void) {
    const handleClick = (event: MouseEvent) => {
        if (!node.contains(event.target as Node)) {
            callback(event);
        }
    };

    const handleDrag = (event: DragEvent) => {
        if (!node.contains(event.target as Node)) {
            callback(event);
        }
    };

    document.addEventListener('click', handleClick, true);
    document.addEventListener('dragstart', handleDrag, true);

    return {
        destroy() {
            document.removeEventListener('click', handleClick, true);
            document.removeEventListener('dragstart', handleDrag, true);
        }
    };
}
