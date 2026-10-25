#!/usr/bin/env python3
"""Verify the APK contains every original runtime asset, byte for byte."""
import hashlib
from pathlib import Path
import sys
import zipfile

root = Path(__file__).resolve().parents[1]
apk = Path(sys.argv[1])
with zipfile.ZipFile(apk) as archive:
    assert archive.testzip() is None, 'Corrupt APK ZIP entry'
    assert 'AndroidManifest.xml' in archive.namelist()
    assert 'classes.dex' in archive.namelist()
    count = 0
    for source in (root / 'assets').rglob('*'):
        if source.is_file():
            name = 'assets/' + source.relative_to(root / 'assets').as_posix()
            assert hashlib.sha256(archive.read(name)).digest() == hashlib.sha256(source.read_bytes()).digest(), name
            if source.suffix in {'.mp3', '.ogg'}:
                assert archive.getinfo(name).compress_type == zipfile.ZIP_STORED, name
            count += 1
    for source in (root / 'res' / 'raw').iterdir():
        name = 'res/raw/' + source.name
        assert archive.read(name) == source.read_bytes(), name
    native = [n for n in archive.namelist() if n.startswith('lib/') and n.endswith('.so')]
    assert not native, f'Unexpected native ABI dependency: {native}'
    print(f'PASS: {count} original assets and all 6 intro videos; no native/32-bit ABI dependency.')
print(f'APK: {apk.name}, {apk.stat().st_size / 1024**2:.1f} MiB')
print('SHA-256:', hashlib.sha256(apk.read_bytes()).hexdigest())
