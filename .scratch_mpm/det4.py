import cv2, numpy as np
D='C:/Users/feder/Downloads/'
img=cv2.imread(D+'medias.png').astype(int)[:,:,::-1]
c=img[165,362]; print(c)
dist=np.sqrt(((img-c)**2).sum(axis=2))
m=(dist<20).astype(np.uint8)*255
m=cv2.morphologyEx(m,cv2.MORPH_OPEN,cv2.getStructuringElement(cv2.MORPH_RECT,(9,9)))
n,lab,st,cen=cv2.connectedComponentsWithStats(m)
for i in range(1,n):
    x,y,w,h,a=st[i]
    if 50<=w<=140 and 50<=h<=140 and a>0.6*w*h: print('slot cx=%d cy=%d w=%d h=%d'%(x+w/2,y+h/2,w,h))
img=cv2.imread(D+'pantalon.png').astype(int)[:,:,::-1]
c=img[352,836]; print(c)
