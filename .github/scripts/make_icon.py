#!/usr/bin/env python3
from pathlib import Path
import sys
from PIL import Image, ImageDraw, ImageFont

root = Path(sys.argv[1] if len(sys.argv) > 1 else "res")
sizes = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}
for folder, size in sizes.items():
    d = root / folder
    d.mkdir(parents=True, exist_ok=True)
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    pad = max(1, size // 16)
    draw.rounded_rectangle(
        [pad, pad, size - pad - 1, size - pad - 1],
        radius=size // 5,
        fill=(18, 18, 24, 255),
        outline=(232, 72, 85, 255),
        width=max(2, size // 24),
    )
    try:
        font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", int(size * 0.48))
    except Exception:
        font = ImageFont.load_default()
    text = "R"
    bbox = draw.textbbox((0, 0), text, font=font)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    draw.text(((size - tw) / 2 - bbox[0], (size - th) / 2 - bbox[1] - size * 0.02), text, fill=(232, 72, 85, 255), font=font)
    img.save(d / "ic_launcher.png")
print("icons written under", root)
