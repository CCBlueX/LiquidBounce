export function convertToSpacedString(name: string): string {
    const regex = /[A-Z]?[a-z]+|[0-9]+|[A-Z]+(?![a-z])/g;
    return (name.match(regex) as string[]).join(" ");
}