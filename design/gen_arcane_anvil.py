"""Generates the arcane anvil block textures.

Style reference: Thaumcraft 4 arcane workbench (dark wood, green cloth with
rune marks, gold fittings). Run from the repo root:  python design/gen_arcane_anvil.py
The GUI uses the original TC4 workbench sprites, loaded at runtime (see ArcaneAnvilScreen).
"""
import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
BLOCK = ROOT / "src/main/resources/assets/tc_t/textures/block"

# ---------------------------------------------------------------- palette
W_OUT = (10, 7, 6)
W0 = (24, 16, 13)
W1 = (36, 24, 20)
W2 = (48, 32, 26)
W3 = (60, 40, 32)
W4 = (76, 52, 40)
W5 = (96, 66, 50)

C_LINE = (1, 34, 10)
C0 = (1, 48, 15)
C1 = (2, 62, 21)
C2 = (3, 69, 24)
C3 = (5, 76, 28)
C4 = (12, 90, 35)
C_HI = (26, 110, 48)

G0 = (74, 44, 8)
G1 = (136, 88, 22)
G2 = (198, 148, 42)
G3 = (238, 190, 78)
G4 = (255, 230, 140)

SLOT = (139, 139, 139)
SLOT_DARK = (55, 55, 55)
WHITE = (255, 255, 255)
GREY = (198, 198, 198)
GREY_DARK = (85, 85, 85)


def rgba(c, a=255):
    return (c[0], c[1], c[2], a)


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


class Canvas:
    def __init__(self, w, h, seed):
        self.im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.px = self.im.load()
        self.w, self.h = w, h
        self.rng = random.Random(seed)

    def set(self, x, y, c, a=255):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[x, y] = rgba(c, a)

    def get(self, x, y):
        return self.px[x, y][:3]

    def rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, c)

    def pick(self, *colors_weights):
        total = sum(w for _, w in colors_weights)
        r = self.rng.random() * total
        for c, w in colors_weights:
            r -= w
            if r <= 0:
                return c
        return colors_weights[-1][0]


# ---------------------------------------------------------------- shared fills
def cloth_fill(cv, x0, y0, x1, y1):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            cv.set(x, y, cv.pick((C1, 3), (C2, 5), (C3, 3), (C0, 0.6), (C4, 0.5)))


