#!/usr/bin/env python3
"""Inspect the exact pinned FCL ARM64 runtime archives without extracting them."""

from pathlib import PurePosixPath
import sys
import tarfile
from pathlib import Path

VERSIONS = {
    "jre8": ("jre/lib/aarch64/server/libjvm.so",),
    "jre17": ("lib/aarch64/server/libjvm.so",),
    "jre21": ("lib/aarch64/server/libjvm.so",),
    "jre25": ("lib/aarch64/server/libjvm.so",),
}

def members(archive: Path) -> set[str]:
    with tarfile.open(archive, mode="r:xz") as tar:
        names = set()
        for item in tar.getmembers():
            name = item.name
            while name.startswith("./"):
                name = name[2:]
            path = PurePosixPath(name)
            if path.is_absolute() or ".." in path.parts:
                raise SystemExit(f"Unsafe archive path in {archive}: {item.name}")
            names.add(name)
        return names

def main() -> None:
    # Gradle's generated source directory is passed by CI to avoid assuming its location.
    root = Path(sys.argv[1])
    failures = []
    for version, jvm_paths in VERSIONS.items():
        folder = root / "app_runtime" / "java" / version
        version_file = folder / "version"
        universal = folder / "universal.tar.xz"
        arm64 = folder / "bin-arm64.tar.xz"
        for required in (version_file, universal, arm64):
            if not required.is_file() or required.stat().st_size == 0:
                failures.append(f"{version}: missing/empty {required.name}")
        if failures and any(version in item for item in failures):
            continue
        universal_names = members(universal)
        arm64_names = members(arm64)
        all_names = universal_names | arm64_names
        if not any(name == "release" or name.endswith("/release") for name in all_names):
            failures.append(f"{version}: release metadata missing from archives")
        if not any(name == "bin/java" or name.endswith("/bin/java") for name in all_names):
            failures.append(f"{version}: bin/java missing from archives")
        jvm_entries = sorted(name for name in all_names if "libjvm.so" in name.lower())
        if not jvm_entries:
            failures.append(f"{version}: libjvm.so missing from both archives")
            print(f"{version}: no libjvm.so entry; nearby JVM candidates: " +
                  repr(sorted(name for name in all_names if "jvm" in name.lower())[:30]), file=sys.stderr)
        elif not any(any(name == candidate or name.endswith("/" + candidate) for name in jvm_entries)
                     for candidate in jvm_paths):
            failures.append(f"{version}: libjvm.so exists at unexpected path(s): {jvm_entries}")
        print(f"{version}: universal={len(universal_names)} entries, arm64={len(arm64_names)} entries")
    if failures:
        print("\n".join(failures), file=sys.stderr)
        raise SystemExit(1)
    print("All four pinned FCL ARM64 runtime archives passed structural checks.")

if __name__ == "__main__":
    main()
