import Combobox from "~/components/combobox";

type VersionSelectorProps = {
    currentVersion: string;
    onChange: (value: string) => void;
};

export default function VersionSelector({currentVersion, onChange}: VersionSelectorProps) {
    // TODO: Get supported versions from ViaVersion
    // const supportedVersions = viaVersion ? viaVersion.getSupportedVersions() : [];
    const supportedVersions = [
        "1.20.1",
        "1.20",
        "1.19.1",
        "1.19",
        "1.18.1",
        "1.18",
        "1.17.1",
        "1.17",
        "1.8.9",
    ]

    return (
        <Combobox
            closeOnSelect
            options={supportedVersions.map((version) => ({
                value: version.toString(),
                label: version.toString(),
                checked: version === currentVersion,
            }))}
            onToggle={(option) => {
                if (option) {
                    onChange(option.value);
                }
            }}
        >
            Version: {currentVersion}
        </Combobox>
    );
}
