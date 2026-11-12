"""Toca un elemento de la pantalla del móvil por su texto o content-desc (uso en pruebas manuales).

Uso: python scripts/tap.py "Más" ["otro texto" ...]
Cada argumento es una expresión regular: "." acepta cualquier carácter, así que
"Galer.a" casa con "Galería" aunque la consola (Git Bash) mangle los acentos.
"""

import os
import re
import subprocess
import sys
import time

ADB = os.environ.get(
    "ADB", r"C:\Users\MELAKUGARCIABONACHO\AppData\Local\Android\Sdk\platform-tools\adb.exe"
)
APP_ID = "com.example.lifet"
BOUNDS = re.compile(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')


def adb(*args: str) -> str:
    result = subprocess.run([ADB, *args], capture_output=True, check=False)
    return result.stdout.decode("utf-8", errors="replace")


def find(pattern: str) -> tuple[int, int] | None:
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    xml = adb("shell", "cat", "/sdcard/ui.xml")
    matcher = re.compile(pattern)
    for node in re.finditer(r"<node [^>]*>", xml):
        text = node.group(0)
        label = re.search(r'(?:text|content-desc)="([^"]*)"', text)
        if label and matcher.search(label.group(1)):
            left, top, right, bottom = map(int, BOUNDS.search(text).groups())
            return (left + right) // 2, (top + bottom) // 2
    return None


def focused_package() -> str:
    match = re.search(r"mCurrentFocus=.*? u0 ([\w.]+)/", adb("shell", "dumpsys", "window"))
    return match.group(1) if match else ""


for wanted in sys.argv[1:]:
    # Nunca tocar dentro de otra app (el móvil es de uso personal).
    if focused_package() != APP_ID:
        sys.exit(f"Abortado: el foco no está en {APP_ID} sino en '{focused_package()}'.")
    point = find(wanted)
    print(f"{wanted} -> {point}")
    if point:
        adb("shell", "input", "tap", str(point[0]), str(point[1]))
    time.sleep(1.5)
