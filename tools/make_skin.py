import struct,zlib,random,os

W=H=64
px=[[(0,0,0,0) for _ in range(W)] for __ in range(H)]
random.seed(17)

def setp(x,y,c):
    if 0<=x<W and 0<=y<H: px[y][x]=c

def noise_rect(x0,y0,x1,y1,base,var=12):
    for y in range(y0,y1):
        for x in range(x0,x1):
            v=random.randint(-var,var)
            setp(x,y,tuple(max(0,min(255,base[i]+v)) for i in range(3))+(255,))

def rect(x0,y0,x1,y1,c):
    for y in range(y0,y1+1):
        for x in range(x0,x1+1): setp(x,y,c)

# Cabeza: piel gris pálida, sucia y descompuesta
for b in [(8,0,16,8),(16,0,24,8),(0,8,8,16),(8,8,16,16),(16,8,24,16),(24,8,32,16)]:
    noise_rect(*b,(98,86,83),16)
noise_rect(8,8,16,16,(112,96,92),12)
rect(9,10,11,11,(250,250,250,255))
rect(13,10,15,11,(250,250,250,255))
rect(9,12,15,15,(72,7,8,255))
for x,y in [(9,12),(10,13),(12,12),(14,13),(15,15),(11,15)]:
    setp(x,y,(135,15,17,255))

# Torso oscuro, costillas y sangre
for b in [(16,16,40,32),(0,16,16,32),(40,16,56,32),(16,48,32,64),(32,48,48,64),(48,48,64,64)]:
    noise_rect(*b,(43,40,42),11)
# zonas de carne expuesta
for b in [(0,22,16,32),(40,22,56,32),(16,56,32,64),(32,56,48,64),(48,56,64,64)]:
    noise_rect(*b,(86,67,66),14)
# pecho ensangrentado/costillas
for x in (21,24,31,34):
    for y in range(20,29): setp(x,y,(112,98,95,255))
rect(25,22,30,31,(70,7,8,255))
for x,y in [(22,21),(23,25),(27,19),(28,26),(32,23),(35,28),(26,30)]:
    setp(x,y,(145,17,20,255))
# sangre en manos/pies
for b in [(0,27,16,32),(40,27,56,32),(16,60,32,64),(32,60,48,64),(48,60,64,64)]:
    x0,y0,x1,y1=b
    rect(x0,y0,x1-1,y1-1,(68,6,8,255))
    for _ in range(18):
        setp(random.randrange(x0,x1),random.randrange(y0,y1),(130,14,18,255))

# capa exterior rota y oscura
for b in [(0,32,16,48),(16,32,32,48),(40,32,56,48)]:
    x0,y0,x1,y1=b
    for y in range(y0,y1):
        for x in range(x0,x1):
            if random.random()<0.28:
                setp(x,y,(22+random.randrange(10),20+random.randrange(10),22+random.randrange(10),255))

raw=b''.join(b'\x00'+bytes(sum(([r,g,b,a] for r,g,b,a in row),[])) for row in px)
def chunk(t,d): return struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d)&0xffffffff)
png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',W,H,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw,9))+chunk(b'IEND',b'')
path='src/main/resources/assets/caminantenocturno/textures/entity/caminante_nocturno.png'
os.makedirs(os.path.dirname(path),exist_ok=True)
open(path,'wb').write(png)
print('texture bytes',len(png))
