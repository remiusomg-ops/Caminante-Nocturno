import struct,zlib,random,os
W=H=64
px=[[(0,0,0,0) for _ in range(W)] for __ in range(H)]
random.seed(9)

def setp(x,y,c):
    if 0<=x<W and 0<=y<H: px[y][x]=c

def fill(x0,y0,x1,y1,c):
    for y in range(y0,y1):
        for x in range(x0,x1):
            setp(x,y,c)

def noise(x0,y0,x1,y1,base,var=8):
    for y in range(y0,y1):
        for x in range(x0,x1):
            d=random.randint(-var,var)
            setp(x,y,tuple(max(0,min(255,v+d)) for v in base)+(255,))

# Cabeza completa, gris pálida y sucia.
for box in [(8,0,16,8),(16,0,24,8),(0,8,8,16),(8,8,16,16),(16,8,24,16),(24,8,32,16)]:
    noise(*box,(86,79,78),10)

# Cara: ojos blancos pequeños y sangre alrededor de boca.
fill(9,10,11,12,(245,245,245,255))
fill(13,10,15,12,(245,245,245,255))
fill(9,12,15,16,(54,10,11,255))
for x,y in [(9,13),(10,14),(12,13),(14,14),(11,15),(15,15)]:
    setp(x,y,(132,16,20,255))

# Torso: ropa muy oscura rota.
noise(16,16,40,32,(34,33,35),7)
for x in range(19,37,4):
    for y in range(20,30,3):
        if random.random()<0.7:
            setp(x,y,(76,67,65,255))
# Mancha central de sangre.
for x in range(25,31):
    for y in range(21,31):
        if random.random()<0.78:
            setp(x,y,(91+random.randint(0,35),10,14,255))

# Piernas.
noise(0,16,16,32,(42,40,42),8)
for x in range(0,16):
    for y in range(27,32):
        if random.random()<0.5:
            setp(x,y,(94,16,18,255))

# Brazos largos: zona completa que usa el modelo.
noise(40,16,56,40,(58,52,52),10)
for x in range(40,56):
    for y in range(28,40):
        if random.random()<0.7:
            setp(x,y,(104,18,21,255))

# Relleno extra oscuro para evitar tiras transparentes o UV raros.
for y in range(64):
    for x in range(64):
        if px[y][x][3]==0 and (
            (16<=x<40 and 16<=y<32) or
            (0<=x<16 and 16<=y<32) or
            (40<=x<56 and 16<=y<40)
        ):
            setp(x,y,(36,34,36,255))

raw=b''.join(b'\x00'+bytes(sum(([r,g,b,a] for r,g,b,a in row),[])) for row in px)
def chunk(t,d): return struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d)&0xffffffff)
png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',W,H,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw,9))+chunk(b'IEND',b'')
path='src/main/resources/assets/caminantenocturno/textures/entity/caminante_nocturno.png'
os.makedirs(os.path.dirname(path),exist_ok=True)
open(path,'wb').write(png)
print('night walker texture regenerated',len(png))
