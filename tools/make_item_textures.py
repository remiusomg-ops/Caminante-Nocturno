import struct,zlib,os

def png16(path,p):
    raw=b''.join(b'\x00'+bytes(sum(([r,g,b,a] for r,g,b,a in row),[])) for row in p)
    def ch(t,d): return len(d).to_bytes(4,'big')+t+d+(zlib.crc32(t+d)&0xffffffff).to_bytes(4,'big')
    data=b'\x89PNG\r\n\x1a\n'+ch(b'IHDR',(16).to_bytes(4,'big')+(16).to_bytes(4,'big')+bytes([8,6,0,0,0]))+ch(b'IDAT',zlib.compress(raw,9))+ch(b'IEND',b'')
    os.makedirs(os.path.dirname(path),exist_ok=True)
    open(path,'wb').write(data)

def blank(): return [[(0,0,0,0) for _ in range(16)] for __ in range(16)]
def s(p,x,y,c):
    if 0<=x<16 and 0<=y<16:p[y][x]=c

# Huevo simple.
p=blank()
for y,a,b in [(2,7,8),(3,6,9),(4,5,10),(5,4,11),(6,3,12),(7,3,12),(8,3,12),(9,3,12),(10,4,11),(11,4,11),(12,5,10),(13,6,9)]:
    for x in range(a,b+1):
        edge=x in (a,b)
        s(p,x,y,(34,31,32,255) if edge else (72,66,66,255))
for q in [(6,5),(10,6),(5,9),(9,11),(7,12)]:s(p,*q,(128,20,23,255))
s(p,7,8,(245,245,245,255));s(p,9,8,(245,245,245,255))
png16('src/main/resources/assets/caminantenocturno/textures/item/caminante_nocturno_spawn_egg.png',p)

# Frasco: pequeño, compacto, estilo poción vanilla.
p=blank()
# corcho
for x in range(7,9): s(p,x,2,(95,57,28,255))
# cuello
for y in range(3,6):
    for x in range(6,10):
        edge=x in (6,9)
        s(p,x,y,(186,203,210,255) if edge else (220,235,238,120))
# cuerpo redondeado
shape={6:(5,10),7:(4,11),8:(3,12),9:(3,12),10:(3,12),11:(4,11),12:(5,10),13:(6,9)}
for y,(a,b) in shape.items():
    for x in range(a,b+1):
        edge=x in (a,b) or y==13
        s(p,x,y,(174,200,210,235) if edge else (220,236,240,90))
# líquido rojo/naranja solo abajo
for y in range(9,13):
    a,b=shape[y]
    for x in range(a+1,b):
        s(p,x,y,(164,30,18,245) if y<11 else (220,74,20,250))
for q in [(6,9),(9,10),(7,11),(10,11)]:s(p,*q,(255,166,34,255))
# banda metálica
for x in range(5,11): s(p,x,7,(128,128,132,255))
s(p,4,8,(236,246,249,220));s(p,5,7,(236,246,249,220))
png16('src/main/resources/assets/caminantenocturno/textures/item/frasco_explosivo.png',p)
print('item textures regenerated')
