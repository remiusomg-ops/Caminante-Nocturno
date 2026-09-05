import struct,zlib,os

def png16(path,pixels):
    W=H=16
    raw=b''.join(b'\x00'+bytes(sum(([r,g,b,a] for r,g,b,a in row),[])) for row in pixels)
    def chunk(t,d):
        return struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d)&0xffffffff)
    png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',W,H,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw,9))+chunk(b'IEND',b'')
    os.makedirs(os.path.dirname(path),exist_ok=True)
    open(path,'wb').write(png)

def blank():
    return [[(0,0,0,0) for _ in range(16)] for __ in range(16)]

def setp(p,x,y,c):
    if 0<=x<16 and 0<=y<16: p[y][x]=c

def rect(p,x0,y0,x1,y1,c):
    for y in range(y0,y1+1):
        for x in range(x0,x1+1): setp(p,x,y,c)

# Huevo del Caminante
p=blank()
# silueta del huevo
rows={2:(7,8),3:(6,9),4:(5,10),5:(4,11),6:(3,12),7:(3,12),8:(3,12),9:(3,12),10:(4,11),11:(4,11),12:(5,10),13:(6,9)}
for y,(a,b) in rows.items():
    for x in range(a,b+1):
        edge=(x==a or x==b or y in (2,13))
        setp(p,x,y,(34,30,31,255) if edge else (70,64,64,255))
# luces y manchas
for x,y in [(6,4),(8,5),(10,7),(5,9),(8,11),(10,10),(6,12)]:
    setp(p,x,y,(118,19,22,255))
for x,y in [(7,7),(9,7)]:
    setp(p,x,y,(242,242,242,255))
setp(p,5,5,(101,92,90,255)); setp(p,6,3,(92,84,82,255))
png16('src/main/resources/assets/caminantenocturno/textures/item/caminante_nocturno_spawn_egg.png',p)

# Frasco explosivo
p=blank()
# cuello y corcho
rect(p,7,1,8,2,(70,46,28,255))
rect(p,6,3,9,4,(180,190,195,255))
# botella
for y,a,b in [(5,5,10),(6,4,11),(7,4,11),(8,3,12),(9,3,12),(10,3,12),(11,4,11),(12,5,10),(13,6,9)]:
    for x in range(a,b+1):
        edge=(x==a or x==b or y in (5,13))
        setp(p,x,y,(166,190,202,220) if edge else (205,225,233,115))
# líquido explosivo
for y in range(8,12):
    for x in range(5,11):
        if p[y][x][3]:
            setp(p,x,y,(145,23,17,230) if y<10 else (210,62,18,235))
for x,y in [(6,8),(9,9),(7,10),(10,11)]:
    setp(p,x,y,(255,154,28,255))
# brillo del vidrio
for x,y in [(5,6),(4,8),(5,10),(7,5)]:
    setp(p,x,y,(235,245,248,220))
png16('src/main/resources/assets/caminantenocturno/textures/item/frasco_explosivo.png',p)

print('item textures generated')