def wood_planks_h(cv, x0, y0, x1, y1, plank=6, palette=(W1, W2, W3, W4)):
    d, m, l, hi = palette
    for y in range(y0, y1 + 1):
        row = (y - y0) % plank
        streak = cv.rng.random()
        for x in range(x0, x1 + 1):
            if row == plank - 1:
                cv.set(x, y, d)
            elif row == 0:
                cv.set(x, y, cv.pick((l, 4), (hi, 1)))
            else:
                c = cv.pick((m, 6), (l, 2), (d, 1))
                if streak < 0.3 and (x + y * 7) % 11 < 3:
                    c = d
                cv.set(x, y, c)
        # plank butt joints
        if row == 2:
            for x in range(x0 + (y // plank * 23) % 37, x1, 41):
                for yy in range(y - 2, y - 2 + plank - 1):
                    cv.set(x, yy, d)


def gold_bracket(cv, x, y, sx, sy, arm=5):
    """L-shaped gold corner bracket. sx/sy = +1/-1 direction of the arms."""
    for i in range(arm):
        for (px, py) in ((x + i * sx, y), (x, y + i * sy)):
            cv.set(px, py, G2 if i else G4)
        # inner shade
        if i > 0:
            cv.set(x + i * sx, y + sy, G0)
            cv.set(x + sx, y + i * sy, G0)
    cv.set(x + sx, y + sy, G1)
    cv.set(x + (arm - 1) * sx, y, G3)
    cv.set(x, y + (arm - 1) * sy, G3)


# ---------------------------------------------------------------- block textures
def block_wood_side():
    cv = Canvas(16, 16, 11)
    # rows 0..5 are behind the metal head, keep a generic wood fill
    wood_planks_h(cv, 0, 0, 15, 5, plank=3, palette=(W0, W1, W2, W3))
    # row 6: gold collar right under the head
    for u in range(16):
        cv.set(u, 6, G3 if u % 4 else G2)
    # rows 7..11: waist, vertical grain
    for v in range(7, 12):
        for u in range(16):
            c = cv.pick((W2, 6), (W3, 2), (W1, 1))
            if u % 3 == 1 and cv.rng.random() < 0.6:
                c = W1
            cv.set(u, v, c)
    for u in (4, 6, 9, 11):
        for v in range(7, 12):
            cv.set(u, v, W0)
    for u in (5, 10):
        cv.set(u, 7, W4)
    # emerald inlay in the middle of the waist
    cv.set(7, 8, C3); cv.set(8, 8, C2)
    cv.set(7, 9, C_HI); cv.set(8, 9, C4)
    cv.set(7, 10, C2); cv.set(8, 10, C0)
    for u in (7, 8):
        cv.set(u, 7, G1)
        cv.set(u, 11, G1)
    # row 12: plinth trim
    for u in range(16):
        cv.set(u, 12, W1 if u % 2 else W2)
    # rows 13..15: foot
    for u in range(16):
        cv.set(u, 13, cv.pick((W4, 3), (W5, 1)))
        cv.set(u, 14, cv.pick((W2, 4), (W3, 2), (W1, 1)))
        cv.set(u, 15, cv.pick((W1, 3), (W0, 1)))
    for u in (2, 13):
        cv.set(u, 13, G4); cv.set(u, 14, G2); cv.set(u, 15, G1)
    cv.set(3, 13, G3); cv.set(12, 13, G3)
    cv.set(3, 14, G0); cv.set(12, 14, G0)
    return cv.im


def block_wood_top():
    cv = Canvas(16, 16, 12)
    for u in range(16):
        plank = u // 3
        for v in range(16):
            c = cv.pick((W2, 6), (W3, 3), (W1, 1))
            if u % 3 == 2:
                c = W1
            if (v + plank * 5) % 9 == 0:
                c = W0
            cv.set(u, v, c)
    # bevel ring of the foot (2..13)
    for i in range(2, 14):
        cv.set(i, 2, W4); cv.set(2, i, W4)
        cv.set(i, 13, W1); cv.set(13, i, W1)
    gold_bracket(cv, 2, 2, 1, 1, 3)
    gold_bracket(cv, 13, 2, -1, 1, 3)
    gold_bracket(cv, 2, 13, 1, -1, 3)
    gold_bracket(cv, 13, 13, -1, -1, 3)
    return cv.im


# 10 x 14 design of the cloth on top of the anvil (TC4 workbench cloth: 3x3 wells
# with a rune triangle above and below).
CLOTH_TOP = [
    "g...##...g",
    "...#..#...",
    "..######..",
    ".ss.ss.ss.",
    ".dd.dd.dd.",
    "..........",
    ".ss.ss.ss.",
    ".dd.dd.dd.",
    "..........",
    ".ss.ss.ss.",
    ".dd.dd.dd.",
    "..######..",
    "...#..#...",
    "g...##...g",
]


def block_cloth_top():
    cv = Canvas(16, 16, 13)
    cloth_fill(cv, 0, 0, 15, 15)
    for z, row in enumerate(CLOTH_TOP):
        for x, ch in enumerate(row):
            u, v = 3 + x, 1 + z
            if ch == "#":
                cv.set(u, v, C_LINE)
            elif ch == "s":
                cv.set(u, v, C0)
            elif ch == "d":
                cv.set(u, v, C1)
            elif ch == "g":
                cv.set(u, v, G3)
    # folded edges along the long sides
    for v in range(1, 15):
        if CLOTH_TOP[v - 1][0] == ".":
            cv.set(3, v, C1)
        if CLOTH_TOP[v - 1][9] == ".":
            cv.set(12, v, C1)
    return cv.im


# hanging flap: rows v (y = 16 - v) and the z span of each row
FLAP_ROWS = [(0, 3, 13), (1, 3, 13), (2, 3, 13), (3, 3, 13), (4, 4, 12), (5, 5, 11), (6, 6, 10), (7, 7, 9)]


def block_cloth_side():
    cv = Canvas(16, 16, 14)
    cv.rect(0, 0, 15, 15, C0)
    for v, a, b in FLAP_ROWS:
        for u in range(a, b):
            cv.set(u, v, cv.pick((C1, 3), (C2, 5), (C3, 3)))
        cv.set(a, v, C0)
        cv.set(b - 1, v, C0)
    for u in range(3, 13):
        cv.set(u, 0, C4 if u % 2 else C3)       # fold over the top edge
    # rune triangle
    cv.set(7, 1, C_LINE); cv.set(8, 1, C_LINE)
    cv.set(6, 2, C_LINE); cv.set(9, 2, C_LINE)
    for u in range(6, 10):
        cv.set(u, 3, C_LINE)
    # bottom hem and gold bead on the tip
    for v, a, b in FLAP_ROWS[4:7]:
        cv.set(a, v, C_LINE); cv.set(b - 1, v, C_LINE)
    cv.set(7, 7, G3); cv.set(8, 7, G1)
    return cv.im



def main():
    BLOCK.mkdir(parents=True, exist_ok=True)
    block_wood_side().save(BLOCK / "arcane_anvil_wood_side.png")
    block_wood_top().save(BLOCK / "arcane_anvil_wood_top.png")
    block_cloth_top().save(BLOCK / "arcane_anvil_cloth.png")
    block_cloth_side().save(BLOCK / "arcane_anvil_cloth_side.png")
    print("written")


if __name__ == "__main__":
    main()
