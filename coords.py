#!/usr/bin/env python3
"""
coords.py - klikni na obrazek, dostanes souradnice do konzole
Pouziti: python3 coords.py <obrazek>
"""
import sys
import tkinter as tk
from PIL import Image, ImageTk


def main():
    if len(sys.argv) < 2:
        print("Pouziti: python3 coords.py <obrazek>")
        sys.exit(1)

    path = sys.argv[1]

    root = tk.Tk()
    root.title(f"coords - {path}")

    img = Image.open(path)
    tk_img = ImageTk.PhotoImage(img)

    canvas = tk.Canvas(root, width=img.width, height=img.height, cursor="crosshair")
    canvas.pack()
    canvas.create_image(0, 0, anchor="nw", image=tk_img)

    def on_click(event):
        print(f"{event.x}, {event.y}", flush=True)

    canvas.bind("<Button-1>", on_click)
    root.mainloop()


if __name__ == "__main__":
    main()
